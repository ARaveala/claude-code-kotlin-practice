package com.practice.plantUser.data

import androidx.room3.ColumnTypeConverter
import com.practice.plantUser.model.GrowZoneType

/** Stores [GrowZoneType] by name, not ordinal — reordering/inserting an enum value
 * later must not silently reinterpret already-stored rows as the wrong type. */
class Converters {
    @ColumnTypeConverter
    fun fromGrowZoneType(type: GrowZoneType): String = type.name

    @ColumnTypeConverter
    fun toGrowZoneType(value: String): GrowZoneType = GrowZoneType.valueOf(value)
}
