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
