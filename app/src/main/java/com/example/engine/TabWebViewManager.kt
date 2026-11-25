package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.preferences.BrowserSettings
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class TabWebEventCallbacks(
    val onPageStarted: (tabId: String, url: String) -> Unit,
    val onPageFinished: (tabId: String, url: String, title: String, canBack: Boolean, canForward: Boolean) -> Unit,
    val onProgressChanged: (tabId: String, progress: Int) -> Unit,
    val onTitleChanged: (tabId: String, title: String) -> Unit,
    val onNavigationStateChanged: (tabId: String, url: String, canBack: Boolean, canForward: Boolean) -> Unit,
    val onNetworkError: (tabId: String, failingUrl: String, description: String) -> Unit,
    val onSslError: (tabId: String, host: String, primaryError: Int, handler: SslErrorHandler) -> Unit,
    val onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String) -> Unit,
    val onOpenNewTabUrl: (url: String, isIncognito: Boolean) -> Unit,
    val onFullscreenVideoShow: (view: View, callback: WebChromeClient.CustomViewCallback) -> Unit,
    val onFullscreenVideoHide: () -> Unit,
    val onFileChooserRequested: (filePathCallback: ValueCallback<Array<Uri>>, acceptTypes: Array<String>) -> Unit,
    val onSitePermissionRequested: (origin: String, resources: Array<String>, request: PermissionRequest) -> Unit,
    val onGeolocationRequested: (origin: String, callback: GeolocationPermissions.Callback) -> Unit,
    val onFindMatchesUpdated: (activeOrdinal: Int, totalMatches: Int) -> Unit,
    val onExternalIntentError: (message: String) -> Unit,
    val onScrollDirectionChanged: (hideBars: Boolean) -> Unit = {},
    val onSiteNotificationPermissionRequested: (tabId: String, origin: String, host: String, jsCallbackId: String) -> Unit = { _, _, _, _ -> },
    val onSiteNotificationPosted: (host: String, title: String, body: String, url: String) -> Unit = { _, _, _, _ -> },
    val getSiteNotificationAllowed: (host: String) -> Boolean? = { null }
)

data class BackNavResult(
    val handled: Boolean,
    val targetUrl: String = "",
    val targetTitle: String = "",
    val canGoBackAfter: Boolean = false,
    val canGoForwardAfter: Boolean = false
)

