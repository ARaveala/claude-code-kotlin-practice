package com.practice.plant_user.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.floor

/**
 * Pan/zoom state for the Area canvas, in screen pixel terms: screenPoint = worldPoint * scale + translation.
 */
data class CanvasTransform(val scale: Float, val translation: Offset)

/**
 * Applies one gesture frame update (from Compose's detectTransformGestures) to [current].
 *
 * Keeps the world point under [centroid] fixed on screen while scale changes, by solving for the
 * translation that maps that same world point back to [centroid] under the new scale. Without this,
 * pinch zoom feels like it's fighting you instead of zooming around your fingers.
 */
fun updateTransform(
    current: CanvasTransform,
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    minScale: Float,
    maxScale: Float,
): CanvasTransform {
    val newScale = (current.scale * zoom).coerceIn(minScale, maxScale)
    val worldPointUnderCentroid = (centroid - current.translation) / current.scale
    val newTranslation = centroid - worldPointUnderCentroid * newScale + pan
    return CanvasTransform(scale = newScale, translation = newTranslation)
}

/**
 * World space top left origins of every grid cell of size [gridSpacingPx] visible within a canvas
 * of [canvasSize], given the current [transform]. Only computes cells inside the current viewport,
 * so cost is bounded by screen size, not by how far the canvas has been panned.
 */
fun visibleGridCellOrigins(
    canvasSize: Size,
    transform: CanvasTransform,
    gridSpacingPx: Float,
): List<Offset> {
    val topLeftWorld = (Offset.Zero - transform.translation) / transform.scale
    val bottomRightWorld = (Offset(canvasSize.width, canvasSize.height) - transform.translation) / transform.scale

    val startX = floor(topLeftWorld.x / gridSpacingPx) * gridSpacingPx
    val startY = floor(topLeftWorld.y / gridSpacingPx) * gridSpacingPx

    val origins = mutableListOf<Offset>()
    var worldX = startX
    while (worldX <= bottomRightWorld.x) {
        var worldY = startY
        while (worldY <= bottomRightWorld.y) {
            origins.add(Offset(worldX, worldY))
            worldY += gridSpacingPx
        }
        worldX += gridSpacingPx
    }
    return origins
}
