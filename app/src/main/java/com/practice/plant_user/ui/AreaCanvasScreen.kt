package com.practice.plant_user.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.plant_user.ui.theme.Plant_userTheme

// Placeholders — real caps are a Phase 3 "Sizing Caps" deliverable (domain_model.md), determined
// by testing, not hardcoded assumptions. Note: at MIN_SCALE the grid draws ~3,700 cells/frame on a
// typical phone screen (measured on this emulator); roadmap.md Phase 5 already plans LOD/dot mode
// rendering at extreme zoom out to address this, so it's not being solved here.
private const val MIN_SCALE = 0.2f
private const val MAX_SCALE = 5f
private val GRID_SPACING = 50.dp
private val GRID_LINE_WIDTH = 1.dp
private val GRID_COLOR = Color(0xFFDDDDDD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaCanvasScreen(area: Area, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(area.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        PannableZoomableGrid(modifier = Modifier.fillMaxSize().padding(innerPadding))
    }
}

@Composable
private fun PannableZoomableGrid(modifier: Modifier = Modifier) {
    var transform by remember { mutableStateOf(CanvasTransform(scale = 1f, translation = Offset.Zero)) }
    val density = LocalDensity.current
    val gridSpacingPx = with(density) { GRID_SPACING.toPx() }
    val strokeWidthPx = with(density) { GRID_LINE_WIDTH.toPx() }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                transform = updateTransform(transform, centroid, pan, zoom, MIN_SCALE, MAX_SCALE)
            }
        },
    ) {
        val cellSize = Size(gridSpacingPx * transform.scale, gridSpacingPx * transform.scale)
        for (worldOrigin in visibleGridCellOrigins(size, transform, gridSpacingPx)) {
            drawRect(
                color = GRID_COLOR,
                topLeft = worldOrigin * transform.scale + transform.translation,
                size = cellSize,
                style = Stroke(width = strokeWidthPx),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AreaCanvasScreenPreview() {
    Plant_userTheme {
        AreaCanvasScreen(area = Area(1, "Backyard"), onBack = {})
    }
}
