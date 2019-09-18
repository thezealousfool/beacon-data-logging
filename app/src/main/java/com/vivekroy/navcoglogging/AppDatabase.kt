package com.vivekroy.navcoglogging

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BeaconEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beaconDao(): BeaconDao
}