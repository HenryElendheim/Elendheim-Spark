package com.elendheim.spark.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The Room database. Three small tables: decks, wheels, and the vault.
 *
 * A single shared instance is handed out through [get] so the whole app talks
 * to one database.
 */
@Database(
    entities = [DeckEntity::class, WheelEntity::class, SavedCollisionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SparkDatabase : RoomDatabase() {

    abstract fun dao(): SparkDao

    companion object {
        @Volatile
        private var instance: SparkDatabase? = null

        fun get(context: Context): SparkDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SparkDatabase::class.java,
                    "elendheim_spark.db"
                ).build().also { instance = it }
            }
    }
}
