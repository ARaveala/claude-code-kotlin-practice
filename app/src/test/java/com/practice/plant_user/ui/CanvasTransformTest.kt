package com.practice.plant_user.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN_SCALE = 0.2f
private const val MAX_SCALE = 5f
private const val FLOAT_TOLERANCE = 0.01f

class UpdateTransformTest {

    @Test
    fun `pure pan leaves scale unchanged and translates by pan`() {
        // A one-finger drag reports zoom=1 every frame. Verifies that case moves the canvas by
        // exactly the drag delta and never touches scale — a pinch bug earlier this session made
        // a one-finger pan jump the scale, so this pins the fix.
        val start = CanvasTransform(scale = 1.5f, translation = Offset(10f, 20f))
        val pan = Offset(30f, -15f)

        val result = updateTransform(start, centroid = Offset(500f, 500f), pan = pan, zoom = 1f, minScale = MIN_SCALE, maxScale = MAX_SCALE)

        assertEquals(start.scale, result.scale, FLOAT_TOLERANCE)
        assertEquals(start.translation.x + pan.x, result.translation.x, FLOAT_TOLERANCE)
        assertEquals(start.translation.y + pan.y, result.translation.y, FLOAT_TOLERANCE)
    }

    @Test
    fun `zoom keeps the world point under the centroid fixed on screen`() {
        // The whole point of the centroid math: whatever world point was under your fingers before
        // a pinch should still be under your fingers after, otherwise zoom feels like it fights you.
        val start = CanvasTransform(scale = 1f, translation = Offset(50f, 50f))
        val centroid = Offset(400f, 300f)
        val worldPointBefore = (centroid - start.translation) / start.scale

        val result = updateTransform(start, centroid = centroid, pan = Offset.Zero, zoom = 2.5f, minScale = MIN_SCALE, maxScale = MAX_SCALE)

        val screenPointAfter = worldPointBefore * result.scale + result.translation
        assertEquals(centroid.x, screenPointAfter.x, FLOAT_TOLERANCE)
        assertEquals(centroid.y, screenPointAfter.y, FLOAT_TOLERANCE)
    }

    @Test
    fun `scale clamps at the maximum instead of overshooting`() {
        val start = CanvasTransform(scale = 4f, translation = Offset.Zero)

        val result = updateTransform(start, centroid = Offset(100f, 100f), pan = Offset.Zero, zoom = 10f, minScale = MIN_SCALE, maxScale = MAX_SCALE)

        assertEquals(MAX_SCALE, result.scale, FLOAT_TOLERANCE)
    }

    @Test
    fun `scale clamps at the minimum instead of undershooting`() {
        val start = CanvasTransform(scale = 0.3f, translation = Offset.Zero)

        val result = updateTransform(start, centroid = Offset(100f, 100f), pan = Offset.Zero, zoom = 0.01f, minScale = MIN_SCALE, maxScale = MAX_SCALE)

        assertEquals(MIN_SCALE, result.scale, FLOAT_TOLERANCE)
    }

    @Test
    fun `identity gesture (zoom 1, no pan) leaves the transform unchanged`() {
        // scale=1 keeps the world-point round trip (divide then multiply by scale) exact in
        // floating point, so this is one of the few cases we can assert without a tolerance.
        val start = CanvasTransform(scale = 1f, translation = Offset(123f, 456f))

        val result = updateTransform(start, centroid = Offset(200f, 200f), pan = Offset.Zero, zoom = 1f, minScale = MIN_SCALE, maxScale = MAX_SCALE)

        assertEquals(start, result)
    }

