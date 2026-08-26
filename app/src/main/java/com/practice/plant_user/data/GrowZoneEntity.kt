package com.practice.plant_user.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "grow_zones",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GrowZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentGrowZoneId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("areaId"), Index("parentGrowZoneId")],
)
data class GrowZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val areaId: Long,
    val parentGrowZoneId: Long?,
    val type: GrowZoneType,
    val name: String,
    val widthCm: Double,
    val depthCm: Double,
    val heightCm: Double?,
    val boundsEnforced: Boolean = true,
    // Position relative to the parent GrowZone's local origin if nested, otherwise relative to
    // the Area canvas origin. No drag-to-place UI yet (Phase 5) — Phase 2 auto-places on creation.
    val xCm: Double,
    val yCm: Double,
)
