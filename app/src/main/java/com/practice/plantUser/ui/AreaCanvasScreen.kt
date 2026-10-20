package com.practice.plantUser.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.practice.plantUser.data.displayName
import com.practice.plantUser.model.Area
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.GrowZoneType
import com.practice.plantUser.model.formatCm
import com.practice.plantUser.model.nestingRejectionReason
import com.practice.plantUser.ui.theme.PlantUserTheme

private const val TAG = "AreaCanvasScreen"

// Zoom range placeholders: Phase 3e derives MIN_SCALE from MAX_WIDTH_DEPTH_CM (model/SizingLimits.kt).
// Note: at MIN_SCALE the grid draws ~3,700 cells/frame on a typical phone screen (measured on this
// emulator); 3e's adaptive grid bounds that, and Phase 5 plans LOD/dot mode for extreme zoom out.
private const val MIN_SCALE = 0.2f
private const val MAX_SCALE = 5f
private val GRID_SPACING = 50.dp
private val GRID_LINE_WIDTH = 1.dp
private val GRID_COLOR = Color(0xFFDDDDDD)

// 1dp/cm placeholder so zones render at correct proportions relative to each other; the on-screen
// scale (grid step, scale bar) is Phase 3e.
private val CM_TO_DP = 1.dp
private val ZONE_BORDER_WIDTH = 2.dp
private val SELECTED_BORDER_WIDTH = 4.dp
private val GHOST_DASH_LENGTH = 6.dp
private val EDIT_PANEL_SIDE_WIDTH = 320.dp

private const val MAX_GROW_ZONE_NAME_LENGTH = 50
private val DEFAULT_TRANSFORM = CanvasTransform(scale = 1f, translation = Offset.Zero)

/** Test tag for the raw Canvas, which has no other semantics to find it by (instrumented tests tap it). */
const val AREA_CANVAS_TEST_TAG = "areaCanvas"

private fun growZoneColor(type: GrowZoneType): Color = when (type) {
    GrowZoneType.GREENHOUSE -> Color(0xFF81C784)
    GrowZoneType.PLOT -> Color(0xFFA1887F)
    GrowZoneType.BOX -> Color(0xFFFFB74D)
    GrowZoneType.WILD -> Color(0xFF64B5F6)
}

private class DrawZone(val topLeftPx: Offset, val sizePx: Size, val fillColor: Color, val borderColor: Color)

