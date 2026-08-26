package com.practice.plant_user.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowZoneDao {
    @Insert
    suspend fun insert(zone: GrowZoneEntity): Long

    // Ascending id order guarantees a parent row always precedes its children — a new zone's
    // parent must already exist to be picked as the current focus, so id order is parent-before-
    // child order too. Rendering relies on this to draw parents before children.
    @Query("SELECT * FROM grow_zones WHERE areaId = :areaId ORDER BY id ASC")
    fun getByArea(areaId: Long): Flow<List<GrowZoneEntity>>
}
