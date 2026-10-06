package com.practice.plantUser.model

// import androidx.compose.ui.geometry.Offset

private const val GROW_ZONE_GAP_CM = 50.0
private const val NESTED_ZONE_PADDING_CM = 20.0

// Flat cap, not type-specific — see domain_model.md's Nesting Rules for why type-pairing
// restrictions were dropped. A top-level zone is depth 1, so this allows 2 levels of nesting.
private const val MAX_NESTING_DEPTH = 3

/** Placeholder auto-layout (Phase 2): next top-level zone goes to the right of the current
 * rightmost zone's edge, plus a fixed gap. No drag-to-place UI exists yet (Phase 5). */
fun nextTopLevelXCm(
    existingZones: List<GrowZone>,
    gapCm: Double = GROW_ZONE_GAP_CM,
): Double {
    if (existingZones.isEmpty()) return 0.0
    val rightmostEdge = existingZones.maxOf { it.xCm + it.widthCm }
    return rightmostEdge + gapCm
}

/** Same stacking idea as [nextTopLevelXCm], scoped to siblings under the same parent zone instead
 * of an Area's top level, and inset from the parent's corner instead of starting flush at zero. */
fun nextNestedPositionCm(
    existingSiblings: List<GrowZone>,
    paddingCm: Double = NESTED_ZONE_PADDING_CM,
): PositionCm {
    if (existingSiblings.isEmpty()) return PositionCm(paddingCm, paddingCm)
    val rightmostEdge = existingSiblings.maxOf { it.xCm + it.widthCm }
    return PositionCm(rightmostEdge + paddingCm, paddingCm)
}

/** [zone]'s depth in its nesting chain, counting itself — a top-level zone (no parent) is 1. */
private fun nestingDepth(
    zone: GrowZone,
    allZones: List<GrowZone>,
): Int {
    var depth = 1
    var parentId = zone.parentGrowZoneId
    while (parentId != null) {
        val parent = allZones.firstOrNull { it.id == parentId } ?: break
        depth++
        parentId = parent.parentGrowZoneId
    }
    return depth
}

/**
 * Why a new zone can't nest inside [parent], or null if it's fine. No type-pairing restrictions
 * (domain_model.md's Nesting Rules explains why — they fought real use cases like a Box overlapping
 * a Plot, and would need to scale to user-defined zone types later). Only a flat depth cap remains;
 * size/fit is deferred to Phase 3's real `bounds_enforced` containment, not checked here.
 * [parent] null means top-level in the Area, always valid.
 */
fun nestingRejectionReason(
    parent: GrowZone?,
    allZones: List<GrowZone>,
): String? {
    if (parent == null) return null
    if (nestingDepth(parent, allZones) + 1 > MAX_NESTING_DEPTH) {
        return "GrowZones can only nest $MAX_NESTING_DEPTH levels deep"
    }
    return null
}

fun canNestGrowZone(
    parent: GrowZone?,
    allZones: List<GrowZone>,
): Boolean = nestingRejectionReason(parent, allZones) == null
