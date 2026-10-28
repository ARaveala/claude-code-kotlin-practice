package com.practice.plantUser.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.GrowZoneType
import com.practice.plantUser.model.PositionCm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun zone(
    id: Long,
    parentGrowZoneId: Long?,
    xCm: Double,
    yCm: Double,
    widthCm: Double = 100.0,
    depthCm: Double = 100.0
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

// This test seems obsolete , or could use stricter values?
class AbsolutePositionCmTest {
    @Test
    fun `top-level zone's position is already absolute`() {
        val topLevel = zone(id = 1, parentGrowZoneId = null, xCm = 10.0, yCm = 20.0)

        val result = absolutePositionCm(topLevel, listOf(topLevel))

        assertEquals(PositionCm(10.0, 20.0), result)
    }

    @Test
    fun absolutePositionCm_keepsDoublePrecision_acrossNestingLevels() {
        val root = GrowZone(
            id = 1, parentGrowZoneId = null, type = GrowZoneType.PLOT, name = "root",
            widthCm = 100_000.0, depthCm = 100.0, heightCm = null, xCm = 90_000.3, yCm = 0.0
        )
        val child = GrowZone(
            id = 2, parentGrowZoneId = 1, type = GrowZoneType.BOX, name = "child",
            widthCm = 500.0, depthCm = 100.0, heightCm = null, xCm = 353.3, yCm = 0.0
        )
        val grandchild = GrowZone(
            id = 3, parentGrowZoneId = 2, type = GrowZoneType.BOX, name = "grandchild",
            widthCm = 50.0, depthCm = 50.0, heightCm = null, xCm = 0.1, yCm = 0.0
        )
        val position = absolutePositionCm(grandchild, listOf(root, child, grandchild))
        val expected = 0.1 + 353.3 + 90_000.3 // summed in Double, the way the model should
        assertEquals("ERROR", expected, position.xCm, 1e-9)
    }

    @Test
    fun `nested zone's position adds its parent's offset`() {
        val parent = zone(id = 1, parentGrowZoneId = null, xCm = 10.0, yCm = 20.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 5.0, yCm = 5.0)

        val result = absolutePositionCm(child, listOf(parent, child))

        assertEquals(PositionCm(15.0, 25.0), result)
    }

    // could this test benefit from stricter values?
    @Test
    fun `two levels of nesting sums both ancestors' offsets`() {
        // Greenhouse-in-greenhouse is the domain model's max nesting depth (2).
        val grandparent = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0)
        val parent = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 10.0)
        val child = zone(id = 3, parentGrowZoneId = 2, xCm = 5.0, yCm = 5.0)

        val result = absolutePositionCm(child, listOf(grandparent, parent, child))

        assertEquals(PositionCm(15.0, 15.0), result)
    }
}

class ParentOriginCmTest {
    @Test
    fun `nested zone's frame starts at its parent's absolute position`() {
        // Drag gives the finger in absolute canvas cm, but a nested zone's x/y are parent-relative.
        // Naive version (no conversion) treats every zone as top-level: a child's dragged corner
        // lands off by its parent's offset. Two levels so a one-level-only walk fails too.
        val grandparent = zone(id = 1, parentGrowZoneId = null, xCm = 100.0, yCm = 200.0)
        val parent = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 20.0)
        val child = zone(id = 3, parentGrowZoneId = 2, xCm = 5.0, yCm = 5.0)

        val result = parentOriginCm(child, listOf(grandparent, parent, child))

        assertEquals(
            "parentOriginCm(child of (10, 20) in (100, 200)) in ui/GrowZonePlacement.kt",
            PositionCm(110.0, 220.0),
            result
        )
    }
}

class HitTestZoneTest {
    @Test
    fun `point inside a zone's bounds returns that zone`() {
        val a = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result = hitTestZone(listOf(a), Offset(50f, 50f), focusedZoneId = null)

        assertEquals(a, result)
    }

    @Test
    fun `point outside every zone's bounds returns null`() {
        val a = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val result = hitTestZone(listOf(a), Offset(150f, 50f), focusedZoneId = null)

        assertNull(result)
    }

    @Test
    fun `point inside both a parent and its nested child selects the child`() {
        val parent = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 10.0, widthCm = 20.0, depthCm = 20.0)
        // (15, 15) is inside the parent's 0..100 bounds AND the child's absolute 10..30 bounds.
        val result = hitTestZone(listOf(parent, child), Offset(15f, 15f), focusedZoneId = null)

        assertEquals(child, result)
    }
}

// Phase 3a rules: the focused zone never responds; inside it, only its descendants and empty
// space do (empty space = null, which clears the selection). Outside it, any zone can be hit.
//
// Side effect of test-first stubs: while hitTestZone ignores focusedZoneId, the tests marked
// "passes on old behaviour" are green because the pre-3a smallest-wins logic already covers them.
// Only the "focused/ancestor/non-descendant" tests prove the new rule.
private fun assertHit(expected: GrowZone?, zones: List<GrowZone>, pointCm: Offset, focusedZoneId: Long?) = assertEquals(
    "hitTestZone(point=$pointCm, focusedZoneId=$focusedZoneId) in ui/GrowZonePlacement.kt",
    expected,
    hitTestZone(zones, pointCm, focusedZoneId)
)

