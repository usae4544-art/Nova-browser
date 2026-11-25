package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val folder: String = "Favorites",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileName: String,
    val url: String,
    val filePath: String,
    val mimeType: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: String = DownloadStatus.QUEUED.name,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val downloadStatus: DownloadStatus
        get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED)

    val progressFraction: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val sourceUrl: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "adblock_rules")
data class AdBlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pattern: String,
    val isAllowlist: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val accentHex: String = "#38BDF8",
    val sortOrder: Int = 0
)

@Entity(tableName = "site_notification_rules")
data class SiteNotificationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val host: String,
    val allowed: Boolean = true,
    val soundId: String = "GLASS_CHIME",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val authorId: String,
    val authorName: String,
    val authorAvatarHex: String = "#38BDF8",
    val rating: Int, // 1 to 5 stars
    val comment: String,
    val likesCount: Int = 0,
    val isOwnReview: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarColorHex: String = "#8B5CF6",
    val bio: String = "Nova Browser Explorer",
    val isLoggedIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
