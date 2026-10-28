package com.practice.plantUser.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.practice.plantUser.model.Anchor

fun DrawScope.drawAnchor(center: Offset, radiusPx: Float, color: Color) {
    drawCircle(
        color = Color.Black,
        radius = radiusPx,
        center = center
    )
}
fun DrawScope.drawResizeHandle(center: Offset, radiusPx: Float, color: Color) {
    // your drawCircle (later: the square with the arrow) goes here
    // this needs to change to drawing the handle image ( a grey or white square with arrow pointing out)
    drawCircle(
        color = color,
        radius = radiusPx,
        center = center
    )
}

fun DrawScope.drawResizeHandles(topLeft: Offset, size: Size, anchor: Anchor) {
    val color = Color.Blue
    val bottomRight = Offset(topLeft.x + size.width + 24.dp.toPx(), topLeft.y + size.height + 24.dp.toPx())
    val topRight = Offset(topLeft.x + size.width + 24.dp.toPx(), topLeft.y - 24.dp.toPx())
    val bottomLeft = Offset(topLeft.x - 24.dp.toPx(), topLeft.y + size.height + 24.dp.toPx())
    val topLeftAdjusted = Offset(topLeft.x - 24.dp.toPx(), topLeft.y - 24.dp.toPx())
    // need same top left calc and to pass anchor to draw anchor
    // unless we find this a feature not a bug?
    val dotRadiusPx = with(density) { 4.dp.toPx() }
    val anchorAdjusted = Offset(topLeft.x + (anchor.fx).toFloat(), topLeft.y + (anchor.fy).toFloat())
    drawAnchor(anchorAdjusted, dotRadiusPx, Color.Black)

    drawResizeHandle(topLeftAdjusted, dotRadiusPx, color)
    drawResizeHandle(bottomRight, dotRadiusPx, color)
    drawResizeHandle(topRight, dotRadiusPx, color)
    drawResizeHandle(bottomLeft, dotRadiusPx, color)
}
