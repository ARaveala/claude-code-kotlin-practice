package com.practice.plantUser.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.practice.plantUser.model.Anchor
import com.practice.plantUser.model.GrowZoneLimits
import com.practice.plantUser.model.formatCm
import kotlin.math.round

// Handle sizes, all in dp; tune on a real phone (phase_3.md 3b, decisions.md 2026-10-20).
// Gap = offset from the corner, about half the hit size, so the corner itself stays visible.
val HANDLE_GAP = 24.dp
val HANDLE_HIT_RADIUS = 24.dp // 48 dp hit size
val HANDLE_RADIUS = 4.dp
val SELECTED_HANDLE_RADIUS = 7.dp
val ANCHOR_DOT_RADIUS = 4.dp

// Size tier (phase_3.md 3b): when the zone's shorter side on screen is below about one hit size,
// no handles and a "zoom in to resize" hint instead. Typed entry still works.
// Preemptive in 3b: at the current MIN_SCALE (0.2) only small zones reach it (e.g. 30 x 30 cm at
// default zoom). Becomes a normal zoom-out case once 3e derives MIN_SCALE from the size cap.
val MIN_ZONE_SIZE_FOR_HANDLES = 48.dp
const val ZOOM_IN_HINT = "Zoom in to resize"

/** Whether the ghost is big enough on screen to show (and grab) handles. One check for drawing,
 * tap and drag, so a hidden handle can't be hit. Per frame: two float compares. */
fun handlesFit(size: Size, minZoneSizePx: Float): Boolean = minOf(size.width, size.height) >= minZoneSizePx

// Drag sizes round to whole cm: finer than a finger can place, and the fields stay readable.
// Precision, not grid snapping (snapping is deferred, known_issues).
const val DRAG_STEP_CM = 1.0

/** [valueCm] rounded to [DRAG_STEP_CM], kept inside the size limits so a coarser step can't round
 * below the minimum. kotlin.math.round rounds .5 to even; fine for a drag. */
fun roundToDragStep(valueCm: Double): Double = (round(valueCm / DRAG_STEP_CM) * DRAG_STEP_CM)
    .coerceIn(GrowZoneLimits.MIN_DIMENSION_CM, GrowZoneLimits.MAX_WIDTH_DEPTH_CM)

/** Screen centre of [corner]'s handle: the corner pushed out diagonally by [gapPx].
 * (2f - 1) is -1 on the left/top and +1 on the right/bottom. Shared by drawing and hit-testing. */
fun handleCenter(topLeft: Offset, size: Size, corner: Anchor, gapPx: Float): Offset {
    val fx = corner.fx.toFloat()
    val fy = corner.fy.toFloat()
    return Offset(
        topLeft.x + size.width * fx + gapPx * (2 * fx - 1),
        topLeft.y + size.height * fy + gapPx * (2 * fy - 1)
    )
}

/** The handle whose centre is nearest [point] within [hitRadiusPx], or null. Null too when the
 * zone is below [minZoneSizePx] on screen (handles hidden, [handlesFit]). Nearest wins so
 * overlapping hit areas still pick the closest. Called per tap, not per frame. */
fun hitTestHandle(
    point: Offset,
    topLeft: Offset,
    size: Size,
    gapPx: Float,
    hitRadiusPx: Float,
    minZoneSizePx: Float
): Anchor? {
    if (!handlesFit(size, minZoneSizePx)) return null
    val corners = Anchor.entries
    var best: Anchor? = null
    var bestDistSq = hitRadiusPx * hitRadiusPx
    // Index loop: compiles to a plain int loop, no Iterator allocation.
    for (i in corners.indices) {
        val d = point - handleCenter(topLeft, size, corners[i], gapPx)
        val distSq = d.x * d.x + d.y * d.y
        if (distSq <= bestDistSq) {
            best = corners[i]
            bestDistSq = distSq
        }
    }
    return best
}

fun DrawScope.drawAnchor(center: Offset, radiusPx: Float, color: Color) {
    drawCircle(
        color = color,
        radius = radiusPx,
        center = center
    )
}

