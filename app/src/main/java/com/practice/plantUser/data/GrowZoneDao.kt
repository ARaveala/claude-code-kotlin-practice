package com.practice.plantUser.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
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

    @Query("SELECT * FROM grow_zones WHERE id = :id")
    suspend fun getById(id: Long): GrowZoneEntity?

    // Targeted UPDATE, not @Update(entity): writes only size and position, so a stale entity copy
    // can never overwrite name, type or boundsEnforced. Returns rows changed (0 = no such zone).
    @Query(
        "UPDATE grow_zones SET widthCm = :widthCm, depthCm = :depthCm, heightCm = :heightCm, " +
            "xCm = :xCm, yCm = :yCm WHERE id = :id"
    )
    suspend fun updateSizeAndPosition(
        id: Long,
        widthCm: Double,
        depthCm: Double,
        heightCm: Double?,
        xCm: Double,
        yCm: Double
    ): Int

    // Direct children only: grandchildren are relative to their own parent, which doesn't move.
    // SQLite REAL is a 64-bit double, so this is the same subtraction Kotlin would do.
    @Query("UPDATE grow_zones SET xCm = xCm - :dxCm, yCm = yCm - :dyCm WHERE parentGrowZoneId = :parentId")
    suspend fun shiftChildren(parentId: Long, dxCm: Double, dyCm: Double): Int

    /** Resize plus the opposite shift on the direct children, so they stay put on screen when the
     * parent's x/y move (non-top-left anchor). One transaction: both are written or neither. The
     * caller computes every value first; this holds only the writes. Returns the parent's rows
     * changed (0 = no such zone, children untouched). */
    @Transaction
    suspend fun resizeKeepingChildrenInPlace(
        id: Long,
        widthCm: Double,
        depthCm: Double,
        heightCm: Double?,
        xCm: Double,
        yCm: Double,
        dxCm: Double,
        dyCm: Double
    ): Int {
        val rows = updateSizeAndPosition(id, widthCm, depthCm, heightCm, xCm, yCm)
        if (rows > 0) shiftChildren(id, dxCm, dyCm)
        return rows
    }
}
