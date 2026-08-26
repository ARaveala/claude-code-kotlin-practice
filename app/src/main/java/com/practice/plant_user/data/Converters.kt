package com.practice.plant_user.data

import androidx.room3.ColumnTypeConverter

/** Stores [GrowZoneType] by name, not ordinal — reordering/inserting an enum value
 * later must not silently reinterpret already-stored rows as the wrong type. */
class Converters {
    @ColumnTypeConverter
    fun fromGrowZoneType(type: GrowZoneType): String = type.name

    @ColumnTypeConverter
    fun toGrowZoneType(value: String): GrowZoneType = GrowZoneType.valueOf(value)
}
