package com.practice.plant_user.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.practice.plant_user.data.AreaDao
import com.practice.plant_user.ui.navigation.AreaCanvasKey
import com.practice.plant_user.ui.navigation.AreaListKey
import com.practice.plant_user.ui.navigation.navConfig
import com.practice.plant_user.viewmodel.AreaViewModel

@Composable
fun PlantUserApp(areaDao: AreaDao) {

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
        entryProvider = entryProvider {
            entry<AreaListKey> {
                AreaListScreen(
                    areas = areas,
                    onAddArea = { name -> areaViewModel.addArea(name) },
                    onAreaClick = { clicked -> backStack.add(AreaCanvasKey(clicked.id)) },  // push
                    modifier = Modifier.fillMaxSize(),
                )
            }
            entry<AreaCanvasKey> { key ->
                val area = areas.firstOrNull { it.id == key.areaId }
                if (area != null) {
                    AreaCanvasScreen(
                        area = area,
                        onBack = { backStack.removeLastOrNull() },   // toolbar arrow: pop
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // null: list not loaded yet (after process death) or area deleted. Shows nothing for now.
            }
        },
    )
    //val area = selectedArea
    //if (area == null) {
    //    AreaListScreen(
    //        areas = areas,
    //        onAddArea = { name -> areaViewModel.addArea(name) },
    //        onAreaClick = { clicked -> selectedArea = clicked },
    //        modifier = Modifier.fillMaxSize(),
    //        )
    //} else {
    //    AreaCanvasScreen(
    //        area = area,
    //        onBack = { selectedArea = null },
    //        modifier = Modifier.fillMaxSize(),
    //        )
    //}
}