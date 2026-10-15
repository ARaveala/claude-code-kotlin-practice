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
 */
fun hitTestZone(zones: List<GrowZone>, pointCm: Offset): GrowZone? {
    val px = pointCm.x.toDouble()
    val py = pointCm.y.toDouble()
    return zones
        .filter { zone ->
            val pos = absolutePositionCm(zone, zones)
            px in pos.xCm..(pos.xCm + zone.widthCm) &&
                py in pos.yCm..(pos.yCm + zone.depthCm)
        }.minByOrNull { it.widthCm * it.depthCm }
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
