package com.practice.plantUser.data

import com.practice.plantUser.model.GrowZoneType

fun GrowZoneType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
