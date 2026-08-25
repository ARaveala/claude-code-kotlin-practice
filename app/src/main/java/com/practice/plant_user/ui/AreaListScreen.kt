package com.practice.plant_user.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.plant_user.ui.theme.Plant_userTheme

data class Area(val id: Int, val name: String)

private const val MAX_AREA_NAME_LENGTH = 50

// Soft UX cap; real ceiling is Int.MAX_VALUE (2,147,483,647), MainActivity's `nextId` counter is
// a plain Int, so this can be raised well past 100 before hitting an actual overflow risk.
private const val MAX_AREAS = 100

/** Whether another Area can be added given the current count. Boundary logic pulled out of the
 * Compose state closure in [MainActivity] so it's unit testable without instrumentation. */
fun canAddArea(currentCount: Int, max: Int = MAX_AREAS): Boolean = currentCount < max

/** Gates the Area name TextField's input: keeps [current] if [candidate] would exceed [maxLength],
 * otherwise accepts [candidate]. Pulled out of the dialog's onValueChange for the same reason. */
fun coerceAreaNameInput(current: String, candidate: String, maxLength: Int = MAX_AREA_NAME_LENGTH): String =
    if (candidate.length <= maxLength) candidate else current

@Composable
fun AreaListScreen(
    areas: List<Area>,
    onAddArea: (String) -> Unit,
    onAreaClick: (Area) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Area")
            }
        },
    ) { innerPadding ->
        if (areas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No areas yet — tap + to add one",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                items(areas, key = { it.id }) { area ->
                    AreaRow(area = area, onClick = { onAreaClick(area) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddAreaDialog(
            atCapacity = !canAddArea(areas.size),
            onConfirm = { name ->
                onAddArea(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AreaRow(area: Area, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = area.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun AddAreaDialog(atCapacity: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Area") },
        text = {
            if (atCapacity) {
                Text("Area limit reached ($MAX_AREAS).")
            } else {
                TextField(
                    value = name,
                    onValueChange = { input -> name = coerceAreaNameInput(name, input) },
                    singleLine = true,
                    label = { Text("Area name") },
                    supportingText = { Text("${name.length}/$MAX_AREA_NAME_LENGTH") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = !atCapacity && name.isNotBlank(),
            ) {
                Text("Add")
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
private fun AreaListScreenPreview() {
    Plant_userTheme {
        AreaListScreen(
            areas = listOf(Area(1, "Backyard"), Area(2, "Greenhouse")),
            onAddArea = {},
            onAreaClick = {},
        )
    }
}
