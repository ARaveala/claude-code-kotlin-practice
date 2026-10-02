package com.practice.plant_user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import com.practice.plant_user.data.GardenDatabase
import com.practice.plant_user.ui.PlantUserApp
import com.practice.plant_user.ui.theme.Plant_userTheme

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
            Plant_userTheme {
                PlantUserApp(areaDao = db.areaDao(), growZoneDao = db.growZoneDao())
            }
        }
    }
}
