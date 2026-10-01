package com.practice.plant_user

import android.content.Context
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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

    companion object {
        private const val TEST_AREA = "Test area"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In-memory: no file on disk, fresh and empty for every test.
        db = Room.inMemoryDatabaseBuilder<GardenDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
        runBlocking { db.areaDao().insert(AreaEntity(1L, TEST_AREA)) }
    }

    // cleanup may need to include separately , popups, drop down menus, bottom sheets
    @After
    fun tearDown() {
        val cancelButtons = composeRule.onAllNodesWithText("Cancel")
        if (cancelButtons.fetchSemanticsNodes().isNotEmpty()) {
            cancelButtons[0].performClick()
            composeRule.waitForIdle()
        }
        db.close()
    }

    private fun launchApp(): StateRestorationTester {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            Plant_userTheme { PlantUserApp(areaDao = db.areaDao()) }
        }
        return restorationTester
    }

    // Room emits on its own thread, so wait for the list to actually load.
    private fun waitForAreaList() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(TEST_AREA).fetchSemanticsNodes().isNotEmpty()
        }
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
    // In AreaList dialog box, keep state on rotate
    fun stateRestore_rotate_addAreaDialog() {
        val restorationTester = launchApp()
        composeRule.onNodeWithContentDescription("Add Area").performClick()
        composeRule.onNodeWithText("Area name").assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("Area name"))
            .performTextInput("area51") // Type into dialog box
        dumpLabels()

        restorationTester.emulateSavedInstanceStateRestore()                           // "rotation"

        composeRule.onNodeWithText("New Area").assertExists("Add area dialog closed after rotate")
        composeRule.onNodeWithText("area51").assertExists("Area Name lost after rotate")

    }

    @Test
    // Create test area, rotate on canvas.
    fun stateRestore_rotate_onCanvas() {
        val restorationTester = launchApp()

        waitForAreaList()

        composeRule.onNodeWithText(TEST_AREA).performClick()
        composeRule.onNodeWithContentDescription("Add GrowZone").assertIsDisplayed()   // on canvas

        restorationTester.emulateSavedInstanceStateRestore()                           // "rotation"
        // Add GrowZone button expected to exist on success
        composeRule.onNodeWithContentDescription("Add GrowZone").assertExists(
            "Canvas lost after rotation: back stack did not survive state restore: After enter GrowZone")   // still on canvas
    }

    @Test
    // In Canvas when adding new GrowZone
    fun stateRestore_rotate_addGrowZone() {
        val restorationTester = launchApp()
        waitForAreaList()

        composeRule.onNodeWithText(TEST_AREA).performClick()
        composeRule.onNodeWithContentDescription("Add GrowZone").performClick()   // on canvas
        composeRule.onNodeWithText("New GrowZone").assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("Zone name"))
            .performTextInput("Test1")
        dumpLabels()
        restorationTester.emulateSavedInstanceStateRestore()                           // "rotation"
        composeRule.onNodeWithText("New GrowZone").assertExists("Add GrowZone dialog box closed on rotate")
        composeRule.onNodeWithText("Test1").assertExists("GrowZone name was lost on rotate")
    }



}