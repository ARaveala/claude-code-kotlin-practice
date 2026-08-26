package com.practice.plant_user.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.practice.plant_user.data.GardenDatabase
import com.practice.plant_user.data.GrowZoneType
import com.practice.plant_user.data.displayName
import com.practice.plant_user.ui.theme.Plant_userTheme
import com.practice.plant_user.viewmodel.GrowZoneViewModel
import com.practice.plant_user.viewmodel.nestingRejectionReason

// Placeholders — real caps are a Phase 3 "Sizing Caps" deliverable (domain_model.md), determined
// by testing, not hardcoded assumptions. Note: at MIN_SCALE the grid draws ~3,700 cells/frame on a
// typical phone screen (measured on this emulator); roadmap.md Phase 5 already plans LOD/dot mode
// rendering at extreme zoom out to address this, so it's not being solved here.
private const val MIN_SCALE = 0.2f
private const val MAX_SCALE = 5f
private val GRID_SPACING = 50.dp
private val GRID_LINE_WIDTH = 1.dp
private val GRID_COLOR = Color(0xFFDDDDDD)

// No real-world cm anchor yet (known_issues.md) — Phase 3 defines the actual scale. 1dp/cm is a
// throwaway placeholder purely so zones render at roughly correct proportions relative to each other.
private val CM_TO_DP = 1.dp
private val ZONE_BORDER_WIDTH = 2.dp

private const val MAX_GROW_ZONE_NAME_LENGTH = 50
private val DEFAULT_TRANSFORM = CanvasTransform(scale = 1f, translation = Offset.Zero)

data class GrowZone(
    val id: Long,
    val parentGrowZoneId: Long?,
    val type: GrowZoneType,
    val name: String,
    val widthCm: Double,
    val depthCm: Double,
    val heightCm: Double?,
    val xCm: Double,
    val yCm: Double,
)

/** Whether [text] parses as a real, usable dimension — rejects blank/non-numeric/zero/negative
 * so a GrowZone can never be created 0x0 (hard to spot on the canvas, hard to delete after). */
fun isPositiveDimension(text: String): Boolean {
    val value = text.toDoubleOrNull() ?: return false
    return value > 0.0
}

/** Gates a dimension TextField's input to digits and at most one decimal point, letting the
 * user type a partial value (e.g. "12.") without rejecting it outright. */
fun coerceDimensionInput(current: String, candidate: String): String =
    if (candidate.isEmpty() || candidate.matches(Regex("^\\d*\\.?\\d*$"))) candidate else current

