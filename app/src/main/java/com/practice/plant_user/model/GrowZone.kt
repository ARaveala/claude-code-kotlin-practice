package com.practice.plant_user.model

import com.practice.plant_user.data.GrowZoneType

data class GrowZone(
    val id: Long,
    val parentGrowZoneId: Long?,
    val type: GrowZoneType,
    val name: String,
    val widthCm: Double,
    val depthCm: Double,
    val heightCm: Double?,
    val xCm: Double,
    val yCm: Double,
)