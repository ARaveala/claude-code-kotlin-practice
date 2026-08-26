package com.practice.plant_user.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

/**
 * [zone]'s absolute position in cm, resolved by walking up its parent chain. A nested zone's own
 * xCm/yCm are relative to its parent's local origin, not the Area's — top-level zones (no parent)
 * are already absolute, so the loop is a no-op for them.
 */
fun absolutePositionCm(zone: GrowZone, allZones: List<GrowZone>): Offset {
    var xCm = zone.xCm.toFloat()
    var yCm = zone.yCm.toFloat()
    var parentId = zone.parentGrowZoneId
    while (parentId != null) {
        val parent = allZones.firstOrNull { it.id == parentId } ?: break
        xCm += parent.xCm.toFloat()
        yCm += parent.yCm.toFloat()
        parentId = parent.parentGrowZoneId
    }
    return Offset(xCm, yCm)
}

/**
 * The smallest (innermost) zone whose absolute bounds contain [pointCm], or null if none. Smallest
 * wins so tapping inside a nested child selects the child, not the parent zone it sits inside.
 */
fun hitTestZone(zones: List<GrowZone>, pointCm: Offset): GrowZone? =
    zones
        .filter { zone ->
            val pos = absolutePositionCm(zone, zones)
            pointCm.x in pos.x..(pos.x + zone.widthCm.toFloat()) &&
                pointCm.y in pos.y..(pos.y + zone.depthCm.toFloat())
        }
        .minByOrNull { it.widthCm * it.depthCm }

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
    maxScale: Float,
): CanvasTransform {
    val posCm = absolutePositionCm(zone, allZones)
    val zoneWidthPx = zone.widthCm.toFloat() * cmToPx
    val zoneHeightPx = zone.depthCm.toFloat() * cmToPx
    val scale = min(viewportSizePx.width / zoneWidthPx, viewportSizePx.height / zoneHeightPx)
        .coerceIn(minScale, maxScale)
    val zoneCenterPx = Offset(
        (posCm.x + zone.widthCm.toFloat() / 2f) * cmToPx,
        (posCm.y + zone.depthCm.toFloat() / 2f) * cmToPx,
    )
    val viewportCenterPx = Offset(viewportSizePx.width / 2f, viewportSizePx.height / 2f)
    val translation = viewportCenterPx - zoneCenterPx * scale
    return CanvasTransform(scale = scale, translation = translation)
}
