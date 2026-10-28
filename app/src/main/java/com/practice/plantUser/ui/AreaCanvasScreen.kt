package com.practice.plantUser.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.practice.plantUser.data.displayName
import com.practice.plantUser.model.Anchor
import com.practice.plantUser.model.Area
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.GrowZoneType
import com.practice.plantUser.model.PositionCm
import com.practice.plantUser.model.dragResize
import com.practice.plantUser.model.formatCm
import com.practice.plantUser.model.nestingRejectionReason
import com.practice.plantUser.model.resize
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
    onResizeGrowZone: (id: Long, widthCm: Double, depthCm: Double, heightCm: Double?, anchor: Anchor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // Rotation (decisions.md, provisional): focus and transform reset together. Selection and the
    // edit session (zone, draft text, warning, selected handle) are user state and survive.
    var focusedZoneId by remember { mutableStateOf<Long?>(null) }
    var transform by remember { mutableStateOf(DEFAULT_TRANSFORM) }
    var selectedZoneId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Null = edit panel closed. Every change is a copy(): one small object per keystroke or drag
    // frame, the previous one becomes garbage.
    var editSession by rememberSaveable(stateSaver = EditSessionSaver) { mutableStateOf<EditSession?>(null) }
    // Composition-time snapshot for reading below; the functions read editSession itself, so a
    // callback never acts on a stale copy.
    val session = editSession
    val resizeAnchor = session?.anchor ?: Anchor.TOP_LEFT
    // Set at handle drag start, read on every drag frame (never in composition, so no recompose).
    var dragParentOrigin by remember { mutableStateOf(PositionCm(0.0, 0.0)) }
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val cmToPx = with(density) { CM_TO_DP.toPx() }

    val focusedZone = growZones.firstOrNull { it.id == focusedZoneId }
    val selectedZone = growZones.firstOrNull { it.id == selectedZoneId }
    val editingZone = session?.let { s -> growZones.firstOrNull { it.id == s.zoneId } }
    // Name box path: to the selected zone if any, else to the focused zone (root first).
    val breadcrumbChain = remember(growZones, selectedZone, focusedZoneId) {
        zoneChain(growZones, selectedZone?.id ?: focusedZoneId)
    }
    val editInput = remember(session?.widthText, session?.depthText, session?.heightText) {
        checkSizeInput(session?.widthText ?: "", session?.depthText ?: "", session?.heightText ?: "")
    }
    // Ghost preview: only while both parsed values are valid. Same Doubles applyEdit() sends.
    val ghostZone = remember(editingZone, editInput, resizeAnchor) {
        val widthCm = editInput.widthCm
        val depthCm = editInput.depthCm
        if (editingZone != null && widthCm != null && depthCm != null) {
            resize(editingZone, widthCm, depthCm, resizeAnchor)
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
        val open = editSession
        if (open != null) {
            Log.d(TAG, "selectZone: ignored from $source, edit panel open for zoneId=${open.zoneId}")
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
        editSession = EditSession(
            zoneId = id,
            widthText = formatCm(zone.widthCm),
            depthText = formatCm(zone.depthCm),
            heightText = zone.heightCm?.let(::formatCm) ?: ""
        )
    }

    fun closeEditPanel() {
        editSession = null
    }

    // Null = tap missed every handle, which clears the selection (back to the top-left anchor).
    fun onHandleTapped(handle: Anchor?) {
        Log.d(TAG, "onHandleTapped: handle=$handle (was ${editSession?.selectedHandle}) zoneId=${editSession?.zoneId}")
        editSession = editSession?.copy(selectedHandle = handle)
    }

    // Grabbing a handle selects it, so its opposite corner is the anchor for the whole drag.
    fun onHandleDragStart(handle: Anchor) {
        val zone = editingZone
        if (zone == null) {
            Log.e(TAG, "onHandleDragStart: handle=$handle but no zone is being edited (session=${editSession?.zoneId})")
            return
        }
        editSession = editSession?.copy(selectedHandle = handle)
        dragParentOrigin = parentOriginCm(zone, growZones)
        Log.d(TAG, "onHandleDragStart: handle=$handle zoneId=${zone.id} parentOrigin=$dragParentOrigin")
    }

    // Per drag frame: the dragged corner (absolute cm) -> zone frame -> dragResize -> the size
    // fields. The fields stay the single source of truth, so ghost, warning and Save are the typed
    // path unchanged. Allocates per frame: two Strings, plus dragResize's GrowZone copies.
    fun onHandleDrag(cornerCm: Offset) {
        val current = editSession ?: return
        val zone = editingZone ?: return
        val resized = dragResize(
            zone,
            current.anchor,
            cornerCm.x - dragParentOrigin.xCm,
            cornerCm.y - dragParentOrigin.yCm
        )
        editSession = current.copy(
            widthText = formatCm(roundToDragStep(resized.widthCm)),
            depthText = formatCm(roundToDragStep(resized.depthCm)),
            warningCm = null
        )
    }

    fun onHandleDragEnd() {
        val current = editSession
        Log.d(
            TAG,
            "onHandleDragEnd: zoneId=${current?.zoneId} handle=${current?.selectedHandle} -> " +
                "${current?.widthText} x ${current?.depthText} cm"
        )
    }

    fun applyEdit() {
        val current = editSession
        val widthCm = editInput.widthCm
        val depthCm = editInput.depthCm
        if (current == null || widthCm == null || depthCm == null || !editInput.isValid) {
            Log.e(
                TAG,
                "applyEdit: invalid draft zoneId=${current?.zoneId} width='${current?.widthText}' " +
                    "depth='${current?.depthText}' height='${current?.heightText}'"
            )
            return
        }
        val id = current.zoneId
        Log.d(TAG, "applyEdit: zoneId=$id -> ${widthCm}x$depthCm heightCm=${editInput.heightCm}")
        onResizeGrowZone(id, widthCm, depthCm, editInput.heightCm, resizeAnchor)
        closeEditPanel()
    }

    fun saveEdit() {
        val warnCm = editInput.softWarnValueCm
        if (warnCm != null) {
            Log.d(TAG, "saveEdit: soft warning at ${warnCm}cm for zoneId=${editSession?.zoneId}")
            editSession = editSession?.copy(warningCm = warnCm)
        } else {
            applyEdit()
        }
    }

    // decisions.md 2026-10-17: back clears selection/panel first, then zooms out one level, then leaves.
    // Shared by system back and the toolbar arrow so the two can't disagree.
    fun navigateBack(source: String) {
        when {
            selectedZoneId != null || editSession != null -> {
                Log.d(TAG, "navigateBack($source): clearing selection=$selectedZoneId editing=${editSession?.zoneId}")
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
                resizeAnchor = resizeAnchor,
                selectedHandle = session?.selectedHandle,
                selectionColor = MaterialTheme.colorScheme.primary,
                ghostColor = MaterialTheme.colorScheme.onSurface,
                readoutBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                onTransformChange = { transform = it },
                onCanvasSizeChange = { canvasSizePx = it },
                onTap = { pointCm -> onZoneTapped(hitTestZone(growZones, pointCm, focusedZoneId)?.id) },
                onHandleTap = { onHandleTapped(it) },
                onHandleDragStart = { onHandleDragStart(it) },
                onHandleDrag = { onHandleDrag(it) },
                onHandleDragEnd = { onHandleDragEnd() },
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

            if (editingZone != null && session != null) {
                EditSizePanel(
                    zoneName = editingZone.name,
                    widthText = session.widthText,
                    depthText = session.depthText,
                    heightText = session.heightText,
                    // Any edit clears a showing soft warning, as before.
                    onWidthChange = { editSession = editSession?.copy(widthText = it, warningCm = null) },
                    onDepthChange = { editSession = editSession?.copy(depthText = it, warningCm = null) },
                    onHeightChange = { editSession = editSession?.copy(heightText = it, warningCm = null) },
                    sizeInput = editInput,
                    warningValueCm = session.warningCm,
                    onSave = { saveEdit() },
                    onCancel = {
                        Log.d(TAG, "edit panel: cancelled for zoneId=${editSession?.zoneId}")
                        closeEditPanel()
                    },
                    onAcceptWarning = { applyEdit() },
                    onCancelWarning = {
                        Log.d(TAG, "edit panel: soft warning cancelled for zoneId=${editSession?.zoneId}, draft kept")
                        editSession = editSession?.copy(warningCm = null)
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
    resizeAnchor: Anchor,
    selectedHandle: Anchor?,
    selectionColor: Color,
    ghostColor: Color,
    readoutBackground: Color,
    onTransformChange: (CanvasTransform) -> Unit,
    onCanvasSizeChange: (Size) -> Unit,
    onTap: (pointCm: Offset) -> Unit,
    onHandleTap: (Anchor?) -> Unit,
    onHandleDragStart: (Anchor) -> Unit,
    onHandleDrag: (cornerCm: Offset) -> Unit,
    onHandleDragEnd: () -> Unit,
    cmToPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gridSpacingPx = with(density) { GRID_SPACING.toPx() }
    val strokeWidthPx = with(density) { GRID_LINE_WIDTH.toPx() }
    val zoneBorderPx = with(density) { ZONE_BORDER_WIDTH.toPx() }
    val selectedBorderPx = with(density) { SELECTED_BORDER_WIDTH.toPx() }
    val ghostDashPx = with(density) { GHOST_DASH_LENGTH.toPx() }
    val handleGapPx = with(density) { HANDLE_GAP.toPx() }
    val handleHitRadiusPx = with(density) { HANDLE_HIT_RADIUS.toPx() }

    // pointerInput(Unit) launches its gesture-detection coroutine once and never restarts it, so
    // anything it reads that can change later (transform, callbacks closing over growZones) must
    // go through rememberUpdatedState — otherwise the very first composition's values get baked
    // in permanently, and e.g. a zone created after the screen opened would never be tappable.
    val currentTransform by rememberUpdatedState(transform)
    val currentOnTransformChange by rememberUpdatedState(onTransformChange)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnHandleTap by rememberUpdatedState(onHandleTap)
    val currentOnHandleDragStart by rememberUpdatedState(onHandleDragStart)
    val currentOnHandleDrag by rememberUpdatedState(onHandleDrag)
    val currentOnHandleDragEnd by rememberUpdatedState(onHandleDragEnd)

    val drawZones = remember(growZones, cmToPx) {
        growZones.map { zone -> toDrawZone(zone, growZones, cmToPx) }
    }
    // Index into drawZones (same order as growZones), -1 if nothing selected. Found once per change,
    // not per frame.
    val selectedIndex = remember(growZones, selectedZoneId) { growZones.indexOfFirst { it.id == selectedZoneId } }
    val ghostDraw = remember(ghostZone, growZones, cmToPx) { ghostZone?.let { toDrawZone(it, growZones, cmToPx) } }
    // Size readout, measured once per size change, not per frame. The style copy is remembered too:
    // this composable recomposes on every pan frame (known_issues), and copy() would allocate each time.
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val readoutStyle = remember(labelStyle, ghostColor) { labelStyle.copy(color = ghostColor) }
    val readoutLayout = remember(ghostZone?.widthCm, ghostZone?.depthCm, readoutStyle) {
        ghostZone?.let { textMeasurer.measure(sizeReadoutText(it.widthCm, it.depthCm), readoutStyle) }
    }
    // Read inside pointerInput(Unit), so it needs the same rememberUpdatedState treatment as above.
    val currentGhostDraw by rememberUpdatedState(ghostDraw)
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
                    // Edit mode with handles showing: taps only select or clear a handle, no zone
                    // hit-test (phase_3.md 3b). Same screen-space rect the handles are drawn on.
                    val ghost = currentGhostDraw
                    if (ghost != null) {
                        val topLeft = ghost.topLeftPx * currentTransform.scale + currentTransform.translation
                        val size = ghost.sizePx * currentTransform.scale
                        currentOnHandleTap(hitTestHandle(tapOffset, topLeft, size, handleGapPx, handleHitRadiusPx))
                        return@detectTapGestures
                    }
                    val pointWorldPx = (tapOffset - currentTransform.translation) / currentTransform.scale
                    currentOnTap(pointWorldPx / cmToPx)
                }
            }.pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val updated = updateTransform(currentTransform, centroid, pan, zoom, MIN_SCALE, MAX_SCALE)
                    currentOnTransformChange(updated)
                }
            }.pointerInput(Unit) {
                // Handle drag. Declared last so it sees each event first (Compose's main pass runs
                // the innermost modifier first). Consuming the move past touch slop cancels the pan
                // above; a press that never moves isn't consumed, so it stays a tap (handle select).
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val ghost = currentGhostDraw ?: return@awaitEachGesture
                    val topLeft = ghost.topLeftPx * currentTransform.scale + currentTransform.translation
                    val size = ghost.sizePx * currentTransform.scale
                    val handle =
                        hitTestHandle(down.position, topLeft, size, handleGapPx, handleHitRadiusPx)
                            ?: return@awaitEachGesture
                    val pastSlop =
                        awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                            ?: return@awaitEachGesture
                    // No jump: keep the finger's offset from the grabbed corner (not the handle
                    // centre, which sits a gap outside it) for the whole drag.
                    val corner = Offset(
                        topLeft.x + size.width * handle.fx.toFloat(),
                        topLeft.y + size.height * handle.fy.toFloat()
                    )
                    val grabOffset = corner - down.position
                    currentOnHandleDragStart(handle)
                    // Screen px -> absolute cm, same conversion as the tap. Pan is cancelled during
                    // the drag, so the transform is stable.
                    val cornerCmAt = { fingerPx: Offset ->
                        (fingerPx + grabOffset - currentTransform.translation) / currentTransform.scale / cmToPx
                    }
                    currentOnHandleDrag(cornerCmAt(pastSlop.position))
                    drag(pastSlop.id) { change ->
                        change.consume()
                        currentOnHandleDrag(cornerCmAt(change.position))
                    }
                    currentOnHandleDragEnd()
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
            val topLeft = ghostDraw.topLeftPx * transform.scale + transform.translation
            val size = ghostDraw.sizePx * transform.scale
            drawRect(
                color = ghostColor,
                topLeft = topLeft,
                size = size,
                style = ghostStroke
            )
            drawResizeHandles(topLeft, size, resizeAnchor, selectedHandle)
            if (readoutLayout != null) drawSizeReadout(readoutLayout, topLeft, size, readoutBackground)
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
            // Scrollable: in landscape the text slot is shorter than the fields, and without a scroll
            // the Column squeezes the last ones to nothing (known_issues.md, fixed 2026-10-20).
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
            onResizeGrowZone = { _, _, _, _, _ -> },
            onBack = {}
        )
    }
}
