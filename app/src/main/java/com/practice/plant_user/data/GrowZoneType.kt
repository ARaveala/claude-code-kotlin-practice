package com.practice.plant_user.data

import com.practice.plant_user.model.GrowZoneType
fun GrowZoneType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
