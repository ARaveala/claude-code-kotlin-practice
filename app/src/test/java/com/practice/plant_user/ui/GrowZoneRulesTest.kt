package com.practice.plant_user.ui

import com.practice.plant_user.model.GrowZone
import com.practice.plant_user.model.GrowZoneType
import com.practice.plant_user.model.nextNestedPositionCm
import junit.framework.TestCase.assertEquals
import org.junit.Test

class GrowZoneRulesTests {
    @Test
    fun nextNestedPosition_keepsDoublePrecision() {
        val sibling = GrowZone(
            id = 1, parentGrowZoneId = 10, type = GrowZoneType.BOX, name = "sibling",
            widthCm = 233.3, depthCm = 50.0, heightCm = null, xCm = 100.0, yCm = 20.0,
        )
        // rightmost edge 333.3 + 20.0 padding = 353.3
        val position = nextNestedPositionCm(listOf(sibling))
        assertEquals(353.3, position.xCm, 1e-9)
    }
}