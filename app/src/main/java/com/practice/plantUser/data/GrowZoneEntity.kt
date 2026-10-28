package com.practice.plantUser.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.GrowZoneType

@Entity(
    tableName = "grow_zones",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GrowZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentGrowZoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("areaId"), Index("parentGrowZoneId")]
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
    val yCm: Double
)

/** Row -> domain model. The one place this conversion is written, so a new column is added here
 * next to the entity, not in every caller. boundsEnforced isn't on GrowZone yet (Phase 3d). */
fun GrowZoneEntity.toModel(): GrowZone = GrowZone(
    id = id,
    parentGrowZoneId = parentGrowZoneId,
    type = type,
    name = name,
    widthCm = widthCm,
    depthCm = depthCm,
    heightCm = heightCm,
    xCm = xCm,
    yCm = yCm
)
