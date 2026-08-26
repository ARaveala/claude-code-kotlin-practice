package com.practice.plant_user.data

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(entities = [AreaEntity::class, GrowZoneEntity::class], version = 1, exportSchema = false)
@ColumnTypeConverters(Converters::class)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun areaDao(): AreaDao
    abstract fun growZoneDao(): GrowZoneDao

    companion object {
        @Volatile private var instance: GardenDatabase? = null

        fun getInstance(context: Context): GardenDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GardenDatabase::class.java,
                    "garden_database",
                )
                    .setDriver(AndroidSQLiteDriver())
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
