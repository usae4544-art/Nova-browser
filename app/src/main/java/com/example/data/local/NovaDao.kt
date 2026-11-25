package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 300")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadEntity): Long

    @Update
    suspend fun update(item: DownloadEntity)

    @Query(
        "UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, " +
            "status = :status, errorMessage = :errorMessage WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Long,
        downloadedBytes: Long,
        totalBytes: Long,
        status: String,
        errorMessage: String? = null
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<QuickNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: QuickNoteEntity): Long

    @Query("DELETE FROM quick_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AdBlockRuleDao {
    @Query("SELECT * FROM adblock_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<AdBlockRuleEntity>>

    @Query("SELECT * FROM adblock_rules WHERE pattern = :pattern AND isAllowlist = :isAllowlist LIMIT 1")
    suspend fun getByPattern(pattern: String, isAllowlist: Boolean): AdBlockRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AdBlockRuleEntity): Long

    @Query("UPDATE adblock_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM adblock_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM adblock_rules WHERE pattern = :pattern AND isAllowlist = :isAllowlist")
    suspend fun deleteByPattern(pattern: String, isAllowlist: Boolean)
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC, id ASC")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShortcutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShortcutEntity): Long

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SiteNotificationRuleDao {
    @Query("SELECT * FROM site_notification_rules ORDER BY updatedAt DESC")
    fun getAllRules(): Flow<List<SiteNotificationRuleEntity>>

    @Query("SELECT * FROM site_notification_rules WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): SiteNotificationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SiteNotificationRuleEntity): Long

    @Query("DELETE FROM site_notification_rules WHERE host = :host")
    suspend fun deleteByHost(host: String)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY isOwnReview DESC, createdAt DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE isOwnReview = 1 LIMIT 1")
    fun getOwnReview(): Flow<ReviewEntity?>

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReviewEntity?

    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: ReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<ReviewEntity>)

    @Update
    suspend fun update(review: ReviewEntity)

    @Query("UPDATE reviews SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementLike(id: Long)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveAccount(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts ORDER BY isLoggedIn DESC, createdAt DESC")
    fun getAllAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
    suspend fun getById(userId: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getByUsername(username: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: UserAccountEntity)

    @Query("UPDATE user_accounts SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE user_accounts SET isLoggedIn = 1 WHERE userId = :userId")
    suspend fun setLoggedIn(userId: String)

    @Query("DELETE FROM user_accounts WHERE userId = :userId")
    suspend fun deleteById(userId: String)
}
