package com.practice.plant_user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.practice.plant_user.data.GardenDatabase
import com.practice.plant_user.ui.Area
import com.practice.plant_user.ui.AreaCanvasScreen
import com.practice.plant_user.ui.AreaListScreen
import com.practice.plant_user.ui.theme.Plant_userTheme
import com.practice.plant_user.viewmodel.AreaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Plant_userTheme {
                val context = LocalContext.current
                val areaViewModel: AreaViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AreaViewModel(GardenDatabase.getInstance(context).areaDao()) }
                    },
                )
                val areas by areaViewModel.areas.collectAsState()
                var selectedArea by remember { mutableStateOf<Area?>(null) }

                val area = selectedArea
                if (area == null) {
                    AreaListScreen(
                        areas = areas,
                        onAddArea = { name -> areaViewModel.addArea(name) },
                        onAreaClick = { clicked -> selectedArea = clicked },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AreaCanvasScreen(
                        area = area,
                        onBack = { selectedArea = null },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}