private fun growZoneColor(type: GrowZoneType): Color = when (type) {
    GrowZoneType.GREENHOUSE -> Color(0xFF81C784)
    GrowZoneType.PLOT -> Color(0xFFA1887F)
    GrowZoneType.BOX -> Color(0xFFFFB74D)
    GrowZoneType.WILD -> Color(0xFF64B5F6)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaCanvasScreen(area: Area, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val growZoneViewModel: GrowZoneViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GrowZoneViewModel(GardenDatabase.getInstance(context).growZoneDao(), area.id) }
        },
    )
    val growZones by growZoneViewModel.growZones.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var focusedZoneId by remember { mutableStateOf<Long?>(null) }
    var transform by remember { mutableStateOf(DEFAULT_TRANSFORM) }
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val cmToPx = with(density) { CM_TO_DP.toPx() }

    val focusedZone = growZones.firstOrNull { it.id == focusedZoneId }

    fun zoomToZone(zone: GrowZone) {
        focusedZoneId = zone.id
        transform = fitTransform(zone, growZones, canvasSizePx, cmToPx, MIN_SCALE, MAX_SCALE)
    }

    fun zoomOutOneLevel() {
        val parentZone = growZones.firstOrNull { it.id == focusedZone?.parentGrowZoneId }
        if (parentZone != null) {
            zoomToZone(parentZone)
        } else {
            focusedZoneId = null
            transform = DEFAULT_TRANSFORM
        }
    }

    // System/hardware back zooms out one nesting level before it leaves the Area, matching the
    // small on-canvas back arrow — otherwise the two "back" affordances would disagree.
    BackHandler {
        if (focusedZoneId != null) zoomOutOneLevel() else onBack()
    }

    val accentColor = focusedZone?.let { growZoneColor(it.type) } ?: MaterialTheme.colorScheme.primary

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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PannableZoomableGrid(
                growZones = growZones,
                transform = transform,
                onTransformChange = { transform = it },
                onCanvasSizeChange = { canvasSizePx = it },
                onTap = { pointCm ->
                    val tapped = hitTestZone(growZones, pointCm)
                    if (tapped != null) zoomToZone(tapped)
                },
                cmToPx = cmToPx,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (focusedZoneId != null) {
                    SmallFloatingActionButton(
                        onClick = { zoomOutOneLevel() },
                        containerColor = accentColor,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zoom out")
                    }
                }
                SmallFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = accentColor,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add GrowZone")
                }
            }
        }
    }

    if (showAddDialog) {
        AddGrowZoneDialog(
            parentZone = focusedZone,
            allZones = growZones,
            onConfirm = { name, type, widthCm, depthCm, heightCm ->
                growZoneViewModel.addGrowZone(name, type, widthCm, depthCm, heightCm, focusedZoneId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun PannableZoomableGrid(
    growZones: List<GrowZone>,
    transform: CanvasTransform,
    onTransformChange: (CanvasTransform) -> Unit,
    onCanvasSizeChange: (Size) -> Unit,
    onTap: (pointCm: Offset) -> Unit,
    cmToPx: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val gridSpacingPx = with(density) { GRID_SPACING.toPx() }
    val strokeWidthPx = with(density) { GRID_LINE_WIDTH.toPx() }
    val zoneBorderPx = with(density) { ZONE_BORDER_WIDTH.toPx() }

    // pointerInput(Unit) launches its gesture-detection coroutine once and never restarts it, so
    // anything it reads that can change later (transform, callbacks closing over growZones) must
    // go through rememberUpdatedState — otherwise the very first composition's values get baked
    // in permanently, and e.g. a zone created after the screen opened would never be tappable.
    val currentTransform by rememberUpdatedState(transform)
    val currentOnTransformChange by rememberUpdatedState(onTransformChange)
    val currentOnTap by rememberUpdatedState(onTap)

    Canvas(
        modifier = modifier
            .onSizeChanged { onCanvasSizeChange(it.toSize()) }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val pointWorldPx = (tapOffset - currentTransform.translation) / currentTransform.scale
                    currentOnTap(pointWorldPx / cmToPx)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val updated = updateTransform(currentTransform, centroid, pan, zoom, MIN_SCALE, MAX_SCALE)
                    currentOnTransformChange(updated)
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

        for (zone in growZones) {
            val posCm = absolutePositionCm(zone, growZones)
            val topLeftPx = posCm * cmToPx
            val sizePx = Size(zone.widthCm.toFloat() * cmToPx, zone.depthCm.toFloat() * cmToPx)
            val color = growZoneColor(zone.type)
            drawRect(
                color = color.copy(alpha = 0.3f),
                topLeft = topLeftPx * transform.scale + transform.translation,
                size = sizePx * transform.scale,
            )
            drawRect(
                color = color,
                topLeft = topLeftPx * transform.scale + transform.translation,
                size = sizePx * transform.scale,
                style = Stroke(width = zoneBorderPx),
            )
        }
    }
}

@Composable
private fun AddGrowZoneDialog(
    parentZone: GrowZone?,
    allZones: List<GrowZone>,
    onConfirm: (name: String, type: GrowZoneType, widthCm: Double, depthCm: Double, heightCm: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GrowZoneType.GREENHOUSE) }
    var widthText by remember { mutableStateOf("100") }
    var depthText by remember { mutableStateOf("100") }
    var heightText by remember { mutableStateOf("") }

    val rejectionReason = nestingRejectionReason(parent = parentZone, allZones = allZones)
    val canSubmit = name.isNotBlank() &&
        isPositiveDimension(widthText) &&
        isPositiveDimension(depthText) &&
        rejectionReason == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New GrowZone") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { input -> name = coerceNameInput(name, input, MAX_GROW_ZONE_NAME_LENGTH) },
                    singleLine = true,
                    label = { Text("Zone name") },
                    supportingText = { Text("${name.length}/$MAX_GROW_ZONE_NAME_LENGTH") },
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow {
                    GrowZoneType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = GrowZoneType.entries.size),
                        ) {
                            Text(option.displayName())
                        }
                    }
                }
                if (rejectionReason != null) {
                    Text(rejectionReason, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextField(
                        value = widthText,
                        onValueChange = { input -> widthText = coerceDimensionInput(widthText, input) },
                        singleLine = true,
                        label = { Text("Width (cm)") },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = depthText,
                        onValueChange = { input -> depthText = coerceDimensionInput(depthText, input) },
                        singleLine = true,
                        label = { Text("Depth (cm)") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = heightText,
                    onValueChange = { input -> heightText = coerceDimensionInput(heightText, input) },
                    singleLine = true,
                    label = { Text("Height (cm, optional)") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name.trim(), type, widthText.toDouble(), depthText.toDouble(), heightText.toDoubleOrNull())
                },
                enabled = canSubmit,
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun AreaCanvasScreenPreview() {
    Plant_userTheme {
        AreaCanvasScreen(area = Area(1L, "Backyard"), onBack = {})
    }
}
