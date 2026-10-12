package com.practice.plantUser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.practice.plantUser.data.GardenDatabase
import com.practice.plantUser.ui.PlantUserApp
import com.practice.plantUser.ui.theme.PlantUserTheme

class MainActivity : ComponentActivity() {
    companion object {
        // DEBUGGING
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate: --- entering", Throwable())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = GardenDatabase.getInstance(applicationContext)
        setContent {
            PlantUserTheme {
                PlantUserApp(areaDao = db.areaDao(), growZoneDao = db.growZoneDao())
            }
        }
    }
}
