package com.practice.plantUser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.practice.plantUser.model.GrowZone

private val CRUMB_MAX_WIDTH = 140.dp

/**
 * Name box: Area, then [chain], the full path (root first) to the selected zone, or to the focused
 * zone when nothing is selected. Each zone name selects that zone on click; the selected one is
 * highlighted. The Area name clears the selection. Focusing from here is later (known_issues.md).
 */
@Composable
fun ZoneBreadcrumb(
    areaName: String,
    chain: List<GrowZone>,
    selectedZoneId: Long?,
    onAreaClick: () -> Unit,
    onZoneClick: (GrowZone) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            areaName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = CRUMB_MAX_WIDTH).clickable(onClick = onAreaClick)
        )
        // One click lambda per crumb per breadcrumb recomposition (closure capturing `zone`); only
        // recomposes on focus/selection/zone changes, not per frame.
        for (i in chain.indices) {
            val zone = chain[i]
            Text(" › ")
            Crumb(zone.name, selected = zone.id == selectedZoneId, onClick = { onZoneClick(zone) })
        }
    }
}

@Composable
private fun Crumb(name: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier =
        Modifier
            .widthIn(max = CRUMB_MAX_WIDTH)
            .clip(MaterialTheme.shapes.small)
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Non-modal: a Surface only consumes touches inside its own bounds, the canvas keeps the rest. */
@Composable
fun ZoneActionBar(showEnter: Boolean, onEnter: () -> Unit, onEditSize: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            if (showEnter) {
                TextButton(onClick = onEnter) { Text("Enter") }
            }
            TextButton(onClick = onEditSize) { Text("Edit size") }
        }
    }
}

/**
 * Inline edit size panel, canvas state (decisions.md). With [warningValueCm] set, the soft warning
 * replaces Save/Cancel; the fields stay visible so the user sees the number, editing hides it again.
 */
@Composable
fun EditSizePanel(
    zoneName: String,
    widthText: String,
    depthText: String,
    heightText: String,
    onWidthChange: (String) -> Unit,
    onDepthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    sizeInput: SizeInput,
    warningValueCm: Double?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onAcceptWarning: () -> Unit,
    onCancelWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Resize: $zoneName", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SizeFields(widthText, depthText, heightText, onWidthChange, onDepthChange, onHeightChange, sizeInput)
            Spacer(Modifier.height(8.dp))
            if (warningValueCm != null) {
                SoftWarningMessage(warningValueCm)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (warningValueCm != null) {
                    TextButton(onClick = onCancelWarning) { Text("Cancel") }
                    TextButton(onClick = onAcceptWarning) { Text("Accept") }
                } else {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onSave, enabled = sizeInput.isValid) { Text("Save") }
                }
            }
        }
    }
}
