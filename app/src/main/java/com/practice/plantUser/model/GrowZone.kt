package com.practice.plantUser.model

/** GrowZone container type, drives nesting rules (see Docs/domain_model.md). */
enum class GrowZoneType { GREENHOUSE, PLOT, BOX, WILD }

/** A position in real world cm. Stays Double end to end; Float only at draw time in ui/. */
data class PositionCm(
    val xCm: Double,
    val yCm: Double,
)

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
