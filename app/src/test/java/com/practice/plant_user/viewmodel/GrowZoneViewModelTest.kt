package com.practice.plant_user.viewmodel

import com.practice.plant_user.data.GrowZoneType
import com.practice.plant_user.ui.GrowZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun zone(
    xCm: Double,
    widthCm: Double,
    parentGrowZoneId: Long? = null,
    type: GrowZoneType = GrowZoneType.PLOT,
    depthCm: Double = 100.0,
    id: Long = 0,
) = GrowZone(
    id = id,
    parentGrowZoneId = parentGrowZoneId,
    type = type,
    name = "z",
    widthCm = widthCm,
    depthCm = depthCm,
    heightCm = null,
    xCm = xCm,
    yCm = 0.0,
)

class NextTopLevelXCmTest {

    @Test
    fun `first zone in an empty Area starts at zero`() {
        assertEquals(0.0, nextTopLevelXCm(existingZones = emptyList()), 0.0)
    }

    @Test
    fun `next zone is placed past the rightmost edge plus the gap`() {
        val existing = listOf(zone(xCm = 0.0, widthCm = 100.0))

        val result = nextTopLevelXCm(existingZones = existing, gapCm = 50.0)

        assertEquals(150.0, result, 0.0)
    }

    @Test
    fun `uses the actual rightmost edge, not just the last zone in the list`() {
        // Guards against a naive "look at the last element" implementation if zones are ever
        // returned out of creation order.
        val existing = listOf(
            zone(xCm = 500.0, widthCm = 50.0),
            zone(xCm = 0.0, widthCm = 100.0),
        )

        val result = nextTopLevelXCm(existingZones = existing, gapCm = 50.0)

        assertEquals(600.0, result, 0.0)
    }
}

class NextNestedOffsetCmTest {

    @Test
    fun `first child in an empty parent is inset from the corner, not flush at zero`() {
        val result = nextNestedOffsetCm(existingSiblings = emptyList(), paddingCm = 20.0)

        assertEquals(20f, result.x, 0f)
        assertEquals(20f, result.y, 0f)
    }

    @Test
    fun `next sibling stacks past the rightmost edge plus the padding, same y`() {
        val existing = listOf(zone(xCm = 20.0, widthCm = 100.0, parentGrowZoneId = 1L))

        val result = nextNestedOffsetCm(existingSiblings = existing, paddingCm = 20.0)

        assertEquals(140f, result.x, 0f)
        assertEquals(20f, result.y, 0f)
    }
}

class CanNestGrowZoneTest {

    // No type-pairing restrictions anymore (domain_model.md's Nesting Rules explains why) — the
    // only remaining rule is a flat depth cap, so these tests are purely about chain depth.
    // Any GrowZone type is fine at any of these levels; type isn't even a parameter anymore.

    private val topLevel = zone(xCm = 0.0, widthCm = 100.0, id = 1, parentGrowZoneId = null) // depth 1
    private val nestedOnce = zone(xCm = 0.0, widthCm = 100.0, id = 2, parentGrowZoneId = 1L) // depth 2
    private val nestedTwice = zone(xCm = 0.0, widthCm = 100.0, id = 3, parentGrowZoneId = 2L) // depth 3
    private val allZones = listOf(topLevel, nestedOnce, nestedTwice)

    @Test
    fun `top-level (no parent) is always valid`() {
        assertTrue(canNestGrowZone(parent = null, allZones = emptyList()))
    }

    @Test
    fun `nesting under a top-level zone reaches depth 2, which is allowed`() {
        assertTrue(canNestGrowZone(parent = topLevel, allZones = allZones))
    }

    @Test
    fun `nesting under a depth-2 zone reaches depth 3, exactly at the cap, still allowed`() {
        // Boundary check: exactly at MAX_NESTING_DEPTH must still succeed, not just below it.
        assertTrue(canNestGrowZone(parent = nestedOnce, allZones = allZones))
    }

    @Test
    fun `nesting under a depth-3 zone would reach depth 4, past the cap, rejected`() {
        assertFalse(canNestGrowZone(parent = nestedTwice, allZones = allZones))
    }

    @Test
    fun `rejection reason is null exactly when nesting is valid`() {
        assertNull(nestingRejectionReason(topLevel, allZones))
        assertNotNull(nestingRejectionReason(nestedTwice, allZones))
    }
}
