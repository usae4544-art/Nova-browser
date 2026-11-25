package com.example.data.repository

import com.example.data.local.AdBlockRuleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.QuickNoteEntity
import com.example.data.local.ShortcutEntity
import com.example.data.local.SiteNotificationRuleEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.BrowserSettings
import com.example.data.preferences.SearchEngine
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow

class BrowserRepository(
    private val database: NovaDatabase,
    private val preferences: BrowserPreferences
) {
    val settings: Flow<BrowserSettings> = preferences.settingsFlow
    val bookmarks: Flow<List<BookmarkEntity>> = database.bookmarkDao().getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = database.historyDao().getRecentHistory()
    val downloads: Flow<List<DownloadEntity>> = database.downloadDao().getAllDownloads()
    val quickNotes: Flow<List<QuickNoteEntity>> = database.quickNoteDao().getAllNotes()
    val adBlockRules: Flow<List<AdBlockRuleEntity>> = database.adBlockRuleDao().getAllRules()
    val shortcuts: Flow<List<ShortcutEntity>> = database.shortcutDao().getAllShortcuts()
    val siteNotificationRules: Flow<List<SiteNotificationRuleEntity>> =
        database.siteNotificationRuleDao().getAllRules()

    suspend fun ensureDefaultShortcuts() {
        if (database.shortcutDao().count() == 0) {
            database.shortcutDao().insertAll(NovaDatabase.defaultShortcuts)
        }
    }

    suspend fun addShortcut(title: String, url: String, accentHex: String = "#38BDF8") {
        val count = database.shortcutDao().count()
        database.shortcutDao().insert(
            ShortcutEntity(
                title = title.trim(),
                url = url.trim(),
                accentHex = accentHex,
                sortOrder = count
            )
        )
    }

    suspend fun deleteShortcut(id: Long) = database.shortcutDao().deleteById(id)

    suspend fun toggleBookmark(title: String, url: String): Boolean {
        val existing = database.bookmarkDao().getByUrl(url)
        return if (existing != null) {
            database.bookmarkDao().deleteById(existing.id)
            false
        } else {
            database.bookmarkDao().insert(
                BookmarkEntity(
                    title = title.ifBlank { url },
                    url = url
                )
            )
            true
        }
    }

    suspend fun deleteBookmark(id: Long) = database.bookmarkDao().deleteById(id)

    suspend fun recordHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("nova:")) return
        database.historyDao().deleteByUrl(url)
        database.historyDao().insert(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = database.historyDao().deleteById(id)

    suspend fun clearHistory() = database.historyDao().clearAll()

    suspend fun saveQuickNote(note: QuickNoteEntity) = database.quickNoteDao().upsert(note)

    suspend fun deleteQuickNote(id: Long) = database.quickNoteDao().deleteById(id)

    suspend fun addAdBlockRule(pattern: String, isAllowlist: Boolean) {
        val normalized = pattern.trim().lowercase()
            .let { if (isAllowlist) it.removePrefix("www.") else it }
        if (normalized.isNotEmpty() && database.adBlockRuleDao().getByPattern(normalized, isAllowlist) == null) {
            database.adBlockRuleDao().insert(
                AdBlockRuleEntity(pattern = normalized, isAllowlist = isAllowlist)
            )
        }
    }

    suspend fun setAdBlockRuleEnabled(id: Long, enabled: Boolean) =
        database.adBlockRuleDao().setEnabled(id, enabled)

    suspend fun deleteAdBlockRule(id: Long) = database.adBlockRuleDao().deleteById(id)

    suspend fun removeAllowlistHost(host: String) =
        database.adBlockRuleDao().deleteByPattern(host.lowercase(), isAllowlist = true)

    suspend fun insertDownload(item: DownloadEntity): Long = database.downloadDao().insert(item)

    suspend fun getDownload(id: Long): DownloadEntity? = database.downloadDao().getById(id)

    suspend fun updateDownloadProgress(
        id: Long,
        downloadedBytes: Long,
        totalBytes: Long,
        status: String,
        errorMessage: String? = null
    ) = database.downloadDao().updateProgress(id, downloadedBytes, totalBytes, status, errorMessage)

    suspend fun deleteDownload(id: Long) = database.downloadDao().deleteById(id)

    suspend fun getSiteNotificationRule(host: String): SiteNotificationRuleEntity? {
        val clean = host.trim().lowercase().removePrefix("www.")
        if (clean.isEmpty()) return null
        return database.siteNotificationRuleDao().getByHost(clean)
    }

    suspend fun setSiteNotificationRule(host: String, allowed: Boolean, soundId: String) {
        val clean = host.trim().lowercase().removePrefix("www.")
        if (clean.isEmpty()) return
        val existing = database.siteNotificationRuleDao().getByHost(clean)
        database.siteNotificationRuleDao().upsert(
            SiteNotificationRuleEntity(
                id = existing?.id ?: 0L,
                host = clean,
                allowed = allowed,
                soundId = soundId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSiteNotificationRule(host: String) {
        val clean = host.trim().lowercase().removePrefix("www.")
        if (clean.isNotEmpty()) {
            database.siteNotificationRuleDao().deleteByHost(clean)
        }
    }

    // Settings delegates
    suspend fun updateThemeMode(mode: AppThemeMode) = preferences.updateThemeMode(mode)
    suspend fun updateSearchEngine(engine: SearchEngine) = preferences.updateSearchEngine(engine)
    suspend fun updateCustomHomepage(url: String) = preferences.updateCustomHomepage(url)
    suspend fun setAdBlockEnabled(enabled: Boolean) = preferences.setAdBlockEnabled(enabled)
    suspend fun setTrackerBlockEnabled(enabled: Boolean) = preferences.setTrackerBlockEnabled(enabled)
    suspend fun setHttpsOnlyMode(enabled: Boolean) = preferences.setHttpsOnlyMode(enabled)
    suspend fun setCookiesEnabled(enabled: Boolean) = preferences.setCookiesEnabled(enabled)
    suspend fun setThirdPartyCookiesEnabled(enabled: Boolean) = preferences.setThirdPartyCookiesEnabled(enabled)
    suspend fun setJavaScriptEnabled(enabled: Boolean) = preferences.setJavaScriptEnabled(enabled)
    suspend fun setPopupBlockingEnabled(enabled: Boolean) = preferences.setPopupBlockingEnabled(enabled)
    suspend fun setAutoDarkWebsites(enabled: Boolean) = preferences.setAutoDarkWebsites(enabled)
    suspend fun setShowTopBar(enabled: Boolean) = preferences.setShowTopBar(enabled)
    suspend fun setShowBottomBar(enabled: Boolean) = preferences.setShowBottomBar(enabled)
    suspend fun setTextZoomPercent(zoom: Int) = preferences.setTextZoomPercent(zoom)
    suspend fun setAllowCameraSites(enabled: Boolean) = preferences.setAllowCameraSites(enabled)
    suspend fun setAllowMicSites(enabled: Boolean) = preferences.setAllowMicSites(enabled)
    suspend fun setAllowLocationSites(enabled: Boolean) = preferences.setAllowLocationSites(enabled)
    suspend fun setNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)
    suspend fun setSiteNotificationSound(soundId: String) = preferences.setSiteNotificationSound(soundId)
    suspend fun setIncognitoBiometricEnabled(enabled: Boolean) = preferences.setIncognitoBiometricEnabled(enabled)
    suspend fun setIncognitoPin(hash: String, salt: String) = preferences.setIncognitoPin(hash, salt)
    suspend fun clearIncognitoPin() = preferences.clearIncognitoPin()
    suspend fun setVpnDnsServer(dns: String) = preferences.setVpnDnsServer(dns)
    suspend fun setVpnGatewayAddress(address: String) = preferences.setVpnGatewayAddress(address)
    suspend fun setVpnNetShieldEnabled(enabled: Boolean) = preferences.setVpnNetShieldEnabled(enabled)
    suspend fun setVpnDohEnabled(enabled: Boolean) = preferences.setVpnDohEnabled(enabled)
}
