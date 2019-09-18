package com.vivekroy.navcoglogging

import androidx.room.*

@Entity(tableName = "beacons")
data class BeaconEntity (
    @ColumnInfo(name = "uid") val id1: String,
    @ColumnInfo(name = "major") val id2: String,
    @ColumnInfo(name = "minor") val id3: String,
    @ColumnInfo(name = "rssi") val rssi: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long
) {
    @PrimaryKey(autoGenerate = true) var sl: Long = 0
}

@Dao
interface BeaconDao {
    @Insert
    fun insertBeacons(vararg beacons: BeaconEntity)
}
