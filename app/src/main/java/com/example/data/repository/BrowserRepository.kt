package com.example.data.repository

import com.example.data.local.AdBlockRuleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.QuickNoteEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.ShortcutEntity
import com.example.data.local.SiteNotificationRuleEntity
import com.example.data.local.UserAccountEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.BrowserSettings
import com.example.data.preferences.SearchEngine
import com.example.engine.SecurityUtils
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

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
    val reviews: Flow<List<ReviewEntity>> = database.reviewDao().getAllReviews()
    val ownReview: Flow<ReviewEntity?> = database.reviewDao().getOwnReview()
    val activeAccount: Flow<UserAccountEntity?> = database.userAccountDao().getActiveAccount()
    val allAccounts: Flow<List<UserAccountEntity>> = database.userAccountDao().getAllAccounts()

    suspend fun ensureDefaultReviews() {
        if (database.reviewDao().count() == 0) {
            database.reviewDao().insertAll(NovaDatabase.defaultReviews)
        }
    }

    suspend fun addOrUpdateOwnReview(
        rating: Int,
        comment: String,
        authorName: String,
        authorAvatarHex: String,
        authorId: String
    ) {
        val existingOwn = database.reviewDao().getOwnReview().firstOrNull()
        if (existingOwn != null) {
            database.reviewDao().update(
                existingOwn.copy(
                    rating = rating,
                    comment = comment.trim(),
                    authorName = authorName.ifBlank { "Nova User" },
                    authorAvatarHex = authorAvatarHex,
                    authorId = authorId,
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            database.reviewDao().insert(
                ReviewEntity(
                    authorId = authorId,
                    authorName = authorName.ifBlank { "Nova User" },
                    authorAvatarHex = authorAvatarHex,
                    rating = rating,
                    comment = comment.trim(),
                    likesCount = 0,
                    isOwnReview = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun likeReview(id: Long) = database.reviewDao().incrementLike(id)

    suspend fun deleteReview(id: Long) = database.reviewDao().deleteById(id)

    suspend fun registerAccount(
        email: String,
        username: String,
        displayName: String,
        password: String,
        avatarColorHex: String = "#8B5CF6",
        bio: String = "Nova Browser Explorer"
    ): Result<UserAccountEntity> {
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim().removePrefix("@").lowercase()
            .ifBlank { cleanEmail.substringBefore("@").replace(Regex("[^a-z0-9._-]"), "_") }
        val cleanName = displayName.trim().ifBlank { cleanUsername }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (database.userAccountDao().getByEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with $cleanEmail already exists. Use Sign In."))
        }
        if (cleanUsername.isNotBlank() && database.userAccountDao().getByUsername(cleanUsername) != null) {
            return Result.failure(IllegalArgumentException("Username @$cleanUsername is already taken."))
        }
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)
        database.userAccountDao().logoutAll()
        val newAccount = UserAccountEntity(
            userId = "user_" + UUID.randomUUID().toString().take(8),
            username = cleanUsername.ifBlank { "nova_user" },
            email = cleanEmail,
            displayName = cleanName,
            avatarColorHex = avatarColorHex.ifBlank { "#8B5CF6" },
            bio = bio.trim().ifBlank { "Nova Browser Explorer" },
            passwordHash = hash,
            passwordSalt = salt,
            googleConnected = false,
            authProvider = "NOVA",
            isLoggedIn = true,
            createdAt = System.currentTimeMillis()
        )
        database.userAccountDao().insertOrUpdate(newAccount)
        return Result.success(newAccount)
    }

    suspend fun loginWithPassword(
        identifier: String,
        password: String
    ): Result<UserAccountEntity> {
        val clean = identifier.trim().removePrefix("@")
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your email or username."))
        }
        val existing = database.userAccountDao().getByEmail(clean)
            ?: database.userAccountDao().getByUsername(clean)
            ?: return Result.failure(IllegalArgumentException("No account found for \"$clean\". Create an account first."))

        if (existing.passwordHash.isNotBlank()) {
            if (!SecurityUtils.verifyPassword(password, existing.passwordHash, existing.passwordSalt)) {
                return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
            }
        }
        database.userAccountDao().logoutAll()
        val updated = existing.copy(isLoggedIn = true)
        database.userAccountDao().insertOrUpdate(updated)
        return Result.success(updated)
    }

    suspend fun switchActiveAccount(userId: String): UserAccountEntity? {
        val target = database.userAccountDao().getById(userId) ?: return null
        database.userAccountDao().logoutAll()
        database.userAccountDao().setLoggedIn(userId)
        return target.copy(isLoggedIn = true)
    }

    suspend fun connectOrSyncGoogleAccount(
        email: String? = null,
        displayName: String? = null,
        avatarColorHex: String = "#38BDF8"
    ): UserAccountEntity {
        val active = database.userAccountDao().getActiveAccount().firstOrNull()
        if (active != null) {
            val resolvedEmail = email?.trim()?.lowercase()?.takeIf { it.contains("@") } ?: active.email
            val resolvedName = displayName?.trim()?.takeIf { it.isNotBlank() } ?: active.displayName
            val updated = active.copy(
                email = resolvedEmail.ifBlank { active.email },
                displayName = resolvedName,
                googleConnected = true,
                isLoggedIn = true
            )
            database.userAccountDao().insertOrUpdate(updated)
            return updated
        }
        val cleanEmail = email?.trim()?.lowercase()?.takeIf { it.contains("@") }.orEmpty()
        val existingByEmail = if (cleanEmail.isNotBlank()) database.userAccountDao().getByEmail(cleanEmail) else null
        database.userAccountDao().logoutAll()
        if (existingByEmail != null) {
            val updated = existingByEmail.copy(
                displayName = displayName?.trim()?.takeIf { it.isNotBlank() } ?: existingByEmail.displayName,
                googleConnected = true,
                authProvider = "GOOGLE",
                isLoggedIn = true
            )
            database.userAccountDao().insertOrUpdate(updated)
            return updated
        }
        val derivedUser = cleanEmail.substringBefore("@").ifBlank { "google_user" }
        val derivedName = displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: cleanEmail.substringBefore("@").takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
            ?: "Google Account"
        val account = UserAccountEntity(
            userId = "google_" + UUID.randomUUID().toString().take(8),
            username = derivedUser,
            email = cleanEmail.ifBlank { "google-session@accounts.google.com" },
            displayName = derivedName,
            avatarColorHex = avatarColorHex,
            bio = "Signed in with Google across Nova Browser",
            googleConnected = true,
            authProvider = "GOOGLE",
            isLoggedIn = true,
            createdAt = System.currentTimeMillis()
        )
        database.userAccountDao().insertOrUpdate(account)
        return account
    }

    suspend fun disconnectGoogleAccount() {
        val active = database.userAccountDao().getActiveAccount().firstOrNull() ?: return
        database.userAccountDao().insertOrUpdate(active.copy(googleConnected = false))
    }

    suspend fun loginOrRegisterAccount(
        email: String,
        username: String,
        displayName: String,
        avatarColorHex: String = "#8B5CF6",
        bio: String = "Nova Browser Explorer"
    ): UserAccountEntity {
        database.userAccountDao().logoutAll()
        val cleanEmail = email.trim().lowercase()
        val existing = if (cleanEmail.isNotEmpty()) database.userAccountDao().getByEmail(cleanEmail) else null
        val account = if (existing != null) {
            val updated = existing.copy(
                username = username.ifBlank { existing.username },
                displayName = displayName.ifBlank { existing.displayName },
                avatarColorHex = avatarColorHex.ifBlank { existing.avatarColorHex },
                isLoggedIn = true
            )
            database.userAccountDao().insertOrUpdate(updated)
            updated
        } else {
            val newAccount = UserAccountEntity(
                userId = "user_" + UUID.randomUUID().toString().take(8),
                username = username.trim().ifBlank { cleanEmail.substringBefore("@").ifBlank { "nova_user" } },
                email = cleanEmail,
                displayName = displayName.trim().ifBlank { username.trim().ifBlank { "Nova Explorer" } },
                avatarColorHex = avatarColorHex,
                bio = bio,
                isLoggedIn = true,
                createdAt = System.currentTimeMillis()
            )
            database.userAccountDao().insertOrUpdate(newAccount)
            newAccount
        }
        return account
    }

    suspend fun logoutAccount() {
        database.userAccountDao().logoutAll()
    }

    suspend fun updateAccountProfile(displayName: String, bio: String, avatarColorHex: String) {
        val current = database.userAccountDao().getActiveAccount().firstOrNull() ?: return
        database.userAccountDao().insertOrUpdate(
            current.copy(
                displayName = displayName.trim().ifBlank { current.displayName },
                bio = bio.trim().ifBlank { current.bio },
                avatarColorHex = avatarColorHex.ifBlank { current.avatarColorHex }
            )
        )
    }

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
