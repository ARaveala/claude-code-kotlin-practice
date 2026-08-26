package com.practice.plant_user.data

/** GrowZone container type, drives nesting rules (see Docs/domain_model.md). */
enum class GrowZoneType { GREENHOUSE, PLOT, BOX, WILD }

fun GrowZoneType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