    @Test
    fun `sequential gestures compose correctly across a zoom, pan, then zoom-back sequence`() {
        // Simulates the real usage pattern: the gesture stream calls updateTransform once per
        // frame, feeding each result back in as the next call's input. Uses zoom factors of 2 and
        // 0.5 (powers of two) so every intermediate value is exact and hand-checkable, no tolerance
        // needed. Expected numbers below were derived by hand, not copied from the implementation.
        val centroid = Offset(500f, 500f)
        val start = CanvasTransform(scale = 1f, translation = Offset.Zero)

        val afterZoomIn = updateTransform(start, centroid, pan = Offset.Zero, zoom = 2f, minScale = MIN_SCALE, maxScale = MAX_SCALE)
        val afterPan = updateTransform(afterZoomIn, centroid, pan = Offset(100f, 50f), zoom = 1f, minScale = MIN_SCALE, maxScale = MAX_SCALE)
        val afterZoomOut = updateTransform(afterPan, centroid, pan = Offset.Zero, zoom = 0.5f, minScale = MIN_SCALE, maxScale = MAX_SCALE)

        assertEquals(1f, afterZoomOut.scale, FLOAT_TOLERANCE)
        assertEquals(50f, afterZoomOut.translation.x, FLOAT_TOLERANCE)
        assertEquals(25f, afterZoomOut.translation.y, FLOAT_TOLERANCE)
    }
}

class VisibleGridCellOriginsTest {

    @Test
    fun `at scale 1 with no translation, produces the exact expected set of cell origins`() {
        // Deterministic expected-value check: a 210x180 canvas with 50px cells should produce
        // columns 0,50,100,150,200 and rows 0,50,100,150 (the loop is boundary-inclusive by
        // design, so a cell landing exactly on the edge is intentionally still drawn).
        val transform = CanvasTransform(scale = 1f, translation = Offset.Zero)

        val origins = visibleGridCellOrigins(Size(210f, 180f), transform, gridSpacingPx = 50f)

        val expected = (0..200 step 50).flatMap { x -> (0..150 step 50).map { y -> Offset(x.toFloat(), y.toFloat()) } }
        assertEquals(expected, origins)
    }

    @Test
    fun `panning far away changes which cells are visible but not how many`() {
        // Guards the "render cost is viewport-bound, not pan-distance-bound" property: however far
        // the user has panned, the number of cells drawn per frame should depend only on screen
        // size and zoom, never on accumulated pan distance.
        val nearOrigin = CanvasTransform(scale = 1f, translation = Offset.Zero)
        val farAway = CanvasTransform(scale = 1f, translation = Offset(-1_000_000f, -1_000_000f))
        val canvasSize = Size(210f, 180f)

        val nearCount = visibleGridCellOrigins(canvasSize, nearOrigin, gridSpacingPx = 50f).size
        val farCount = visibleGridCellOrigins(canvasSize, farAway, gridSpacingPx = 50f).size

        assertEquals(nearCount, farCount)
    }

    @Test
    fun `at MAX_SCALE, cell count stays small`() {
        // Guards against the draw loop ballooning if MAX_SCALE is ever raised without noticing —
        // at high zoom each cell is huge on screen, so only a handful should be visible.
        val transform = CanvasTransform(scale = MAX_SCALE, translation = Offset.Zero)

        val origins = visibleGridCellOrigins(Size(1080f, 2100f), transform, gridSpacingPx = 131.25f)

        assertTrue("expected a small cell count at MAX_SCALE, got ${origins.size}", origins.size < 50)
    }

    @Test
    fun `at MIN_SCALE, cell count is bounded but not small — known Phase 5 LOD follow-up`() {
        // This pins the actual measured behavior on a 1080x2100 phone-sized canvas rather than
        // asserting a comfortable-sounding bound: at MIN_SCALE the grid currently draws ~3,700
        // cells/frame. roadmap.md Phase 5 already plans LOD/dot-mode rendering at extreme zoom-out
        // to address this — it's a known follow-up, not something this test is meant to hide.
        val transform = CanvasTransform(scale = MIN_SCALE, translation = Offset.Zero)

        val origins = visibleGridCellOrigins(Size(1080f, 2100f), transform, gridSpacingPx = 131.25f)

        assertTrue("expected MIN_SCALE cell count in the low thousands, got ${origins.size}", origins.size in 3000..4500)
    }
}
