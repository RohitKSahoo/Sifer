package com.rohit.sifer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones")
    fun getAllZones(): Flow<List<Zone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: Zone): Long

    @Delete
    suspend fun deleteZone(zone: Zone)

    @Update
    suspend fun updateZone(zone: Zone)
    
    @Query("SELECT * FROM zones WHERE id = :id")
    suspend fun getZoneById(id: Int): Zone?
}
