package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.preferences.BrowserSettings
import org.json.JSONObject
import java.io.ByteArrayInputStream
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
    val onScrollDirectionChanged: (hideBars: Boolean) -> Unit = {}
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
    private val webViews = LinkedHashMap<String, WebView>(8, 0.75f, true)
    private val incognitoTabIds = ConcurrentHashMap.newKeySet<String>()
    private val currentPageUrls = ConcurrentHashMap<String, String>()
    private val homeAnchorIndices = ConcurrentHashMap<String, Int>()
    private val appliedSettingsCache = ConcurrentHashMap<String, Pair<BrowserSettings, Boolean>>()
    private var callbacks: TabWebEventCallbacks? = null
    private var currentSettings: BrowserSettings = BrowserSettings()
    @Volatile
    private var effectiveWebsiteDarkMode: Boolean = false
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

    fun attachCallbacks(cb: TabWebEventCallbacks) {
        callbacks = cb
    }

    /**
     * Immediately updates Dark Mode across ALL open websites (both native WebView darkening
     * and universal smart CSS dark mode) whenever phone system theme, app theme, or Dark Sites toggles.
     */
    fun updateWebsiteDarkMode(enabled: Boolean) {
        if (effectiveWebsiteDarkMode == enabled && webViews.isEmpty()) return
        effectiveWebsiteDarkMode = enabled
        webViews.forEach { (_, webView) ->
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
        webViews.forEach { (tabId, webView) ->
            applySettingsToWebView(
                webView = webView,
                settings = settings,
                isIncognito = incognitoTabIds.contains(tabId),
                desktopMode = (webView.tag as? Boolean) == true
            )
            if (barVisibilityChanged) {
                injectFloatingGlassSafeInsets(webView)
            }
        }
    }

    fun getOrCreateWebView(
        context: Context,
        tabId: String,
        isIncognito: Boolean,
        desktopMode: Boolean
    ): WebView {
        webViews[tabId]?.let { existing ->
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

        configureWebView(webView, tabId, isIncognito, desktopMode)
        appliedSettingsCache[tabId] = currentSettings to desktopMode
        lastResumedTabId = tabId
        webViews[tabId] = webView
        return webView
    }

    fun getExistingWebView(tabId: String): WebView? = webViews[tabId]

    fun getCertificateSummary(tabId: String, fallbackUrl: String): SiteCertificateSummary {
        val wv = webViews[tabId]
        val url = wv?.url?.takeIf { it.isNotBlank() } ?: fallbackUrl
        return SecurityUtils.buildCertificateSummary(url, wv?.certificate)
    }

    fun onActivityResume() {
        webViews.values.forEach { runCatching { it.onResume(); it.resumeTimers() } }
    }

    fun onActivityPause() {
        webViews.values.forEach { runCatching { it.onPause() } }
    }

    fun loadUrl(tabId: String, url: String) {
        currentPageUrls[tabId] = url
        val privacyHeaders = mapOf(
            "DNT" to "1",
            "Sec-GPC" to "1"
        )
        webViews[tabId]?.loadUrl(url, privacyHeaders)
    }

    /**
     * Marks the current WebView history index as the floor when user returns to NOVA Home,
     * so future navigations from Home go back to Home rather than pre-Home pages.
     */
    fun markHomeAnchor(tabId: String) {
        val wv = webViews[tabId] ?: return
        val historyList = wv.copyBackForwardList()
        homeAnchorIndices[tabId] = historyList.currentIndex + 1
    }

    fun canGoBackSmart(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return findPreviousDistinctHistoryIndex(wv, tabId) != null
    }

    /**
     * Navigates back inside the active website first, skipping about:blank and
     * redirect-duplicate entries so Back always lands on the previous real webpage.
     */
    fun goBackSmart(tabId: String): BackNavResult {
        val wv = webViews[tabId] ?: return BackNavResult(handled = false)
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
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoForward()) {
            wv.goForward()
            true
        } else {
            false
        }
    }

    fun reload(tabId: String) {
        webViews[tabId]?.reload()
    }

    fun stopLoading(tabId: String) {
        webViews[tabId]?.stopLoading()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val wv = webViews[tabId] ?: return
        wv.tag = enabled
        applyUserAgent(wv, enabled)
        wv.reload()
    }

    fun findInPage(tabId: String, query: String) {
        val wv = webViews[tabId] ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            callbacks?.onFindMatchesUpdated(0, 0)
        } else {
            wv.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        webViews[tabId]?.findNext(forward)
    }

    fun clearFindMatches(tabId: String) {
        webViews[tabId]?.clearMatches()
        callbacks?.onFindMatchesUpdated(0, 0)
    }

    fun extractReadableArticle(
        tabId: String,
        onResult: (title: String, byline: String, paragraphs: List<String>) -> Unit
    ) {
        val wv = webViews[tabId]
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
        webViews.remove(tabId)?.let { wv ->
            destroyWebViewSafely(wv, clearData = wasIncognito)
        }
        if (wasIncognito && incognitoTabIds.isEmpty()) {
            clearIncognitoSessionArtifacts()
        }
    }

    fun clearIncognitoSessionArtifacts() {
        runCatching {
            CookieManager.getInstance().removeSessionCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    fun clearAllBrowsingData() {
        webViews.values.forEach { wv ->
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
        }
    }

    fun destroyAll() {
        val all = webViews.values.toList()
        webViews.clear()
        incognitoTabIds.clear()
        currentPageUrls.clear()
        homeAnchorIndices.clear()
        appliedSettingsCache.clear()
        all.forEach { destroyWebViewSafely(it, clearData = false) }
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

                // Always allow standard web & media streaming schemes inside WebView
                if (scheme == "http" || scheme == "https" || scheme == "about" ||
                    scheme == "blob" || scheme == "data" || scheme == "javascript"
                ) {
                    return false
                }

                // External app intents (mailto:, tel:, intent://, market://, etc.)
                return handleExternalUri(uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val resolvedUrl = url ?: return
                currentPageUrls[tabId] = resolvedUrl
                callbacks?.onScrollDirectionChanged(false)
                callbacks?.onPageStarted(tabId, resolvedUrl)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                if (view != null) {
                    injectFloatingGlassSafeInsets(view)
                    injectUniversalDarkModeScript(view, effectiveWebsiteDarkMode)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val resolvedUrl = url ?: view?.url.orEmpty()
                currentPageUrls[tabId] = resolvedUrl
                if (view != null) {
                    injectFloatingGlassSafeInsets(view)
                    injectUniversalDarkModeScript(view, effectiveWebsiteDarkMode)
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
                webViews.remove(tabId)?.let { destroyWebViewSafely(it, clearData = false) }
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
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
     * Video-safe Universal Dark Mode CSS engine for ALL websites:
     * - NEVER uses `html { filter: invert(...) }` (which breaks HTML5 <video> players & position: fixed).
     * - Leaves websites that are already naturally dark untouched.
     * - Applies crisp semantic dark surfaces & typography to light websites while leaving
     *   <video>, <audio>, <img>, <canvas>, <svg>, and <iframe> 100% untouched.
     */
    private fun injectUniversalDarkModeScript(webView: WebView, darkEnabled: Boolean) {
        val js = if (!darkEnabled) {
            """
            (function() {
                var existing = document.getElementById('__nova_universal_dark_mode__');
                if (existing) existing.remove();
                if (document.documentElement) {
                    document.documentElement.removeAttribute('data-nova-dark');
                }
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                try {
                    function applyNovaDark() {
                        var styleId = '__nova_universal_dark_mode__';
                        var root = document.documentElement;
                        var body = document.body;
                        if (!root) return;

                        function parseRgbLum(bg) {
                            if (!bg || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)') return null;
                            var m = bg.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
                            if (!m) return null;
                            var r = parseInt(m[1], 10) / 255;
                            var g = parseInt(m[2], 10) / 255;
                            var b = parseInt(m[3], 10) / 255;
                            return 0.2126 * r + 0.7152 * g + 0.0722 * b;
                        }

                        var bodyLum = body ? parseRgbLum(getComputedStyle(body).backgroundColor) : null;
                        var rootLum = parseRgbLum(getComputedStyle(root).backgroundColor);
                        var lum = bodyLum !== null ? bodyLum : rootLum;

                        // If site is already genuinely dark (e.g. YouTube Dark, GitHub Dark), keep native dark theme
                        if (lum !== null && lum < 0.34 && !root.hasAttribute('data-nova-dark')) {
                            var oldStyle = document.getElementById(styleId);
                            if (oldStyle) oldStyle.remove();
                            return;
                        }

                        if (!document.getElementById(styleId)) {
                            var style = document.createElement('style');
                            style.id = styleId;
                            style.textContent =
                                'html[data-nova-dark="1"] { color-scheme: dark !important; background-color: #0b0f1b !important; } ' +
                                'html[data-nova-dark="1"] body { background-color: #0b0f1b !important; color: #e5ecff !important; } ' +
                                'html[data-nova-dark="1"] main, ' +
                                'html[data-nova-dark="1"] article, ' +
                                'html[data-nova-dark="1"] section, ' +
                                'html[data-nova-dark="1"] header, ' +
                                'html[data-nova-dark="1"] nav, ' +
                                'html[data-nova-dark="1"] footer, ' +
                                'html[data-nova-dark="1"] aside, ' +
                                'html[data-nova-dark="1"] table, ' +
                                'html[data-nova-dark="1"] tr, ' +
                                'html[data-nova-dark="1"] td, ' +
                                'html[data-nova-dark="1"] th { ' +
                                '  background-color: #0e1424 !important; color: #e5ecff !important; border-color: rgba(148,163,184,0.22) !important; ' +
                                '} ' +
                                'html[data-nova-dark="1"] p, ' +
                                'html[data-nova-dark="1"] h1, html[data-nova-dark="1"] h2, html[data-nova-dark="1"] h3, ' +
                                'html[data-nova-dark="1"] h4, html[data-nova-dark="1"] h5, html[data-nova-dark="1"] h6, ' +
                                'html[data-nova-dark="1"] li, html[data-nova-dark="1"] span, html[data-nova-dark="1"] label { ' +
                                '  color: #e2e8f8 !important; ' +
                                '} ' +
                                'html[data-nova-dark="1"] a { color: #7dd3fc !important; } ' +
                                'html[data-nova-dark="1"] input, html[data-nova-dark="1"] textarea, html[data-nova-dark="1"] select { ' +
                                '  background-color: #161f36 !important; color: #f1f5ff !important; border-color: rgba(148,163,184,0.32) !important; ' +
                                '} ' +
                                'html[data-nova-dark="1"] video, html[data-nova-dark="1"] audio, html[data-nova-dark="1"] img, ' +
                                'html[data-nova-dark="1"] picture, html[data-nova-dark="1"] canvas, html[data-nova-dark="1"] iframe { ' +
                                '  filter: none !important; background-color: transparent !important; ' +
                                '}';
                            (document.head || root).appendChild(style);
                        }
                        root.setAttribute('data-nova-dark', '1');
                    }
                    applyNovaDark();
                    setTimeout(applyNovaDark, 450);
                } catch (e) {}
            })();
            """.trimIndent()
        }
        runCatching { webView.evaluateJavascript(js, null) }
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

    private fun handleExternalUri(uri: Uri): Boolean {
        return try {
            val intent = if (uri.scheme.equals("intent", ignoreCase = true)) {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            callbacks?.onExternalIntentError("No app installed to open ${uri.scheme ?: "this"} link.")
            true
        }
    }

    private fun evictOldestIfNeeded(excludeTabId: String) {
        if (webViews.size < maxCachedWebViews) return
        val oldestKey = webViews.keys.firstOrNull { it != excludeTabId } ?: return
        webViews.remove(oldestKey)?.let { destroyWebViewSafely(it, clearData = false) }
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
