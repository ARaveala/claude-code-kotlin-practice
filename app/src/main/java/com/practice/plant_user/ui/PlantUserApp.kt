package com.practice.plant_user.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.practice.plant_user.data.AreaDao
import com.practice.plant_user.data.GrowZoneDao
import com.practice.plant_user.ui.navigation.AreaCanvasKey
import com.practice.plant_user.ui.navigation.AreaListKey
import com.practice.plant_user.ui.navigation.navConfig
import com.practice.plant_user.viewmodel.AreaViewModel
import com.practice.plant_user.viewmodel.GrowZoneViewModel

@Composable
fun PlantUserApp(areaDao: AreaDao, growZoneDao: GrowZoneDao) {

    val areaViewModel: AreaViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AreaViewModel(areaDao) }
                                   },
        )
    val areas by areaViewModel.areas.collectAsState()
    val backStack = rememberNavBackStack(navConfig, AreaListKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },     // system back / gesture

        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes each screen's ViewModels to its back-stack entry; cleared on pop.
            // Removing this makes ViewModels live for the whole Activity (see decisions.md).
            // Verify with Docs/manual_checks.md M1.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AreaListKey> {  AreaListScreen(
                areas = areas,
                onAddArea = { name -> areaViewModel.addArea(name) },
                onAreaClick = { clicked -> backStack.add(AreaCanvasKey(clicked.id)) },  // push
                modifier = Modifier.fillMaxSize(),
            )}
            entry<AreaCanvasKey> { canvaskey ->
                val area = areas.firstOrNull { it.id == canvaskey.areaId }
                /// viewModel() uses this entry's own ViewModelStore (per-entry decorator),
                // so no viewModel(key = ...) is needed to keep each area's ViewModel separate.
                val growZoneViewModel = viewModel { GrowZoneViewModel(growZoneDao, canvaskey.areaId) }
                val growZones by growZoneViewModel.growZones.collectAsState()
                if (area != null) {
                    AreaCanvasScreen(
                        area = area,
                        growZones = growZones,
                        onAddGrowZone = growZoneViewModel::addGrowZone,
                        onBack = { backStack.removeLastOrNull() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    )
}
