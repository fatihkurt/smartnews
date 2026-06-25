package com.example.smartnewsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Article::class, InterestProfile::class, ChatMessage::class],
    version = 6,
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
                // Create the new table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `interest_profile_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `keyword` TEXT NOT NULL, `score` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL)"
                )
                // Create unique index
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_interest_profile_keyword` ON `interest_profile_new` (`keyword`)")
                
                // Copy deduplicated data: we sum the scores and take the max lastUpdated
                db.execSQL(
                    "INSERT INTO interest_profile_new (keyword, score, lastUpdated) SELECT keyword, SUM(score), MAX(lastUpdated) FROM interest_profile GROUP BY keyword"
                )
                
                // Remove the old table
                db.execSQL("DROP TABLE interest_profile")
                
                // Change the table name to the correct one
                db.execSQL("ALTER TABLE interest_profile_new RENAME TO interest_profile")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN feedback INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
