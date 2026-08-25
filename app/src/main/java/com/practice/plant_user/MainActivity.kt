package com.practice.plant_user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.practice.plant_user.ui.Area
import com.practice.plant_user.ui.AreaCanvasScreen
import com.practice.plant_user.ui.AreaListScreen
import com.practice.plant_user.ui.theme.Plant_userTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Plant_userTheme {
                val areas = remember { mutableStateListOf<Area>() }
                var nextId by remember { mutableIntStateOf(0) }
                var selectedArea by remember { mutableStateOf<Area?>(null) }

                val area = selectedArea
                if (area == null) {
                    AreaListScreen(
                        areas = areas,
                        onAddArea = { name ->
                            areas.add(Area(id = nextId, name = name))
                            nextId++
                        },
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