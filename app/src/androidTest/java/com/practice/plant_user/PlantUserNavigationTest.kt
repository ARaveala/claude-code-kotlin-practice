package com.practice.plant_user

import android.content.Context
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.practice.plant_user.data.AreaEntity
import com.practice.plant_user.data.GardenDatabase
import com.practice.plant_user.ui.PlantUserApp
import com.practice.plant_user.ui.theme.Plant_userTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantUserAppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: GardenDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In-memory: no file on disk, fresh and empty for every test.
        db = Room.inMemoryDatabaseBuilder<GardenDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
        runBlocking { db.areaDao().insert(AreaEntity(1L, "Test area")) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun launchApp(): StateRestorationTester {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            Plant_userTheme { PlantUserApp(areaDao = db.areaDao()) }
        }
        return restorationTester
    }
    //debug only
    private fun dumpLabels(tag: String = "NavTest") {
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
            .forEach { node ->
                Log.d(tag, "text: " + node.config[SemanticsProperties.Text].joinToString { it.text })
            }
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .fetchSemanticsNodes()
            .forEach { node ->
                Log.d(tag, "desc: " + node.config[SemanticsProperties.ContentDescription].joinToString())
            }
    }
    @Test
    // Create test area, creat GrowZone, click/enter GrowZone , rotate.
    fun stateRestore_rotate_onGrowZone() {
        val restorationTester = launchApp()

        // Room emits on its own thread, so wait for the list to actually load.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Test area").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Test area").performClick()
        composeRule.onNodeWithContentDescription("Add GrowZone").assertIsDisplayed()   // on canvas

        restorationTester.emulateSavedInstanceStateRestore()                           // "rotation"
        // Add GrowZone button expected to exist on success
        composeRule.onNodeWithContentDescription("Add GrowZone").assertExists(
            "Canvas lost after rotation: back stack did not survive state restore: After enter GrowZone")   // still on canvas
    }
    @Test
    // In AreaList dialog box, keep state on rotate
    fun stateRestore_rotate_addAreaDialog() {
        val restorationTester = launchApp()
        composeRule.onNodeWithContentDescription("Add Area").performClick()
        dumpLabels()
        composeRule.onNodeWithText("Area name").assertExists()
        composeRule.onNode(hasSetTextAction()).performTextInput("area51") // Type into dialog box

        restorationTester.emulateSavedInstanceStateRestore()                           // "rotation"
        composeRule.onNodeWithText("New Area").assertExists("Add area dialog closed after rotate")
        composeRule.onNodeWithText("area51").assertExists("Area Name lost after rotate")

    }

}