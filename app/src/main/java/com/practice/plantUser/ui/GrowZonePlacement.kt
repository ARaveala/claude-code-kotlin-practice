package com.practice.plantUser.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.PositionCm
import kotlin.math.min

/**
 * [zone]'s absolute position in cm, resolved by walking up its parent chain. A nested zone's own
 * xCm/yCm are relative to its parent's local origin, not the Area's — top-level zones (no parent)
 * are already absolute, so the loop is a no-op for them.
 */
fun absolutePositionCm(zone: GrowZone, allZones: List<GrowZone>): PositionCm {
    var xCm = zone.xCm
    var yCm = zone.yCm
    var parentId = zone.parentGrowZoneId
    while (parentId != null) {
        val parent = allZones.firstOrNull { it.id == parentId } ?: break
        xCm += parent.xCm
        yCm += parent.yCm
        parentId = parent.parentGrowZoneId
    }
    return PositionCm(xCm, yCm)
}

/**
 * The smallest (innermost) zone whose absolute bounds contain [pointCm], or null if none. Smallest
 * wins so tapping inside a nested child selects the child, not the parent zone it sits inside.
 * [focusedZoneId] is the zone the canvas is zoomed into (null at Area level); it never responds.
 * Inside the focused zone only its descendants respond, so empty space there is null even though
 * its ancestors (or an overlapping unrelated zone) also contain the point. Equal areas: first wins.
 */
fun hitTestZone(zones: List<GrowZone>, pointCm: Offset, focusedZoneId: Long?): GrowZone? {
    val px = pointCm.x.toDouble()
    val py = pointCm.y.toDouble()
    val focused = zones.firstOrNull { it.id == focusedZoneId }
    val insideFocused = focused != null && containsPointCm(focused, zones, px, py)

    // One pass, no filtered list. Still allocates one PositionCm per zone checked (absolutePositionCm),
    // fine per tap, not something to call per frame.
    var best: GrowZone? = null
    var bestAreaCm2 = Double.POSITIVE_INFINITY
    for (i in zones.indices) {
        val zone = zones[i]
        if (zone.id == focusedZoneId) continue
        if (insideFocused && !isDescendantOf(zone, focusedZoneId, zones)) continue
        if (!containsPointCm(zone, zones, px, py)) continue
        val areaCm2 = zone.widthCm * zone.depthCm
        if (areaCm2 < bestAreaCm2) {
            best = zone
            bestAreaCm2 = areaCm2
        }
    }
    return best
}

private fun containsPointCm(zone: GrowZone, allZones: List<GrowZone>, px: Double, py: Double): Boolean {
    val pos = absolutePositionCm(zone, allZones)
    return px in pos.xCm..(pos.xCm + zone.widthCm) && py in pos.yCm..(pos.yCm + zone.depthCm)
}

/** Whether [ancestorId] appears anywhere up [zone]'s parent chain (a zone is not its own descendant). */
private fun isDescendantOf(zone: GrowZone, ancestorId: Long?, allZones: List<GrowZone>): Boolean {
    var parentId = zone.parentGrowZoneId
    while (parentId != null) {
        if (parentId == ancestorId) return true
        parentId = allZones.firstOrNull { it.id == parentId }?.parentGrowZoneId
    }
    return false
}

/** [zoneId]'s nesting chain, root first and ending with the zone itself; empty if not found. */
fun zoneChain(zones: List<GrowZone>, zoneId: Long?): List<GrowZone> {
    val chain = ArrayList<GrowZone>()
    var current = zones.firstOrNull { it.id == zoneId }
    while (current != null) {
        chain.add(current)
        val parentId = current.parentGrowZoneId ?: break
        current = zones.firstOrNull { it.id == parentId }
    }
    chain.reverse()
    return chain
}

/**
 * Transform that fits [zone] fully inside [viewportSizePx], centered — the "contain" behaviour
 * domain_model.md calls for (zone's edges fully visible on screen), not "fill" (which would crop
 * whichever dimension doesn't match the viewport's aspect ratio, e.g. a very long thin plot).
 */
fun fitTransform(
    zone: GrowZone,
    allZones: List<GrowZone>,
    viewportSizePx: Size,
    cmToPx: Float,
    minScale: Float,
    maxScale: Float
): CanvasTransform {
    val posCm = absolutePositionCm(zone, allZones)
    val zoneWidthPx = zone.widthCm.toFloat() * cmToPx
    val zoneHeightPx = zone.depthCm.toFloat() * cmToPx
    val scale =
        min(viewportSizePx.width / zoneWidthPx, viewportSizePx.height / zoneHeightPx)
            .coerceIn(minScale, maxScale)
    val zoneCenterPx =
        Offset(
            (posCm.xCm.toFloat() + zone.widthCm.toFloat() / 2f) * cmToPx,
            (posCm.yCm.toFloat() + zone.depthCm.toFloat() / 2f) * cmToPx
        )
    val viewportCenterPx = Offset(viewportSizePx.width / 2f, viewportSizePx.height / 2f)
    val translation = viewportCenterPx - zoneCenterPx * scale
    return CanvasTransform(scale = scale, translation = translation)
}