class HitTestZoneFocusTest {
    @Test
    fun `empty space inside the focused zone hits nothing`() {
        val focused = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        assertHit(null, listOf(focused), Offset(50f, 50f), focusedZoneId = 1)
    }

    @Test
    fun `empty space inside the focused zone does not select its ancestor`() {
        // The greenhouse contains the tap point too, but the tap is inside the focused bed.
        val greenhouse = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 1000.0, depthCm = 1000.0)
        val bed = zone(id = 2, parentGrowZoneId = 1, xCm = 100.0, yCm = 100.0, widthCm = 100.0, depthCm = 100.0)

        assertHit(null, listOf(greenhouse, bed), Offset(150f, 150f), focusedZoneId = 2)
    }

    @Test
    fun `overlapping non-descendant inside the focused zone is ignored`() {
        // Freeform overlap (bounds_enforced off, Phase 3d): q is top level, not bed's child.
        val bed = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val q = zone(id = 2, parentGrowZoneId = null, xCm = 50.0, yCm = 50.0, widthCm = 20.0, depthCm = 20.0)

        assertHit(null, listOf(bed, q), Offset(55f, 55f), focusedZoneId = 1)
    }

    @Test
    fun `child of the focused zone is hit (passes on old behaviour)`() {
        val focused = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 10.0, widthCm = 20.0, depthCm = 20.0)

        assertHit(child, listOf(focused, child), Offset(15f, 15f), focusedZoneId = 1)
    }

    @Test
    fun `grandchild inside the focused zone wins over its parent (passes on old behaviour)`() {
        val focused = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 10.0, yCm = 10.0, widthCm = 50.0, depthCm = 50.0)
        val grandchild = zone(id = 3, parentGrowZoneId = 2, xCm = 5.0, yCm = 5.0, widthCm = 10.0, depthCm = 10.0)

        // grandchild absolute: 15..25
        assertHit(grandchild, listOf(focused, child, grandchild), Offset(20f, 20f), focusedZoneId = 1)
    }

    @Test
    fun `sibling outside the focused zone can be selected (passes on old behaviour)`() {
        val greenhouse = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 1000.0, depthCm = 1000.0)
        val bed = zone(id = 2, parentGrowZoneId = 1, xCm = 100.0, yCm = 100.0, widthCm = 100.0, depthCm = 100.0)
        val sibling = zone(id = 3, parentGrowZoneId = 1, xCm = 300.0, yCm = 100.0, widthCm = 100.0, depthCm = 100.0)

        // sibling absolute: 300..400, 100..200
        assertHit(sibling, listOf(greenhouse, bed, sibling), Offset(350f, 150f), focusedZoneId = 2)
    }

    @Test
    fun `smallest of two overlapping unrelated zones wins (passes on old behaviour)`() {
        val big = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)
        val small = zone(id = 2, parentGrowZoneId = null, xCm = 25.0, yCm = 25.0, widthCm = 50.0, depthCm = 50.0)

        assertHit(small, listOf(big, small), Offset(30f, 30f), focusedZoneId = null)
    }
}

class ZoneChainTest {
    private fun assertChain(expectedIds: List<Long>, zones: List<GrowZone>, zoneId: Long?) = assertEquals(
        "zoneChain(zoneId=$zoneId) in ui/GrowZonePlacement.kt",
        expectedIds,
        zoneChain(zones, zoneId).map { it.id }
    )

    @Test
    fun `chain is root first and ends with the zone itself`() {
        val root = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0)
        val child = zone(id = 2, parentGrowZoneId = 1, xCm = 0.0, yCm = 0.0)
        val grandchild = zone(id = 3, parentGrowZoneId = 2, xCm = 0.0, yCm = 0.0)

        assertChain(listOf(1L, 2L, 3L), listOf(root, child, grandchild), zoneId = 3)
    }

    @Test
    fun `no focus gives an empty chain`() {
        val root = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0)

        assertChain(emptyList(), listOf(root), zoneId = null)
    }
}

class FitTransformTest {
    @Test
    fun `square zone in a square viewport is centered with scale matching the size ratio`() {
        val zone = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result =
            fitTransform(
                zone = zone,
                allZones = listOf(zone),
                viewportSizePx = Size(200f, 200f),
                cmToPx = 1f,
                minScale = 0.2f,
                maxScale = 5f
            )

        assertEquals(2f, result.scale, 0f)
        assertEquals(Offset(0f, 0f), result.translation)
    }

    @Test
    fun `mismatched aspect ratio uses the more constraining dimension, not either alone`() {
        // A square zone in a 2:1 viewport must still fit fully on screen (the "contain" rule),
        // so scale has to come from the height ratio (2), not the wider width ratio (4).
        val zone = zone(id = 1, parentGrowZoneId = null, xCm = 0.0, yCm = 0.0, widthCm = 100.0, depthCm = 100.0)

        val result =
            fitTransform(
                zone = zone,
                allZones = listOf(zone),
                viewportSizePx = Size(400f, 200f),
                cmToPx = 1f,
                minScale = 0.2f,
                maxScale = 5f
            )

        assertEquals(2f, result.scale, 0f)
    }
}
