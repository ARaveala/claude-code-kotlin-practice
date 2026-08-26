package com.practice.plant_user.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.practice.plant_user.data.GrowZoneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun zone(
    id: Long,
    parentGrowZoneId: Long?,
    xCm: Double,
    yCm: Double,
    widthCm: Double = 100.0,
    depthCm: Double = 100.0,
) = GrowZone(
    id = id,
    parentGrowZoneId = parentGrowZoneId,
    type = GrowZoneType.PLOT,
    name = "z$id",
    widthCm = widthCm,
    depthCm = depthCm,
    heightCm = null,
    xCm = xCm,
    yCm = yCm,
)

class AbsolutePositionCmTest {

    @Test
    fun `top-level zone's position is already absolute`() {
        val topLevel = zone(id = 1, parentGrowZoneId = null, xCm = 10.0, yCm = 20.0)

        val result = absolutePositionCm(topLevel, listOf(topLevel))

        assertEquals(Offset(10f, 20f), result)
    }

    @Test
    fun `nested zone's position adds its parent's offset`() {
        val parent = zone(id = 1, parentGrowZoneId = null, xCm = 10.0, yCm = 20.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 5.0, yCm = 5.0)

        val result = absolutePositionCm(child, listOf(parent, child))

        assertEquals(Offset(15f, 25f), result)
    }

    @Test
    fun `two levels of nesting sums both ancestors' offsets`() {
        // Greenhouse-in-greenhouse is the domain model's max nesting depth (2).
        val grandparent = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0)
        val parent = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 10.0)
        val child = zone(id = 3, parentGrowZoneId = 2, xCm = 5.0, yCm = 5.0)

        val result = absolutePositionCm(child, listOf(grandparent, parent, child))

        assertEquals(Offset(15f, 15f), result)
    }
}

class HitTestZoneTest {

    @Test
    fun `point inside a zone's bounds returns that zone`() {
        val a = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result = hitTestZone(listOf(a), Offset(50f, 50f))

        assertEquals(a, result)
    }

    @Test
    fun `point outside every zone's bounds returns null`() {
        val a = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result = hitTestZone(listOf(a), Offset(150f, 50f))

        assertNull(result)
    }

    @Test
    fun `point inside both a parent and its nested child selects the child`() {
        val parent = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 10.0, widthCm = 20.0, depthCm = 20.0)

        // (15, 15) is inside the parent's 0..100 bounds AND the child's absolute 10..30 bounds.
        val result = hitTestZone(listOf(parent, child), Offset(15f, 15f))

        assertEquals(child, result)
    }
}

class FitTransformTest {

    @Test
    fun `square zone in a square viewport is centered with scale matching the size ratio`() {
        val zone = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result = fitTransform(
            zone = zone,
            allZones = listOf(zone),
            viewportSizePx = Size(200f, 200f),
            cmToPx = 1f,
            minScale = 0.2f,
            maxScale = 5f,
        )

        assertEquals(2f, result.scale, 0f)
        assertEquals(Offset(0f, 0f), result.translation)
    }

    @Test
    fun `mismatched aspect ratio uses the more constraining dimension, not either alone`() {
        // A square zone in a 2:1 viewport must still fit fully on screen (the "contain" rule),
        // so scale has to come from the height ratio (2), not the wider width ratio (4).
        val zone = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result = fitTransform(
            zone = zone,
            allZones = listOf(zone),
            viewportSizePx = Size(400f, 200f),
            cmToPx = 1f,
            minScale = 0.2f,
            maxScale = 5f,
        )

        assertEquals(2f, result.scale, 0f)
    }
}
