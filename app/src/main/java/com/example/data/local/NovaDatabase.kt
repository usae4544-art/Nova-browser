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
        SiteNotificationRuleEntity::class,
        ReviewEntity::class,
        UserAccountEntity::class
    ],
    version = 3,
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
    abstract fun reviewDao(): ReviewDao
    abstract fun userAccountDao(): UserAccountDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reviews` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`authorId` TEXT NOT NULL, " +
                        "`authorName` TEXT NOT NULL, " +
                        "`authorAvatarHex` TEXT NOT NULL, " +
                        "`rating` INTEGER NOT NULL, " +
                        "`comment` TEXT NOT NULL, " +
                        "`likesCount` INTEGER NOT NULL, " +
                        "`isOwnReview` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_accounts` (" +
                        "`userId` TEXT PRIMARY KEY NOT NULL, " +
                        "`username` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, " +
                        "`avatarColorHex` TEXT NOT NULL, " +
                        "`bio` TEXT NOT NULL, " +
                        "`isLoggedIn` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val defaultReviews: List<ReviewEntity> = listOf(
            ReviewEntity(
                authorId = "user_priya",
                authorName = "Priya Sharma",
                authorAvatarHex = "#EC4899",
                rating = 5,
                comment = "The Liquid Glass design is so smooth and futuristic! NetShield DoH VPN blocks every tracker completely.",
                likesCount = 42,
                isOwnReview = false,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            ),
            ReviewEntity(
                authorId = "user_aravind",
                authorName = "Aravind Kumar",
                authorAvatarHex = "#3B82F6",
                rating = 5,
                comment = "Best browser on Android! Download speed is blazing fast and the typewriter typing animations are so satisfying.",
                likesCount = 28,
                isOwnReview = false,
                createdAt = System.currentTimeMillis() - 86400000L * 5
            ),
            ReviewEntity(
                authorId = "user_alex",
                authorName = "Alex Mercer",
                authorAvatarHex = "#10B981",
                rating = 5,
                comment = "Chromium performance with iPhone iOS 26 glass aesthetic and sakuga anime particles. Absolutely 10/10.",
                likesCount = 35,
                isOwnReview = false,
                createdAt = System.currentTimeMillis() - 86400000L * 9
            ),
            ReviewEntity(
                authorId = "user_zainab",
                authorName = "Zainab Fatima",
                authorAvatarHex = "#F59E0B",
                rating = 5,
                comment = "Private Incognito vault with biometric lock and all-file download support is a game changer for daily browsing.",
                likesCount = 19,
                isOwnReview = false,
                createdAt = System.currentTimeMillis() - 86400000L * 14
            ),
            ReviewEntity(
                authorId = "user_marcus",
                authorName = "Marcus Vance",
                authorAvatarHex = "#8B5CF6",
                rating = 4,
                comment = "Liquid Lens tab bar feels ultra responsive. Very clean ad blocking and zero lag with dark websites.",
                likesCount = 12,
                isOwnReview = false,
                createdAt = System.currentTimeMillis() - 86400000L * 20
            )
        )

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
