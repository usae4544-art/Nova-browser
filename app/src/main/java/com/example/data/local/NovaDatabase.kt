package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        QuickNoteEntity::class,
        AdBlockRuleEntity::class,
        ShortcutEntity::class,
        SiteNotificationRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun adBlockRuleDao(): AdBlockRuleDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun siteNotificationRuleDao(): SiteNotificationRuleDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `site_notification_rules` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`host` TEXT NOT NULL, " +
                        "`allowed` INTEGER NOT NULL, " +
                        "`soundId` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_browser.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val defaultShortcuts: List<ShortcutEntity> = listOf(
            ShortcutEntity(title = "Google", url = "https://www.google.com", accentHex = "#38BDF8", sortOrder = 0),
            ShortcutEntity(title = "YouTube", url = "https://m.youtube.com", accentHex = "#FB7185", sortOrder = 1),
            ShortcutEntity(title = "Wikipedia", url = "https://en.m.wikipedia.org", accentHex = "#A78BFA", sortOrder = 2),
            ShortcutEntity(title = "GitHub", url = "https://github.com", accentHex = "#60A5FA", sortOrder = 3),
            ShortcutEntity(title = "Reddit", url = "https://www.reddit.com", accentHex = "#F97316", sortOrder = 4),
            ShortcutEntity(title = "Instagram", url = "https://www.instagram.com", accentHex = "#E879F9", sortOrder = 5),
            ShortcutEntity(title = "X", url = "https://x.com", accentHex = "#94A3B8", sortOrder = 6),
            ShortcutEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", accentHex = "#34D399", sortOrder = 7)
        )
    }
}
