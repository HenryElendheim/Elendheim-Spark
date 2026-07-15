package com.elendheim.spark.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The Room database. Three small tables: decks, wheels, and the vault.
 *
 * A single shared instance is handed out through [get] so the whole app talks
 * to one database.
 */
@Database(
    entities = [DeckEntity::class, WheelEntity::class, SavedCollisionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SparkDatabase : RoomDatabase() {

    abstract fun dao(): SparkDao

    companion object {
        @Volatile
        private var instance: SparkDatabase? = null

        // v1 -> v2: saved ideas gained a custom title and a save number. Add the
        // columns in place so nobody's existing vault is wiped on upgrade.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_collisions ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_collisions ADD COLUMN saveNumber INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): SparkDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SparkDatabase::class.java,
                    "elendheim_spark.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