/** cm (Double) to draw-ready px (Float), converted once after the multiply (decisions.md). */
private fun toDrawZone(zone: GrowZone, allZones: List<GrowZone>, cmToPx: Float): DrawZone {
    val pos = absolutePositionCm(zone, allZones)
    val color = growZoneColor(zone.type)
    return DrawZone(
        topLeftPx = Offset((pos.xCm * cmToPx).toFloat(), (pos.yCm * cmToPx).toFloat()),
        sizePx = Size((zone.widthCm * cmToPx).toFloat(), (zone.depthCm * cmToPx).toFloat()),
        fillColor = color.copy(alpha = 0.3f),
        borderColor = color
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaCanvasScreen(
    area: Area,
    growZones: List<GrowZone>,
    onAddGrowZone: (
        name: String,
        type: GrowZoneType,
        widthCm: Double,
        depthCm: Double,
        heightCm: Double?,
        parentId: Long?
    ) -> Unit,
    onResizeGrowZone: (id: Long, widthCm: Double, depthCm: Double, heightCm: Double?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // Rotation (decisions.md, provisional): focus and transform reset together. Selection and the
    // edit panel (zone id, draft text, showing warning) are user state and survive.
    var focusedZoneId by remember { mutableStateOf<Long?>(null) }
    var transform by remember { mutableStateOf(DEFAULT_TRANSFORM) }
    var selectedZoneId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingZoneId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editWidthText by rememberSaveable { mutableStateOf("") }
    var editDepthText by rememberSaveable { mutableStateOf("") }
    var editHeightText by rememberSaveable { mutableStateOf("") }
    var editWarningCm by rememberSaveable { mutableStateOf<Double?>(null) }
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val cmToPx = with(density) { CM_TO_DP.toPx() }

    val focusedZone = growZones.firstOrNull { it.id == focusedZoneId }
    val selectedZone = growZones.firstOrNull { it.id == selectedZoneId }
    val editingZone = growZones.firstOrNull { it.id == editingZoneId }
    // Name box path: to the selected zone if any, else to the focused zone (root first).
    val breadcrumbChain = remember(growZones, selectedZone, focusedZoneId) {
        zoneChain(growZones, selectedZone?.id ?: focusedZoneId)
    }
    val editInput = remember(editWidthText, editDepthText, editHeightText) {
        checkSizeInput(editWidthText, editDepthText, editHeightText)
    }
    // Ghost preview: only while both parsed values are valid. Same Doubles applyEdit() sends.
    val ghostZone = remember(editingZone, editInput) {
        val widthCm = editInput.widthCm
        val depthCm = editInput.depthCm
        if (editingZone != null && widthCm != null && depthCm != null) {
            editingZone.copy(widthCm = widthCm, depthCm = depthCm)
        } else {
            null
        }
    }

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

    // Single place selection changes. Ignored while the edit panel is open, so a stray tap (canvas
    // or breadcrumb) can't discard a draft; Save/Cancel/back close the panel first.
    fun selectZone(id: Long?, source: String) {
        if (editingZoneId != null) {
            Log.d(TAG, "selectZone: ignored from $source, edit panel open for zoneId=$editingZoneId")
            return
        }
        Log.d(TAG, "selectZone: zoneId=$id from $source (was $selectedZoneId, focused=$focusedZoneId)")
        selectedZoneId = id
    }

    // The gesture handler only calls this, so the tap mapping can change without touching the actions.
    fun onZoneTapped(id: Long?) = selectZone(id, "canvas tap")

    fun enterZone(id: Long) {
        val zone = growZones.firstOrNull { it.id == id }
        if (zone == null) {
            Log.e(TAG, "enterZone: zoneId=$id not found in areaId=${area.id}")
            return
        }
        Log.d(TAG, "enterZone: zoneId=$id")
        selectedZoneId = null
        zoomToZone(zone)
    }

    fun openEditPanel(id: Long) {
        val zone = growZones.firstOrNull { it.id == id }
        if (zone == null) {
            Log.e(TAG, "openEditPanel: zoneId=$id not found in areaId=${area.id}")
            return
        }
        Log.d(TAG, "openEditPanel: zoneId=$id current ${zone.widthCm}x${zone.depthCm} heightCm=${zone.heightCm}")
        editingZoneId = id
        editWidthText = formatCm(zone.widthCm)
        editDepthText = formatCm(zone.depthCm)
        editHeightText = zone.heightCm?.let(::formatCm) ?: ""
        editWarningCm = null
    }

    fun closeEditPanel() {
        editingZoneId = null
        editWarningCm = null
    }

    fun applyEdit() {
        val id = editingZoneId
        val widthCm = editInput.widthCm
        val depthCm = editInput.depthCm
        if (id == null || widthCm == null || depthCm == null || !editInput.isValid) {
            Log.e(
                TAG,
                "applyEdit: invalid draft zoneId=$id width='$editWidthText' depth='$editDepthText' " +
                    "height='$editHeightText'"
            )
            return
        }
        Log.d(TAG, "applyEdit: zoneId=$id -> ${widthCm}x$depthCm heightCm=${editInput.heightCm}")
        onResizeGrowZone(id, widthCm, depthCm, editInput.heightCm)
        closeEditPanel()
    }

    fun saveEdit() {
        val warnCm = editInput.softWarnValueCm
        if (warnCm != null) {
            Log.d(TAG, "saveEdit: soft warning at ${warnCm}cm for zoneId=$editingZoneId")
            editWarningCm = warnCm
        } else {
            applyEdit()
        }
    }

    // decisions.md 2026-10-17: back clears selection/panel first, then zooms out one level, then leaves.
    // Shared by system back and the toolbar arrow so the two can't disagree.
    fun navigateBack(source: String) {
        when {
            selectedZoneId != null || editingZoneId != null -> {
                Log.d(TAG, "navigateBack($source): clearing selection=$selectedZoneId editing=$editingZoneId")
                selectedZoneId = null
                closeEditPanel()
            }
            focusedZoneId != null -> {
                Log.d(TAG, "navigateBack($source): zooming out from zoneId=$focusedZoneId")
                zoomOutOneLevel()
            }
            else -> {
                Log.d(TAG, "navigateBack($source): leaving areaId=${area.id}")
                onBack()
            }
        }
    }

    BackHandler { navigateBack("system back") }

    val accentColor = focusedZone?.let { growZoneColor(it.type) } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    ZoneBreadcrumb(
                        areaName = area.name,
                        chain = breadcrumbChain,
                        selectedZoneId = selectedZone?.id,
                        onAreaClick = { selectZone(null, "breadcrumb") },
                        onZoneClick = { zone -> selectZone(zone.id, "breadcrumb") }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateBack("toolbar arrow") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val isLandscape = maxWidth > maxHeight
            PannableZoomableGrid(
                growZones = growZones,
                transform = transform,
                selectedZoneId = selectedZoneId,
                ghostZone = ghostZone,
                selectionColor = MaterialTheme.colorScheme.primary,
                ghostColor = MaterialTheme.colorScheme.onSurface,
                onTransformChange = { transform = it },
                onCanvasSizeChange = { canvasSizePx = it },
                onTap = { pointCm -> onZoneTapped(hitTestZone(growZones, pointCm, focusedZoneId)?.id) },
                cmToPx = cmToPx,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (focusedZoneId != null) {
                    SmallFloatingActionButton(
                        onClick = { zoomOutOneLevel() },
                        containerColor = accentColor
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zoom out")
                    }
                }
                SmallFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = accentColor
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add GrowZone")
                }
            }

            if (editingZone != null) {
                EditSizePanel(
                    zoneName = editingZone.name,
                    widthText = editWidthText,
                    depthText = editDepthText,
                    heightText = editHeightText,
                    onWidthChange = {
                        editWidthText = it
                        editWarningCm = null
                    },
                    onDepthChange = {
                        editDepthText = it
                        editWarningCm = null
                    },
                    onHeightChange = {
                        editHeightText = it
                        editWarningCm = null
                    },
                    sizeInput = editInput,
                    warningValueCm = editWarningCm,
                    onSave = { saveEdit() },
                    onCancel = {
                        Log.d(TAG, "edit panel: cancelled for zoneId=$editingZoneId")
                        closeEditPanel()
                    },
                    onAcceptWarning = { applyEdit() },
                    onCancelWarning = {
                        Log.d(TAG, "edit panel: soft warning cancelled for zoneId=$editingZoneId, draft kept")
                        editWarningCm = null
                    },
                    modifier =
                    if (isLandscape) {
                        Modifier.align(Alignment.CenterEnd).width(EDIT_PANEL_SIDE_WIDTH).padding(8.dp)
                    } else {
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp)
                    }
                )
            } else if (selectedZone != null) {
                ZoneActionBar(
                    showEnter = selectedZone.id != focusedZoneId,
                    onEnter = { enterZone(selectedZone.id) },
                    onEditSize = { openEditPanel(selectedZone.id) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddGrowZoneDialog(
            parentZone = focusedZone,
            allZones = growZones,
            onConfirm = { name, type, widthCm, depthCm, heightCm ->
                onAddGrowZone(name, type, widthCm, depthCm, heightCm, focusedZoneId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun PannableZoomableGrid(
    growZones: List<GrowZone>,
    transform: CanvasTransform,
    selectedZoneId: Long?,
    ghostZone: GrowZone?,
    selectionColor: Color,
    ghostColor: Color,
    onTransformChange: (CanvasTransform) -> Unit,
    onCanvasSizeChange: (Size) -> Unit,
    onTap: (pointCm: Offset) -> Unit,
    cmToPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gridSpacingPx = with(density) { GRID_SPACING.toPx() }
    val strokeWidthPx = with(density) { GRID_LINE_WIDTH.toPx() }
    val zoneBorderPx = with(density) { ZONE_BORDER_WIDTH.toPx() }
    val selectedBorderPx = with(density) { SELECTED_BORDER_WIDTH.toPx() }
    val ghostDashPx = with(density) { GHOST_DASH_LENGTH.toPx() }

    // pointerInput(Unit) launches its gesture-detection coroutine once and never restarts it, so
    // anything it reads that can change later (transform, callbacks closing over growZones) must
    // go through rememberUpdatedState — otherwise the very first composition's values get baked
    // in permanently, and e.g. a zone created after the screen opened would never be tappable.
    val currentTransform by rememberUpdatedState(transform)
    val currentOnTransformChange by rememberUpdatedState(onTransformChange)
    val currentOnTap by rememberUpdatedState(onTap)

    val drawZones = remember(growZones, cmToPx) {
        growZones.map { zone -> toDrawZone(zone, growZones, cmToPx) }
    }
    // Index into drawZones (same order as growZones), -1 if nothing selected. Found once per change,
    // not per frame.
    val selectedIndex = remember(growZones, selectedZoneId) { growZones.indexOfFirst { it.id == selectedZoneId } }
    val ghostDraw = remember(ghostZone, growZones, cmToPx) { ghostZone?.let { toDrawZone(it, growZones, cmToPx) } }
    // Strokes (and the ghost's dash PathEffect) built once, not per frame (known_issues).
    val zoneBorderStroke = remember(zoneBorderPx) { Stroke(width = zoneBorderPx) }
    val selectedStroke = remember(selectedBorderPx) { Stroke(width = selectedBorderPx) }
    val gridStroke = remember(strokeWidthPx) { Stroke(width = strokeWidthPx) }
    val ghostStroke = remember(zoneBorderPx, ghostDashPx) {
        Stroke(width = zoneBorderPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ghostDashPx, ghostDashPx)))
    }
    Canvas(
        modifier =
        modifier
            .testTag(AREA_CANVAS_TEST_TAG)
            .onSizeChanged { onCanvasSizeChange(it.toSize()) }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val pointWorldPx = (tapOffset - currentTransform.translation) / currentTransform.scale
                    currentOnTap(pointWorldPx / cmToPx)
                }
            }.pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val updated = updateTransform(currentTransform, centroid, pan, zoom, MIN_SCALE, MAX_SCALE)
                    currentOnTransformChange(updated)
                }
            }
    ) {
        val cellSize = Size(gridSpacingPx * transform.scale, gridSpacingPx * transform.scale)
        for (worldOrigin in visibleGridCellOrigins(size, transform, gridSpacingPx)) {
            drawRect(
                color = GRID_COLOR,
                topLeft = worldOrigin * transform.scale + transform.translation,
                size = cellSize,
                style = gridStroke
            )
        }
        for (i in drawZones.indices) {
            val dz = drawZones[i]
            val topLeft = dz.topLeftPx * transform.scale + transform.translation
            val size = dz.sizePx * transform.scale
            drawRect(color = dz.fillColor, topLeft = topLeft, size = size)
            drawRect(color = dz.borderColor, topLeft = topLeft, size = size, style = zoneBorderStroke)
        }
        // Drawn after all zones so a child can't paint over its selected parent's highlight.
        if (selectedIndex >= 0) {
            val dz = drawZones[selectedIndex]
            drawRect(
                color = selectionColor,
                topLeft = dz.topLeftPx * transform.scale + transform.translation,
                size = dz.sizePx * transform.scale,
                style = selectedStroke
            )
        }
        if (ghostDraw != null) {
            drawRect(
                color = ghostColor,
                topLeft = ghostDraw.topLeftPx * transform.scale + transform.translation,
                size = ghostDraw.sizePx * transform.scale,
                style = ghostStroke
            )
        }
    }
}

@Composable
private fun AddGrowZoneDialog(
    parentZone: GrowZone?,
    allZones: List<GrowZone>,
    onConfirm: (name: String, type: GrowZoneType, widthCm: Double, depthCm: Double, heightCm: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(GrowZoneType.GREENHOUSE) }
    var widthText by rememberSaveable { mutableStateOf("100") }
    var depthText by rememberSaveable { mutableStateOf("100") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var warningCm by rememberSaveable { mutableStateOf<Double?>(null) }

    val rejectionReason = nestingRejectionReason(parent = parentZone, allZones = allZones)
    val sizeInput = remember(widthText, depthText, heightText) { checkSizeInput(widthText, depthText, heightText) }
    val canSubmit = name.isNotBlank() && sizeInput.isValid && rejectionReason == null

    fun confirm() {
        val widthCm = sizeInput.widthCm
        val depthCm = sizeInput.depthCm
        if (!canSubmit || widthCm == null || depthCm == null) {
            Log.e(
                TAG,
                "AddGrowZoneDialog.confirm: invalid input name='$name' width='$widthText' " +
                    "depth='$depthText' height='$heightText' nesting=$rejectionReason"
            )
            return
        }
        Log.d(TAG, "AddGrowZoneDialog.confirm: '$name' $type ${widthCm}x$depthCm heightCm=${sizeInput.heightCm}")
        onConfirm(name.trim(), type, widthCm, depthCm, sizeInput.heightCm)
    }

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
                    supportingText = { Text("${name.length}/$MAX_GROW_ZONE_NAME_LENGTH") }
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow {
                    GrowZoneType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = GrowZoneType.entries.size)
                        ) {
                            Text(option.displayName())
                        }
                    }
                }
                if (rejectionReason != null) {
                    Text(rejectionReason, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                SizeFields(
                    widthText = widthText,
                    depthText = depthText,
                    heightText = heightText,
                    onWidthChange = {
                        widthText = it
                        warningCm = null
                    },
                    onDepthChange = {
                        depthText = it
                        warningCm = null
                    },
                    onHeightChange = {
                        heightText = it
                        warningCm = null
                    },
                    sizeInput = sizeInput
                )
                warningCm?.let { valueCm ->
                    Spacer(Modifier.height(8.dp))
                    SoftWarningMessage(valueCm)
                }
            }
        },
        confirmButton = {
            if (warningCm != null) {
                TextButton(onClick = { confirm() }) { Text("Accept") }
            } else {
                TextButton(
                    onClick = {
                        val warnCm = sizeInput.softWarnValueCm
                        if (warnCm != null) {
                            Log.d(TAG, "AddGrowZoneDialog: soft warning at ${warnCm}cm")
                            warningCm = warnCm
                        } else {
                            confirm()
                        }
                    },
                    enabled = canSubmit
                ) {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            // Cancel on the warning returns to editing (draft kept); otherwise it closes the dialog.
            TextButton(onClick = { if (warningCm != null) warningCm = null else onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

// Deletable: I have had no use of this so far
@Preview(showBackground = true)
@Composable
private fun AreaCanvasScreenPreview() {
    PlantUserTheme {
        AreaCanvasScreen(
            area = Area(1L, "Backyard"),
            growZones = emptyList(),
            onAddGrowZone = { _, _, _, _, _, _ -> },
            onResizeGrowZone = { _, _, _, _ -> },
            onBack = {}
        )
    }
}
