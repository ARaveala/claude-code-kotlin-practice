package com.practice.plantUser.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.practice.plantUser.model.Anchor

// Handle sizes, all in dp; tune on a real phone (phase_3.md 3b, decisions.md 2026-10-20).
// Gap = offset from the corner, about half the hit size, so the corner itself stays visible.
val HANDLE_GAP = 24.dp
val HANDLE_HIT_RADIUS = 24.dp // 48 dp hit size
val HANDLE_RADIUS = 4.dp
val SELECTED_HANDLE_RADIUS = 7.dp
val ANCHOR_DOT_RADIUS = 4.dp

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

/** The handle whose centre is nearest [point] within [hitRadiusPx], or null. Nearest wins so
 * overlapping hit areas on a small zone still pick the closest. Called per tap, not per frame. */
fun hitTestHandle(point: Offset, topLeft: Offset, size: Size, gapPx: Float, hitRadiusPx: Float): Anchor? {
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
    // this needs to change to drawing the handle image ( a grey or white square with arrow pointing out)
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