class TabWebViewManager(
    private val appContext: Context,
    private val adBlockEngine: AdBlockEngine
) {
    private val webViewLock = Any()
    private val webViews: MutableMap<String, WebView> = Collections.synchronizedMap(
        LinkedHashMap<String, WebView>(8, 0.75f, true)
    )
    private val incognitoTabIds = ConcurrentHashMap.newKeySet<String>()
    private val currentPageUrls = ConcurrentHashMap<String, String>()
    private val homeAnchorIndices = ConcurrentHashMap<String, Int>()
    private val appliedSettingsCache = ConcurrentHashMap<String, Pair<BrowserSettings, Boolean>>()
    private var callbacks: TabWebEventCallbacks? = null
    private var currentSettings: BrowserSettings = BrowserSettings()
    @Volatile
    private var effectiveWebsiteDarkMode: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastResumedTabId: String? = null
    private val maxCachedWebViews = 12
    private val defaultVideoPosterBitmap: Bitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    init {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            runCatching { WebViewCompat.startSafeBrowsing(appContext) {} }
        }
    }

    private fun snapshotWebViews(): List<Pair<String, WebView>> = synchronized(webViewLock) {
        webViews.entries.map { it.key to it.value }
    }

    fun attachCallbacks(cb: TabWebEventCallbacks) {
        callbacks = cb
    }

    fun primeTabUrl(tabId: String, url: String) {
        if (url.isNotBlank()) {
            currentPageUrls[tabId] = url
        }
    }

    /**
     * Immediately updates Dark Mode across ALL open websites (both native WebView darkening
     * and universal smart CSS dark mode) whenever phone system theme, app theme, or Dark Sites toggles.
     */
    fun updateWebsiteDarkMode(enabled: Boolean) {
        if (effectiveWebsiteDarkMode == enabled) return
        effectiveWebsiteDarkMode = enabled
        snapshotWebViews().forEach { (_, webView) ->
            applyDarkModeToWebView(webView, enabled)
            injectUniversalDarkModeScript(webView, enabled)
        }
    }

    fun updateGlobalSettings(settings: BrowserSettings) {
        val barVisibilityChanged = currentSettings.showTopBar != settings.showTopBar ||
            currentSettings.showBottomBar != settings.showBottomBar
        currentSettings = settings
        val cookieManager = runCatching { CookieManager.getInstance() }.getOrNull()
        cookieManager?.setAcceptCookie(settings.cookiesEnabled)
        snapshotWebViews().forEach { (tabId, webView) ->
            val desktopMode = (webView.tag as? Boolean) == true
            applySettingsToWebView(
                webView = webView,
                settings = settings,
                isIncognito = incognitoTabIds.contains(tabId),
                desktopMode = desktopMode
            )
            appliedSettingsCache[tabId] = settings to desktopMode
            if (barVisibilityChanged) {
                injectFloatingGlassSafeInsets(webView)
            }
        }
    }

    /**
     * Captures a lightweight downsampled backdrop snapshot of an active tab's WebView
     * using hardware-accurate PixelCopy (API 26+) with clean software fallback (API 24-25),
     * so floating overlays (3-dot Menu, Tab Switcher, Dialogs) can render true frosted blur
     * without attaching RenderEffect directly onto Chromium WebView hardware surfaces.
     */
    fun captureLowResBlurBitmap(
        tabId: String,
        window: Window?,
        fallbackColor: Int,
        downsample: Int = 10,
        onCaptured: (Bitmap?) -> Unit
    ) {
        val wv = synchronized(webViewLock) { webViews[tabId] }
        if (wv == null) {
            onCaptured(null)
            return
        }
        val w = wv.width
        val h = wv.height
        if (w <= 0 || h <= 0) {
            onCaptured(null)
            return
        }
        val factor = downsample.coerceIn(4, 16)
        val bw = (w / factor).coerceIn(48, 220)
        val bh = (h / factor).coerceIn(48, 380)

        fun fallbackSoftwareCapture(): Bitmap? = runCatching {
            val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawColor(fallbackColor)
            canvas.scale(bw.toFloat() / w.toFloat(), bh.toFloat() / h.toFloat())
            canvas.translate(-wv.scrollX.toFloat(), -wv.scrollY.toFloat())
            wv.draw(canvas)
            bmp
        }.getOrNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window != null) {
            val loc = IntArray(2)
            wv.getLocationInWindow(loc)
            val decor = window.decorView
            val left = loc[0].coerceIn(0, decor.width)
            val top = loc[1].coerceIn(0, decor.height)
            val right = (loc[0] + w).coerceIn(left, decor.width)
            val bottom = (loc[1] + h).coerceIn(top, decor.height)
            if (right - left > 4 && bottom - top > 4) {
                val srcRect = android.graphics.Rect(left, top, right, bottom)
                val dest = runCatching { Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888) }.getOrNull()
                if (dest != null) {
                    val requested = runCatching {
                        PixelCopy.request(
                            window,
                            srcRect,
                            dest,
                            { result ->
                                if (result == PixelCopy.SUCCESS) {
                                    onCaptured(dest)
                                } else {
                                    onCaptured(fallbackSoftwareCapture())
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )
                    }.isSuccess
                    if (requested) return
                }
            }
        }
        onCaptured(fallbackSoftwareCapture())
    }

    fun getOrCreateWebView(
        context: Context,
        tabId: String,
        isIncognito: Boolean,
        desktopMode: Boolean
    ): WebView {
        synchronized(webViewLock) { webViews[tabId] }?.let { existing ->
            val desired = currentSettings to desktopMode
            if (appliedSettingsCache[tabId] != desired) {
                applySettingsToWebView(existing, currentSettings, isIncognito, desktopMode)
                appliedSettingsCache[tabId] = desired
            }
            if (lastResumedTabId != tabId) {
                lastResumedTabId = tabId
                runCatching { existing.onResume() }
            }
            return existing
        }

        evictOldestIfNeeded(excludeTabId = tabId)

        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            tag = desktopMode
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(
                if (effectiveWebsiteDarkMode) android.graphics.Color.parseColor("#0A0E1B")
                else android.graphics.Color.WHITE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var accumulatedDelta = 0
                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    val dy = scrollY - oldScrollY
                    if (scrollY <= 36) {
                        accumulatedDelta = 0
                        callbacks?.onScrollDirectionChanged(false)
                    } else {
                        if ((dy > 0 && accumulatedDelta < 0) || (dy < 0 && accumulatedDelta > 0)) {
                            accumulatedDelta = 0
                        }
                        accumulatedDelta += dy
                        if (accumulatedDelta > 72) {
                            accumulatedDelta = 0
                            callbacks?.onScrollDirectionChanged(true)
                        } else if (accumulatedDelta < -44) {
                            accumulatedDelta = 0
                            callbacks?.onScrollDirectionChanged(false)
                        }
                    }
                }
            }
        }

        if (isIncognito) {
            incognitoTabIds.add(tabId)
        }

        webView.addJavascriptInterface(NovaNotifyJsBridge(isIncognito), "__novaNotifyBridge")
        configureWebView(webView, tabId, isIncognito, desktopMode)
        appliedSettingsCache[tabId] = currentSettings to desktopMode
        lastResumedTabId = tabId
        synchronized(webViewLock) {
            webViews[tabId] = webView
        }
        return webView
    }

    fun getExistingWebView(tabId: String): WebView? = synchronized(webViewLock) { webViews[tabId] }

    fun getCertificateSummary(tabId: String, fallbackUrl: String): SiteCertificateSummary {
        val wv = synchronized(webViewLock) { webViews[tabId] }
        val url = wv?.url?.takeIf { it.isNotBlank() } ?: fallbackUrl
        return SecurityUtils.buildCertificateSummary(url, wv?.certificate)
    }

    fun onActivityResume() {
        snapshotWebViews().forEach { (_, wv) -> runCatching { wv.onResume(); wv.resumeTimers() } }
    }

    fun onActivityPause() {
        snapshotWebViews().forEach { (_, wv) -> runCatching { wv.onPause() } }
    }

    fun loadUrl(tabId: String, url: String) {
        currentPageUrls[tabId] = url
        val privacyHeaders = mapOf(
            "DNT" to "1",
            "Sec-GPC" to "1"
        )
        synchronized(webViewLock) { webViews[tabId] }?.loadUrl(url, privacyHeaders)
    }

    /**
     * Marks the current WebView history index as the floor when user returns to NOVA Home,
     * so future navigations from Home go back to Home rather than pre-Home pages.
     */
    fun markHomeAnchor(tabId: String) {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return
        val historyList = wv.copyBackForwardList()
        homeAnchorIndices[tabId] = historyList.currentIndex + 1
    }

    fun canGoBackSmart(tabId: String): Boolean {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return false
        return findPreviousDistinctHistoryIndex(wv, tabId) != null
    }

    /**
     * Navigates back inside the active website first, skipping about:blank and
     * redirect-duplicate entries so Back always lands on the previous real webpage.
     */
    fun goBackSmart(tabId: String): BackNavResult {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return BackNavResult(handled = false)
        val historyList = wv.copyBackForwardList()
        val currentIndex = historyList.currentIndex
        val targetIndex = findPreviousDistinctHistoryIndex(wv, tabId)

        if (targetIndex != null && currentIndex >= 0) {
            val steps = targetIndex - currentIndex
            val targetItem = historyList.getItemAtIndex(targetIndex)
            val targetUrl = targetItem?.url.orEmpty()
            val targetTitle = targetItem?.title.orEmpty()
            currentPageUrls[tabId] = targetUrl
            wv.goBackOrForward(steps)
            val floor = homeAnchorIndices[tabId] ?: 0
            return BackNavResult(
                handled = true,
                targetUrl = targetUrl,
                targetTitle = targetTitle,
                canGoBackAfter = targetIndex > floor,
                canGoForwardAfter = true
            )
        }

        // Fallback if WebView has internal SPA history state not reflected by distinct URLs
        val floor = homeAnchorIndices[tabId] ?: 0
        if (wv.canGoBack() && currentIndex > floor) {
            wv.goBack()
            return BackNavResult(
                handled = true,
                targetUrl = wv.url.orEmpty(),
                targetTitle = wv.title.orEmpty(),
                canGoBackAfter = (currentIndex - 1) > floor,
                canGoForwardAfter = true
            )
        }

        return BackNavResult(handled = false)
    }

    private fun findPreviousDistinctHistoryIndex(wv: WebView, tabId: String): Int? {
        val list = wv.copyBackForwardList()
        val currentIndex = list.currentIndex
        if (currentIndex <= 0) return null

        val floor = (homeAnchorIndices[tabId] ?: 0).coerceAtLeast(0)
        val currentUrl = normalizeUrlForCompare(list.currentItem?.url ?: wv.url)

        for (i in (currentIndex - 1) downTo floor) {
            val item = list.getItemAtIndex(i) ?: continue
            val candidate = item.url.orEmpty()
            if (candidate.isBlank() || candidate.equals("about:blank", ignoreCase = true)) continue
            val normalizedCandidate = normalizeUrlForCompare(candidate)
            // Skip instant http->https or trailing-slash redirect duplicates of the current page
            if (normalizedCandidate == currentUrl && i > floor) continue
            return i
        }
        return null
    }

    private fun normalizeUrlForCompare(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removeSuffix("/")
            .lowercase()
    }

    fun goForward(tabId: String): Boolean {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return false
        return if (wv.canGoForward()) {
            wv.goForward()
            true
        } else {
            false
        }
    }

    fun reload(tabId: String) {
        synchronized(webViewLock) { webViews[tabId] }?.reload()
    }

    fun stopLoading(tabId: String) {
        synchronized(webViewLock) { webViews[tabId] }?.stopLoading()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return
        wv.tag = enabled
        appliedSettingsCache[tabId] = currentSettings to enabled
        applyUserAgent(wv, enabled)
        wv.reload()
    }

    fun findInPage(tabId: String, query: String) {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            callbacks?.onFindMatchesUpdated(0, 0)
        } else {
            wv.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        synchronized(webViewLock) { webViews[tabId] }?.findNext(forward)
    }

    fun clearFindMatches(tabId: String) {
        synchronized(webViewLock) { webViews[tabId] }?.clearMatches()
        callbacks?.onFindMatchesUpdated(0, 0)
    }

    fun extractReadableArticle(
        tabId: String,
        onResult: (title: String, byline: String, paragraphs: List<String>) -> Unit
    ) {
        val wv = synchronized(webViewLock) { webViews[tabId] }
        if (wv == null) {
            onResult("Reader View", "", emptyList())
            return
        }
        val script = """
            (function() {
                try {
                    var title = document.title || '';
                    var h1 = document.querySelector('h1');
                    if (h1 && h1.innerText && h1.innerText.trim().length > 4) {
                        title = h1.innerText.trim();
                    }
                    var root = document.querySelector('article') || document.querySelector('main') || document.body;
                    var nodes = root ? root.querySelectorAll('p, h2, h3, li') : [];
                    var blocks = [];
                    for (var i = 0; i < nodes.length && blocks.length < 80; i++) {
                        var t = (nodes[i].innerText || '').replace(/\s+/g, ' ').trim();
                        if (t.length >= 28) {
                            blocks.push(t);
                        }
                    }
                    return JSON.stringify({
                        title: title,
                        byline: window.location.hostname || '',
                        blocks: blocks
                    });
                } catch (e) {
                    return JSON.stringify({ title: document.title || 'Reader', byline: '', blocks: [] });
                }
            })();
        """.trimIndent()

        wv.evaluateJavascript(script) { raw ->
            try {
                if (raw.isNullOrBlank() || raw == "null") {
                    onResult(wv.title ?: "Reader View", "", emptyList())
                    return@evaluateJavascript
                }
                // evaluateJavascript returns a JSON-encoded string literal
                val unquoted = JSONObject("{\"v\":$raw}").getString("v")
                val json = JSONObject(unquoted)
                val title = json.optString("title", wv.title ?: "Reader View")
                val byline = json.optString("byline", "")
                val arr = json.optJSONArray("blocks")
                val list = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        arr.optString(i)?.takeIf { it.isNotBlank() }?.let(list::add)
                    }
                }
                onResult(title, byline, list)
            } catch (_: Exception) {
                onResult(wv.title ?: "Reader View", "", emptyList())
            }
        }
    }

    fun destroyTab(tabId: String) {
        val wasIncognito = incognitoTabIds.remove(tabId)
        currentPageUrls.remove(tabId)
        homeAnchorIndices.remove(tabId)
        appliedSettingsCache.remove(tabId)
        if (lastResumedTabId == tabId) {
            lastResumedTabId = null
        }
        synchronized(webViewLock) { webViews.remove(tabId) }?.let { wv ->
            destroyWebViewSafely(wv, clearData = wasIncognito)
        }
        if (wasIncognito && incognitoTabIds.isEmpty()) {
            clearIncognitoSessionArtifacts(clearPersistentIfSafe = false)
        }
    }

    fun clearIncognitoSessionArtifacts(clearPersistentIfSafe: Boolean = false) {
        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeSessionCookies(null)
            if (clearPersistentIfSafe && synchronized(webViewLock) { webViews.keys.none { !incognitoTabIds.contains(it) } }) {
                cookieManager.removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
            }
            cookieManager.flush()
            WebViewDatabase.getInstance(appContext).clearHttpAuthUsernamePassword()
            WebViewDatabase.getInstance(appContext).clearFormData()
        }
    }

    fun clearAllBrowsingData() {
        snapshotWebViews().forEach { (_, wv) ->
            runCatching {
                wv.clearCache(true)
                wv.clearHistory()
                wv.clearFormData()
            }
        }
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            WebViewDatabase.getInstance(appContext).clearHttpAuthUsernamePassword()
            WebViewDatabase.getInstance(appContext).clearFormData()
        }
    }

    fun destroyAll() {
        val entries = synchronized(webViewLock) {
            webViews.entries.map { it.key to it.value }.also { webViews.clear() }
        }
        val hadIncognito = incognitoTabIds.isNotEmpty()
        entries.forEach { (tabId, wv) ->
            destroyWebViewSafely(wv, clearData = incognitoTabIds.contains(tabId))
        }
        incognitoTabIds.clear()
        currentPageUrls.clear()
        homeAnchorIndices.clear()
        appliedSettingsCache.clear()
        if (hadIncognito) {
            clearIncognitoSessionArtifacts(clearPersistentIfSafe = false)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(
        webView: WebView,
        tabId: String,
        isIncognito: Boolean,
        desktopMode: Boolean
    ) {
        applySettingsToWebView(webView, currentSettings, isIncognito, desktopMode)

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            callbacks?.onFindMatchesUpdated(activeMatchOrdinal, numberOfMatches)
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            callbacks?.onDownloadRequested(
                url,
                userAgent.orEmpty(),
                contentDisposition.orEmpty(),
                mimetype.orEmpty()
            )
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return null
                // Never block top-level user navigations
                if (request.isForMainFrame) return null

                val pageUrl = currentPageUrls[tabId]
                val blockReason = adBlockEngine.checkShouldBlock(reqUrl, pageUrl)
                if (blockReason != null) {
                    val isJs = reqUrl.substringBefore("?").endsWith(".js", ignoreCase = true)
                    return if (isJs) {
                        WebResourceResponse(
                            "application/javascript",
                            "utf-8",
                            200,
                            "OK",
                            emptyMap(),
                            ByteArrayInputStream("/* blocked by Nova Shield */".toByteArray())
                        )
                    } else {
                        WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            204,
                            "Blocked by Nova Shield",
                            emptyMap(),
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase().orEmpty()

                if (scheme == "http" && currentSettings.httpsOnlyMode && request.isForMainFrame) {
                    val upgraded = uri.toString().replaceFirst("http://", "https://", ignoreCase = true)
                    view?.loadUrl(upgraded)
                    return true
                }

                // Block unsafe direct file/content URI navigations from web content
                if (scheme == "file" || scheme == "content") {
                    return true
                }

                // Always allow standard web & media streaming schemes inside WebView
                if (scheme == "http" || scheme == "https" || scheme == "about" ||
                    scheme == "blob" || scheme == "data" || scheme == "javascript"
                ) {
                    return false
                }

                // External app intents (mailto:, tel:, intent://, market://, etc.)
                return handleExternalUri(view, uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val resolvedUrl = url ?: return
                currentPageUrls[tabId] = resolvedUrl
                if (view != null) {
                    applyDarkModeToWebView(view, effectiveWebsiteDarkMode)
                    injectUniversalDarkModeScript(view, effectiveWebsiteDarkMode)
                }
                callbacks?.onScrollDirectionChanged(false)
                callbacks?.onPageStarted(tabId, resolvedUrl)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                if (view != null) {
                    injectFloatingGlassSafeInsets(view)
                    injectUniversalDarkModeScript(view, effectiveWebsiteDarkMode)
                    injectNotificationBridgeScript(view, tabId, isIncognito)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val resolvedUrl = url ?: view?.url.orEmpty()
                currentPageUrls[tabId] = resolvedUrl
                if (view != null) {
                    injectFloatingGlassSafeInsets(view)
                    injectUniversalDarkModeScript(view, effectiveWebsiteDarkMode)
                    injectNotificationBridgeScript(view, tabId, isIncognito)
                }
                callbacks?.onPageFinished(
                    tabId,
                    resolvedUrl,
                    view?.title.orEmpty(),
                    view?.canGoBack() == true,
                    view?.canGoForward() == true
                )
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                val resolvedUrl = url ?: view?.url.orEmpty()
                currentPageUrls[tabId] = resolvedUrl
                callbacks?.onNavigationStateChanged(
                    tabId,
                    resolvedUrl,
                    view?.canGoBack() == true,
                    view?.canGoForward() == true
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.description?.toString() ?: "Unable to reach server"
                    } else {
                        "Unable to reach server"
                    }
                    callbacks?.onNetworkError(
                        tabId,
                        request.url?.toString().orEmpty(),
                        description
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                if (handler == null) return
                val host = UrlResolver.extractHost(error?.url ?: view?.url)
                    .ifBlank { "Unknown host" }
                val primaryError = error?.primaryError ?: SslError.SSL_INVALID
                // Never blindly call handler.proceed(); delegate to explicit user warning dialog
                callbacks?.onSslError(tabId, host, primaryError, handler)
            }

            override fun onSafeBrowsingHit(
                view: WebView?,
                request: WebResourceRequest?,
                threatType: Int,
                callback: SafeBrowsingResponse?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && callback != null) {
                    callback.showInterstitial(true)
                } else {
                    super.onSafeBrowsingHit(view, request, threatType, callback)
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                // Gracefully recover without crashing the app
                val wasIncognito = incognitoTabIds.contains(tabId)
                synchronized(webViewLock) { webViews.remove(tabId) }?.let {
                    destroyWebViewSafely(it, clearData = wasIncognito)
                }
                appliedSettingsCache.remove(tabId)
                callbacks?.onNetworkError(
                    tabId,
                    currentPageUrls[tabId].orEmpty(),
                    "The webpage renderer stopped unexpectedly. Tap Reload to recover."
                )
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun getDefaultVideoPoster(): Bitmap = defaultVideoPosterBitmap

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                callbacks?.onProgressChanged(tabId, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    callbacks?.onTitleChanged(tabId, title)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    callbacks?.onFullscreenVideoShow(view, callback)
                }
            }

            override fun onHideCustomView() {
                callbacks?.onFullscreenVideoHide()
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback == null) return false
                val acceptTypes = fileChooserParams?.acceptTypes ?: arrayOf("*/*")
                callbacks?.onFileChooserRequested(filePathCallback, acceptTypes)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val resources = request.resources ?: emptyArray()
                // Auto-grant Widevine / EME Protected Media ID so HTML5 & streaming videos play seamlessly
                val onlyProtectedMedia = resources.isNotEmpty() &&
                    resources.all { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
                if (onlyProtectedMedia) {
                    runCatching { request.grant(resources) }
                    return
                }
                val origin = request.origin?.toString().orEmpty()
                callbacks?.onSitePermissionRequested(origin, resources, request)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (origin != null && callback != null) {
                    callbacks?.onGeolocationRequested(origin, callback)
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (currentSettings.popupBlockingEnabled && !isUserGesture) {
                    return false
                }
                val hitResult = view?.hitTestResult
                val extraUrl = hitResult?.extra
                if (!extraUrl.isNullOrBlank() && (extraUrl.startsWith("http://") || extraUrl.startsWith("https://"))) {
                    callbacks?.onOpenNewTabUrl(extraUrl, isIncognito)
                    return true
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                if (transport != null && view != null) {
                    val tempWebView = WebView(view.context).apply {
                        webViewClient = object : WebViewClient() {
                            private var dispatched = false
                            override fun shouldOverrideUrlLoading(
                                childView: WebView?,
                                childReq: WebResourceRequest?
                            ): Boolean {
                                val target = childReq?.url?.toString().orEmpty()
                                if (!dispatched && (target.startsWith("http://") || target.startsWith("https://"))) {
                                    dispatched = true
                                    callbacks?.onOpenNewTabUrl(target, isIncognito)
                                    childView?.let { wv ->
                                        wv.post { destroyWebViewSafely(wv, clearData = isIncognito) }
                                    }
                                }
                                return true
                            }
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                    // Safety cleanup if JS never issues a navigation URL
                    tempWebView.postDelayed({ destroyWebViewSafely(tempWebView, clearData = isIncognito) }, 4000L)
                    return true
                }
                return false
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applySettingsToWebView(
        webView: WebView,
        settings: BrowserSettings,
        isIncognito: Boolean,
        desktopMode: Boolean
    ) {
        webView.settings.apply {
            javaScriptEnabled = settings.javascriptEnabled
            // DOM Storage is required by modern HTML5 & YouTube/streaming video players
            domStorageEnabled = true
            databaseEnabled = !isIncognito
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = !settings.popupBlockingEnabled
            textZoom = settings.textZoomPercent
            mixedContentMode = if (settings.httpsOnlyMode) {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

            // Security hardening: disable unsafe local file access while preserving content media playback
            allowFileAccess = false
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            runCatching { WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, true) }
        }

        applyUserAgent(webView, desktopMode)

        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(settings.cookiesEnabled)
            // Allow embedded video players (YouTube/Vimeo/etc.) while AdBlockEngine strips known trackers
            cookieManager.setAcceptThirdPartyCookies(
                webView,
                settings.cookiesEnabled && !isIncognito
            )
        }

        applyDarkModeToWebView(webView, effectiveWebsiteDarkMode)
    }

    @Suppress("DEPRECATION")
    private fun applyDarkModeToWebView(webView: WebView, darkEnabled: Boolean) {
        webView.setBackgroundColor(
            if (darkEnabled) android.graphics.Color.parseColor("#0A0E1B")
            else android.graphics.Color.WHITE
        )

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            runCatching {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, darkEnabled)
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            runCatching {
                WebSettingsCompat.setForceDark(
                    webView.settings,
                    if (darkEnabled) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            runCatching {
                WebSettingsCompat.setForceDarkStrategy(
                    webView.settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
        }
    }

    /**
     * Dynamically adapts full-bleed WebView top & bottom safe-area padding to the user's
     * Top Bar & Bottom Bar visibility preferences, updating in-place without reloading.
     */
    private fun injectFloatingGlassSafeInsets(webView: WebView) {
        val topPadCss = if (currentSettings.showTopBar) "max(84px, env(safe-area-inset-top, 0px))" else "env(safe-area-inset-top, 0px)"
        val scrollPadTopCss = if (currentSettings.showTopBar) "94px" else "12px"
        val bottomPadCss = if (currentSettings.showBottomBar) "92px" else "0px"
        val js = """
            (function() {
                try {
                    var id = '__nova_floating_safe_insets__';
                    var body = document.body;
                    if (!body) return;
                    var cs = getComputedStyle(body);
                    // Do not shift full-viewport locked web apps or video players
                    if (cs.overflowY === 'hidden' && document.documentElement.scrollHeight <= window.innerHeight + 20) {
                        var existingLocked = document.getElementById(id);
                        if (existingLocked) existingLocked.remove();
                        return;
                    }
                    var style = document.getElementById(id);
                    if (!style) {
                        style = document.createElement('style');
                        style.id = id;
                        (document.head || document.documentElement).appendChild(style);
                    }
                    style.textContent =
                        'html { scroll-padding-top: $scrollPadTopCss !important; } ' +
                        'body { padding-top: $topPadCss !important; ' +
                        'padding-bottom: $bottomPadCss !important; box-sizing: border-box !important; }';
                } catch (e) {}
            })();
        """.trimIndent()
        runCatching { webView.evaluateJavascript(js, null) }
    }

    /**
     * Video-safe Universal Dark Mode CSS + Smart Light-Surface Detector for ALL websites:
     * - NEVER uses `html { filter: invert(...) }` (which breaks HTML5 <video> players & position: fixed).
     * - Detects light-background containers (including dynamic SPA divs, cards, modals, menus, popups)
     *   and darkens them via MutationObserver so bright white patches never remain in Dark Mode.
     * - Leaves <video>, <audio>, <img>, <canvas>, <svg>, and <iframe> 100% untouched.
     */
    private fun injectUniversalDarkModeScript(webView: WebView, darkEnabled: Boolean) {
        val js = if (!darkEnabled) {
            """
            (function() {
                if (window.__novaDarkObserver) {
                    window.__novaDarkObserver.disconnect();
                    window.__novaDarkObserver = null;
                }
                var existing = document.getElementById('__nova_universal_dark_mode__');
                if (existing) existing.remove();
                var meta = document.getElementById('__nova_color_scheme_meta__');
                if (meta) meta.remove();
                if (document.documentElement) {
                    document.documentElement.removeAttribute('data-nova-dark');
                }
                document.querySelectorAll('[data-nova-dark-surface]').forEach(function(el) {
                    el.removeAttribute('data-nova-dark-surface');
                });
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                try {
                    var styleId = '__nova_universal_dark_mode__';
                    var root = document.documentElement;
                    if (!root) return;

                    if (!document.getElementById('__nova_color_scheme_meta__')) {
                        var meta = document.createElement('meta');
                        meta.id = '__nova_color_scheme_meta__';
                        meta.name = 'color-scheme';
                        meta.content = 'dark';
                        (document.head || root).appendChild(meta);
                    }

                    function parseRgbLum(bg) {
                        if (!bg || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)') return null;
                        var m = bg.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?/);
                        if (!m) return null;
                        var alpha = m[4] !== undefined ? parseFloat(m[4]) : 1;
                        if (alpha < 0.12) return null;
                        var r = parseInt(m[1], 10) / 255;
                        var g = parseInt(m[2], 10) / 255;
                        var b = parseInt(m[3], 10) / 255;
                        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
                    }

                    function scanLightSurfaces() {
                        var body = document.body;
                        var bodyLum = body ? parseRgbLum(getComputedStyle(body).backgroundColor) : null;
                        var rootLum = parseRgbLum(getComputedStyle(root).backgroundColor);
                        var baseLum = bodyLum !== null ? bodyLum : rootLum;

                        if (!document.getElementById(styleId)) {
                            var style = document.createElement('style');
                            style.id = styleId;
                            style.textContent =
                                ':root { color-scheme: dark !important; } ' +
                                'html[data-nova-dark="1"], html[data-nova-dark="1"] body { background-color: #0b0f1b !important; color: #e5ecff !important; } ' +
                                'html[data-nova-dark="1"] [data-nova-dark-surface="1"] { ' +
                                '  background-color: #101629 !important; background-image: none !important; color: #e5ecff !important; border-color: rgba(148,163,184,0.24) !important; ' +
                                '} ' +
                                'html[data-nova-dark="1"] [data-nova-dark-surface="1"] * { color: #e2e8f8 !important; border-color: rgba(148,163,184,0.20) !important; } ' +
                                'html[data-nova-dark="1"] [data-nova-dark-surface="1"] a, html[data-nova-dark="1"] a { color: #7dd3fc !important; } ' +
                                'html[data-nova-dark="1"] main, html[data-nova-dark="1"] article, html[data-nova-dark="1"] section, ' +
                                'html[data-nova-dark="1"] header, html[data-nova-dark="1"] nav, html[data-nova-dark="1"] footer, ' +
                                'html[data-nova-dark="1"] aside, html[data-nova-dark="1"] table, html[data-nova-dark="1"] tr, ' +
                                'html[data-nova-dark="1"] td, html[data-nova-dark="1"] th, html[data-nova-dark="1"] dialog { ' +
                                '  border-color: rgba(148,163,184,0.22) !important; ' +
                                '} ' +
                                'html[data-nova-dark="1"] p, html[data-nova-dark="1"] h1, html[data-nova-dark="1"] h2, html[data-nova-dark="1"] h3, ' +
                                'html[data-nova-dark="1"] h4, html[data-nova-dark="1"] h5, html[data-nova-dark="1"] h6, ' +
                                'html[data-nova-dark="1"] li, html[data-nova-dark="1"] label { color: #e2e8f8 !important; } ' +
                                'html[data-nova-dark="1"] input, html[data-nova-dark="1"] textarea, html[data-nova-dark="1"] select { ' +
                                '  background-color: #161f36 !important; color: #f1f5ff !important; border-color: rgba(148,163,184,0.34) !important; ' +
                                '} ' +
                                'html[data-nova-dark="1"] video, html[data-nova-dark="1"] audio, html[data-nova-dark="1"] img, ' +
                                'html[data-nova-dark="1"] picture, html[data-nova-dark="1"] canvas, html[data-nova-dark="1"] svg, html[data-nova-dark="1"] iframe { ' +
                                '  filter: none !important; ' +
                                '}';
                            (document.head || root).appendChild(style);
                        }

                        if (baseLum === null || baseLum >= 0.34) {
                            root.setAttribute('data-nova-dark', '1');
                        }

                        if (!body) return;
                        var candidates = body.querySelectorAll('div, section, article, aside, header, nav, footer, main, ul, ol, li, form, fieldset, table, td, th, pre, code, blockquote, details, summary, dialog, button, [role="dialog"], [role="menu"], [role="listbox"], [role="banner"]');
                        var limit = Math.min(candidates.length, 600);
                        for (var i = 0; i < limit; i++) {
                            var el = candidates[i];
                            if (el.hasAttribute('data-nova-dark-surface')) continue;
                            var cs = getComputedStyle(el);
                            var lum = parseRgbLum(cs.backgroundColor);
                            var bgImg = cs.backgroundImage || '';
                            var hasPhotoUrl = bgImg.indexOf('url(') !== -1;
                            if (lum !== null && lum > 0.42 && !hasPhotoUrl) {
                                el.setAttribute('data-nova-dark-surface', '1');
                                root.setAttribute('data-nova-dark', '1');
                            }
                        }
                    }

                    scanLightSurfaces();
                    setTimeout(scanLightSurfaces, 350);

                    if (!window.__novaDarkObserver && document.body) {
                        var scheduled = false;
                        window.__novaDarkObserver = new MutationObserver(function() {
                            if (scheduled) return;
                            scheduled = true;
                            requestAnimationFrame(function() {
                                scheduled = false;
                                scanLightSurfaces();
                            });
                        });
                        window.__novaDarkObserver.observe(document.body, { childList: true, subtree: true });
                    }
                } catch (e) {}
            })();
            """.trimIndent()
        }
        runCatching { webView.evaluateJavascript(js, null) }
    }

    fun resolveJsNotificationPermission(tabId: String, callbackId: String, granted: Boolean) {
        val wv = synchronized(webViewLock) { webViews[tabId] } ?: return
        val js = "window.__novaResolveNotifyPerm && window.__novaResolveNotifyPerm(${JSONObject.quote(callbackId)}, $granted);"
        runCatching { wv.evaluateJavascript(js, null) }
    }

    private fun injectNotificationBridgeScript(webView: WebView, tabId: String, isIncognito: Boolean) {
        val currentUrl = currentPageUrls[tabId].orEmpty()
        if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) return
        val js = """
            (function() {
                try {
                    if (window.__novaNotifyPatched) return;
                    window.__novaNotifyPatched = true;
                    window.__novaNotifyCallbacks = {};
                    window.__novaResolveNotifyPerm = function(id, granted) {
                        var cb = window.__novaNotifyCallbacks[id];
                        if (cb) {
                            delete window.__novaNotifyCallbacks[id];
                            cb(granted ? 'granted' : 'denied');
                        }
                    };
                    var isIncog = $isIncognito;
                    function NovaWebNotification(title, options) {
                        var opts = options || {};
                        if (!isIncog && window.__novaNotifyBridge) {
                            window.__novaNotifyBridge.showNotification(window.location.href, String(title || ''), String(opts.body || ''));
                        }
                    }
                    Object.defineProperty(NovaWebNotification, 'permission', {
                        get: function() {
                            if (isIncog || !window.__novaNotifyBridge) return 'denied';
                            return window.__novaNotifyBridge.getPermissionState(window.location.href) || 'default';
                        }
                    });
                    NovaWebNotification.requestPermission = function(legacyCb) {
                        return new Promise(function(resolve) {
                            if (isIncog || !window.__novaNotifyBridge) {
                                if (typeof legacyCb === 'function') legacyCb('denied');
                                resolve('denied');
                                return;
                            }
                            var state = window.__novaNotifyBridge.getPermissionState(window.location.href);
                            if (state === 'granted' || state === 'denied') {
                                if (typeof legacyCb === 'function') legacyCb(state);
                                resolve(state);
                                return;
                            }
                            var id = 'np_' + Date.now() + '_' + Math.floor(Math.random() * 100000);
                            window.__novaNotifyCallbacks[id] = function(res) {
                                if (typeof legacyCb === 'function') legacyCb(res);
                                resolve(res);
                            };
                            window.__novaNotifyBridge.requestPermission(window.location.href, id);
                        });
                    };
                    window.Notification = NovaWebNotification;
                } catch (e) {}
            })();
        """.trimIndent()
        runCatching { webView.evaluateJavascript(js, null) }
    }

    private inner class NovaNotifyJsBridge(
        private val isIncognito: Boolean
    ) {
        @JavascriptInterface
        fun getPermissionState(url: String?): String {
            if (isIncognito) return "denied"
            val host = UrlResolver.extractHost(url).removePrefix("www.")
            if (host.isBlank()) return "denied"
            return when (callbacks?.getSiteNotificationAllowed(host)) {
                true -> "granted"
                false -> "denied"
                null -> "default"
            }
        }

        @JavascriptInterface
        fun requestPermission(url: String?, callbackId: String?) {
            val cbId = callbackId.orEmpty()
            if (isIncognito || url.isNullOrBlank()) {
                mainHandler.post {
                    snapshotWebViews().forEach { (id, _) ->
                        resolveJsNotificationPermission(id, cbId, false)
                    }
                }
                return
            }
            val host = UrlResolver.extractHost(url).removePrefix("www.")
            mainHandler.post {
                val matchingTabId = currentPageUrls.entries.firstOrNull {
                    UrlResolver.extractHost(it.value).removePrefix("www.") == host
                }?.key ?: lastResumedTabId.orEmpty()
                callbacks?.onSiteNotificationPermissionRequested(matchingTabId, url, host, cbId)
            }
        }

        @JavascriptInterface
        fun showNotification(url: String?, title: String?, body: String?) {
            if (isIncognito || url.isNullOrBlank()) return
            val host = UrlResolver.extractHost(url).removePrefix("www.")
            if (host.isBlank()) return
            mainHandler.post {
                callbacks?.onSiteNotificationPosted(host, title.orEmpty(), body.orEmpty(), url)
            }
        }
    }

    private fun applyUserAgent(webView: WebView, desktopMode: Boolean) {
        val defaultUa = WebSettings.getDefaultUserAgent(appContext)
        val targetUa = if (desktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        } else {
            defaultUa
        }
        if (webView.settings.userAgentString != targetUa) {
            webView.settings.userAgentString = targetUa
        }
    }

    private fun handleExternalUri(view: WebView?, uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme.isBlank() || scheme == "file" || scheme == "content") {
            return true
        }
        return try {
            val intent = if (scheme == "intent") {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                    component = null
                    selector = null
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    flags = flags and (
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        ).inv()
                    if (`package` == appContext.packageName) {
                        `package` = null
                    }
                }
            } else {
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                appContext.startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                val fallback = if (scheme == "intent") intent.getStringExtra("browser_fallback_url") else null
                if (!fallback.isNullOrBlank() && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                    view?.loadUrl(fallback)
                } else {
                    callbacks?.onExternalIntentError("No app installed to open $scheme link.")
                }
            }
            true
        } catch (_: Exception) {
            callbacks?.onExternalIntentError("No app installed to open $scheme link.")
            true
        }
    }

    private fun evictOldestIfNeeded(excludeTabId: String) {
        val evicted: Pair<String, WebView>? = synchronized(webViewLock) {
            if (webViews.size < maxCachedWebViews) return@synchronized null
            val oldestKey = webViews.keys.firstOrNull { it != excludeTabId } ?: return@synchronized null
            webViews.remove(oldestKey)?.let { oldestKey to it }
        }
        evicted?.let { (oldestKey, wv) ->
            val wasIncognito = incognitoTabIds.remove(oldestKey)
            appliedSettingsCache.remove(oldestKey)
            destroyWebViewSafely(wv, clearData = wasIncognito)
        }
    }

    private fun destroyWebViewSafely(webView: WebView, clearData: Boolean) {
        runCatching {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            if (clearData) {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