fun DrawScope.drawResizeHandle(center: Offset, radiusPx: Float, color: Color) {
    // Plain dot for now; the square-with-arrow handle is in the Phase 3 polish list (phase_3.md).
    drawCircle(
        color = color,
        radius = radiusPx,
        center = center
    )
}

/** All four handles plus the anchor dot. Runs every frame: dp.toPx() is a multiply (DrawScope is
 * a Density), Offsets are value classes, so nothing here allocates. */
fun DrawScope.drawResizeHandles(topLeft: Offset, size: Size, anchor: Anchor, selectedHandle: Anchor?) {
    val color = Color.Blue
    val gapPx = HANDLE_GAP.toPx()
    val radiusPx = HANDLE_RADIUS.toPx()
    val selectedRadiusPx = SELECTED_HANDLE_RADIUS.toPx()
    val corners = Anchor.entries
    for (i in corners.indices) {
        val corner = corners[i]
        val r = if (corner == selectedHandle) selectedRadiusPx else radiusPx
        drawResizeHandle(handleCenter(topLeft, size, corner, gapPx), r, color)
    }
    val anchorPoint = Offset(
        topLeft.x + size.width * anchor.fx.toFloat(),
        topLeft.y + size.height * anchor.fy.toFloat()
    )
    drawAnchor(anchorPoint, ANCHOR_DOT_RADIUS.toPx(), Color.Black)
}

// Size readout above the ghost: gap above the top handle row, and padding inside its background.
val READOUT_MARGIN = 8.dp
val READOUT_PADDING = 4.dp

/** The ghost's live size as shown on the canvas, e.g. "153 x 80 cm". */
fun sizeReadoutText(widthCm: Double, depthCm: Double): String = "${formatCm(widthCm)} x ${formatCm(depthCm)} cm"

/** [readout] centred above the ghost, clear of the top handle row, on a rounded background.
 * The caller measures the text once per change; this only places it, so nothing allocates per
 * frame (Offset, Size and CornerRadius are value classes). */
fun DrawScope.drawSizeReadout(readout: TextLayoutResult, ghostTopLeft: Offset, ghostSize: Size, background: Color) {
    val paddingPx = READOUT_PADDING.toPx()
    val textWidth = readout.size.width.toFloat()
    val textHeight = readout.size.height.toFloat()
    // Bottom of the readout sits above the largest (selected) top handle, plus a margin.
    val bottom = ghostTopLeft.y - HANDLE_GAP.toPx() - SELECTED_HANDLE_RADIUS.toPx() - READOUT_MARGIN.toPx()
    val textTopLeft = Offset(ghostTopLeft.x + (ghostSize.width - textWidth) / 2f, bottom - paddingPx - textHeight)
    drawLabel(readout, textTopLeft, paddingPx, background)
}

/** [hint] centred just below the ghost (no handles are drawn in this tier, so no clearance needed). */
fun DrawScope.drawZoomInHint(hint: TextLayoutResult, ghostTopLeft: Offset, ghostSize: Size, background: Color) {
    val paddingPx = READOUT_PADDING.toPx()
    val textTopLeft = Offset(
        ghostTopLeft.x + (ghostSize.width - hint.size.width) / 2f,
        ghostTopLeft.y + ghostSize.height + READOUT_MARGIN.toPx() + paddingPx
    )
    drawLabel(hint, textTopLeft, paddingPx, background)
}

/** Measured text on a rounded background, padded by [paddingPx] on every side. */
private fun DrawScope.drawLabel(text: TextLayoutResult, textTopLeft: Offset, paddingPx: Float, background: Color) {
    drawRoundRect(
        color = background,
        topLeft = Offset(textTopLeft.x - paddingPx, textTopLeft.y - paddingPx),
        size = Size(text.size.width + 2 * paddingPx, text.size.height + 2 * paddingPx),
        cornerRadius = CornerRadius(paddingPx)
    )
    drawText(text, topLeft = textTopLeft)
}
