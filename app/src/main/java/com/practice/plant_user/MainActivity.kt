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
import android.util.Log
import com.practice.plant_user.data.GardenDatabase
import com.practice.plant_user.ui.Area
import com.practice.plant_user.ui.AreaCanvasScreen
import com.practice.plant_user.ui.AreaListScreen
import com.practice.plant_user.ui.PlantUserApp
import com.practice.plant_user.ui.theme.Plant_userTheme
import com.practice.plant_user.viewmodel.AreaViewModel

class MainActivity : ComponentActivity() {
    companion object {
        // DEBUGGING
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate: --- entering", Throwable())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val areaDao = GardenDatabase.getInstance(applicationContext).areaDao()

        setContent {
            Plant_userTheme {
                PlantUserApp(areaDao)
            }
        }
    }
}