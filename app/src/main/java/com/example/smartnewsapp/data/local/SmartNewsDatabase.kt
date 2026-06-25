package com.example.smartnewsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Article::class, InterestProfile::class, ChatMessage::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartNewsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun profileDao(): ProfileDao
    abstract fun chatDao(): ChatDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `interest_profile_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `keyword` TEXT NOT NULL, `score` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL)"
                )
                db.execSQL(
                    """
                    INSERT INTO interest_profile_new (keyword, score, lastUpdated)
                    SELECT
                        normalizedKeyword,
                        CASE
                            WHEN SUM(score) > 10 THEN 10
                            WHEN SUM(score) < -10 THEN -10
                            ELSE SUM(score)
                        END AS score,
                        MAX(lastUpdated) AS lastUpdated
                    FROM (
                        SELECT LOWER(TRIM(keyword)) AS normalizedKeyword, score, lastUpdated
                        FROM interest_profile
                        WHERE TRIM(keyword) != ''
                    )
                    GROUP BY normalizedKeyword
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE interest_profile")
                db.execSQL("ALTER TABLE interest_profile_new RENAME TO interest_profile")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_interest_profile_keyword` ON `interest_profile` (`keyword`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN feedback INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN feedRank INTEGER NOT NULL DEFAULT 2147483647")
            }
        }
    }
}
