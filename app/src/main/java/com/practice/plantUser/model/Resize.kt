package com.practice.plantUser.model

/** A rect corner as a fraction of the size from the top-left: the corner of (x, y, w, d) is
 * (x + w * fx, y + d * fy). y grows downward, as on screen. The same formula works in cm (Double)
 * and, after toFloat(), in px. The closed set of four corners stays here and in the handle layout
 * only; nothing else should assume four corners (phase_3.md 3b, custom shapes later). */
enum class Anchor(val fx: Double, val fy: Double) {
    TOP_LEFT(0.0, 0.0),
    TOP_RIGHT(1.0, 0.0),
    BOTTOM_LEFT(0.0, 1.0),
    BOTTOM_RIGHT(1.0, 1.0);

    /** The diagonally opposite corner: grabbing a handle makes this the anchor. */
    val opposite: Anchor
        get() = when (this) {
            TOP_LEFT -> BOTTOM_RIGHT
            TOP_RIGHT -> BOTTOM_LEFT
            BOTTOM_LEFT -> TOP_RIGHT
            BOTTOM_RIGHT -> TOP_LEFT
        }
}

/** [zone] resized to [newWidthCm] x [newDepthCm] with its [anchor] corner unchanged. Typed entry,
 * drag resize and save all go through here, so the preview equals what gets stored.
 *
 * Works in the zone's own frame (parent-relative for nested zones). Does not validate: callers
 * check the sizes with [widthDepthRejectionReason] first. The new position may be negative.
 * TOP_LEFT keeps x/y bit-identical (x + w * 0.0 - newW * 0.0 == x); other anchors can round in
 * the last bit, so compare their anchor corner with a tolerance. */
fun resize(zone: GrowZone, newWidthCm: Double, newDepthCm: Double, anchor: Anchor): GrowZone {
    val anchorXCm = zone.xCm + zone.widthCm * anchor.fx
    val anchorYCm = zone.yCm + zone.depthCm * anchor.fy
    return zone.copy(
        widthCm = newWidthCm,
        depthCm = newDepthCm,
        xCm = anchorXCm - newWidthCm * anchor.fx,
        yCm = anchorYCm - newDepthCm * anchor.fy
    )
}

/** [zone] resized so the corner opposite [anchor] follows the finger at ([fingerXCm], [fingerYCm]).
 * [zone] is the zone as it was at drag start, so the anchor point stays fixed for the whole drag.
 * Goes through [resize], so a drag and typed entry of the same size give the same result.
 * (1 - 2 * fx) is +1 for a left anchor and -1 for a right one, so the distance is positive on the
 * handle's side of the anchor. Runs every drag frame: two Double params, no PositionCm allocation. */
fun dragResize(zone: GrowZone, anchor: Anchor, fingerXCm: Double, fingerYCm: Double): GrowZone {
    val anchorXCm = zone.xCm + zone.widthCm * anchor.fx
    val anchorYCm = zone.yCm + zone.depthCm * anchor.fy
    // Clamped, not rejected: past the anchor the zone stops at the minimum instead of flipping.
    val newWidthCm = ((fingerXCm - anchorXCm) * (1 - 2 * anchor.fx))
        .coerceIn(GrowZoneLimits.MIN_DIMENSION_CM, GrowZoneLimits.MAX_WIDTH_DEPTH_CM)
    val newDepthCm = ((fingerYCm - anchorYCm) * (1 - 2 * anchor.fy))
        .coerceIn(GrowZoneLimits.MIN_DIMENSION_CM, GrowZoneLimits.MAX_WIDTH_DEPTH_CM)
    return resize(zone, newWidthCm, newDepthCm, anchor)
}
