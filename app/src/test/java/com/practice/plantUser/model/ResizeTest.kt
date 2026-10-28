package com.practice.plantUser.model

import org.junit.Assert.assertEquals
import org.junit.Test

// Corners are written out as plain sums per test, not via Anchor.fx/fy: a test using the same
// formula as resize() would agree with a wrong fraction in the enum.

private const val TOLERANCE_CM = 1e-9

private fun zone(
    id: Long,
    parentGrowZoneId: Long?,
    xCm: Double,
    yCm: Double,
    widthCm: Double = 100.0,
    depthCm: Double = 50.0
) = GrowZone(
    id = id,
    parentGrowZoneId = parentGrowZoneId,
    type = GrowZoneType.PLOT,
    name = "z$id",
    widthCm = widthCm,
    depthCm = depthCm,
    heightCm = null,
    xCm = xCm,
    yCm = yCm
)

// Message reads e.g. "resize(BOTTOM_RIGHT, 100x50 -> 150x80) cornerY in model/Resize.kt".
private fun assertCm(call: String, what: String, expected: Double, actual: Double) =
    assertEquals("resize($call) $what in model/Resize.kt", expected, actual, TOLERANCE_CM)

class ResizeTest {
    // x=10, y=20, 100 x 50: corners at x 10/110, y 20/70.
    private val before = zone(id = 1, parentGrowZoneId = null, xCm = 10.0, yCm = 20.0)

    @Test
    fun `top left anchor keeps x and y (3a behaviour)`() {
        val call = "TOP_LEFT, 100x50 -> 150x80"
        val after = resize(before, 150.0, 80.0, Anchor.TOP_LEFT)

        assertCm(call, "xCm", before.xCm, after.xCm)
        assertCm(call, "yCm", before.yCm, after.yCm)
        assertCm(call, "widthCm", 150.0, after.widthCm)
        assertCm(call, "depthCm", 80.0, after.depthCm)
    }

    @Test
    fun `bottom right anchor keeps the bottom right corner`() {
        val call = "BOTTOM_RIGHT, 100x50 -> 150x80"
        val after = resize(before, 150.0, 80.0, Anchor.BOTTOM_RIGHT)

        assertCm(call, "cornerX", before.xCm + before.widthCm, after.xCm + after.widthCm)
        assertCm(call, "cornerY", before.yCm + before.depthCm, after.yCm + after.depthCm)
        // Grows left and up, past the origin: positions may be negative (phase_3.md 3b).
        assertCm(call, "xCm", -40.0, after.xCm)
        assertCm(call, "yCm", -10.0, after.yCm)
        assertCm(call, "widthCm", 150.0, after.widthCm)
        assertCm(call, "depthCm", 80.0, after.depthCm)
    }

    @Test
    fun `top right anchor keeps the top right corner`() {
        val call = "TOP_RIGHT, 100x50 -> 150x80"
        val after = resize(before, 150.0, 80.0, Anchor.TOP_RIGHT)

        assertCm(call, "cornerX", before.xCm + before.widthCm, after.xCm + after.widthCm)
        // Top edge is y itself, no depth added.
        assertCm(call, "cornerY", before.yCm, after.yCm)
        assertCm(call, "widthCm", 150.0, after.widthCm)
        assertCm(call, "depthCm", 80.0, after.depthCm)
    }

    @Test
    fun `bottom left anchor keeps the bottom left corner`() {
        val call = "BOTTOM_LEFT, 100x50 -> 150x80"
        val after = resize(before, 150.0, 80.0, Anchor.BOTTOM_LEFT)

        // Left edge is x itself, no width added.
        assertCm(call, "cornerX", before.xCm, after.xCm)
        assertCm(call, "cornerY", before.yCm + before.depthCm, after.yCm + after.depthCm)
        assertCm(call, "widthCm", 150.0, after.widthCm)
        assertCm(call, "depthCm", 80.0, after.depthCm)
    }

    @Test
    fun `shrinking with bottom right anchor moves x and y towards the anchor`() {
        val call = "BOTTOM_RIGHT, 100x50 -> 60x30"
        val after = resize(before, 60.0, 30.0, Anchor.BOTTOM_RIGHT)

        assertCm(call, "cornerX", before.xCm + before.widthCm, after.xCm + after.widthCm)
        assertCm(call, "cornerY", before.yCm + before.depthCm, after.yCm + after.depthCm)
        // 110 - 60 = 50, 70 - 30 = 40.
        assertCm(call, "xCm", 50.0, after.xCm)
        assertCm(call, "yCm", 40.0, after.yCm)
    }
}

class DragResizeTest {
    // x=10, y=20, 100 x 50: TOP_LEFT anchor point is (10, 20).
    private val before = zone(id = 1, parentGrowZoneId = null, xCm = 10.0, yCm = 20.0)

    @Test
    fun `finger past the anchor clamps to the minimum instead of flipping`() {
        // Finger 5 cm left of a left anchor: unclamped, width is -5 and the zone flips over.
        val call = "dragResize TOP_LEFT, finger (5, 100)"
        val after = dragResize(before, Anchor.TOP_LEFT, fingerXCm = 5.0, fingerYCm = 100.0)

        assertCm(call, "widthCm", GrowZoneLimits.MIN_DIMENSION_CM, after.widthCm)
        assertCm(call, "xCm", before.xCm, after.xCm)
    }
}
