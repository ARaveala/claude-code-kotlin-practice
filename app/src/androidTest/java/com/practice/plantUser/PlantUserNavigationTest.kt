package com.practice.plantUser

import android.content.Context
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.practice.plantUser.data.AreaEntity
import com.practice.plantUser.data.GardenDatabase
import com.practice.plantUser.ui.PlantUserApp
import com.practice.plantUser.ui.theme.PlantUserTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantUserNavigationTest {
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
        db =
            Room
                .inMemoryDatabaseBuilder<GardenDatabase>(context)
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
            PlantUserTheme { PlantUserApp(areaDao = db.areaDao(), growZoneDao = db.growZoneDao()) }
        }
        return restorationTester
    }

    // Room emits on its own thread, so wait for the list to actually load.
    private fun waitForAreaList() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(TEST_AREA).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private val failures = mutableListOf<String>()

    /*Example of original
    private fun collectFailure(nodeWithText: String, msg: String) {
        try {
            composeRule.onNodeWithText(nodeWithText).assertExists(msg)
        } catch (e: AssertionError) {
            failures += msg
            Log.e("RestoreTest:", msg)
        }
    }*/
    private fun assertNoFailures() {
        if (failures.isNotEmpty()) {
            throw AssertionError(
                "${failures.size} restore checks failed:\n" +
                    failures.joinToString("\n") { "- $it" }
            )
        }
    }
    private fun collectFailure(
        msg: String,
        matcher: SemanticsMatcher,
        assertion: SemanticsNodeInteraction.() -> Unit = { assertExists() }
    ) {
        try {
            composeRule.onNode(matcher).assertion()
        } catch (e: AssertionError) {
            failures += msg
            Log.e("RestoreTest:", msg)
        }
    }

    // overrides
    private fun collectText(text: String, msg: String) = collectFailure(msg, hasText(text))

    // Syntax errors will go when these functions are used for other tests
    private fun collectSelected(label: String, msg: String) =
        collectFailure(msg, hasText(label) and isSelectable()) { assertIsSelected() }

    private fun collectNotSelected(label: String, msg: String) =
        collectFailure(msg, hasText(label) and isSelectable()) { assertIsNotSelected() }

    // debug only
    private fun dumpLabels(tag: String = "NavTest") {
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
            .forEach { node ->
                Log.d(tag, "text: " + node.config[SemanticsProperties.Text].joinToString { it.text })
            }
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .fetchSemanticsNodes()
            .forEach { node ->
                Log.d(tag, "desc: " + node.config[SemanticsProperties.ContentDescription].joinToString())
            }
    }

    // In AreaList dialog box, keep state on rotate
    @Test
    fun stateRestore_rotate_addAreaDialog() {
        val restorationTester = launchApp()
        composeRule.onNodeWithContentDescription("Add Area").performClick()
        composeRule.onNodeWithText("Area name").assertExists()
        composeRule
            .onNode(hasSetTextAction() and hasText("Area name"))
            .performTextInput("area51") // Type into dialog box
        dumpLabels()

        restorationTester.emulateSavedInstanceStateRestore() // "rotation"

        composeRule.onNodeWithText("New Area").assertExists("Add area dialog closed after rotate")
        composeRule.onNodeWithText("area51").assertExists("Area Name lost after rotate")
    }

    // Create test area, rotate on canvas.
    @Test
    fun stateRestore_rotate_onCanvas() {
        val restorationTester = launchApp()

        waitForAreaList()

        composeRule.onNodeWithText(TEST_AREA).performClick()
        composeRule.onNodeWithContentDescription("Add GrowZone").assertIsDisplayed() // on canvas

        restorationTester.emulateSavedInstanceStateRestore() // "rotation"
        // Add GrowZone button expected to exist on success
        composeRule.onNodeWithContentDescription("Add GrowZone").assertExists(
            "Canvas lost after rotation: back stack did not survive state restore"
        ) // still on canvas
    }

    // In Canvas when adding new GrowZone
    @Test
    fun stateRestore_rotate_addGrowZone() {
        val restorationTester = launchApp()
        waitForAreaList()

        composeRule.onNodeWithText(TEST_AREA).performClick()
        composeRule.onNodeWithContentDescription("Add GrowZone").performClick() // on canvas
        composeRule.onNodeWithText("New GrowZone").assertExists()
        composeRule
            .onNode(hasSetTextAction() and hasText("Zone name"))
            .performTextInput("Test1")
        composeRule
            .onNode(hasSetTextAction() and hasText("Width (cm)"))
            .performTextReplacement("230")
        composeRule
            .onNode(hasSetTextAction() and hasText("Depth (cm)"))
            .performTextReplacement("231.1")
        composeRule
            .onNode(hasSetTextAction() and hasText("Height (cm, optional)"))
            .performTextReplacement("232.2")
        composeRule.onNodeWithText("Plot").performClick()
        composeRule.onNode(hasText("Plot") and isSelectable()).assertIsSelected() // setup sanity check, fail fast
        dumpLabels()
        restorationTester.emulateSavedInstanceStateRestore() // "rotation"
        collectText("New GrowZone", "Add GrowZone dialog box closed on rotate")
        collectText("Test1", "GrowZone name was lost on rotate")
        collectText("230", "GrowZone width was lost on rotate")
        collectText("231.1", "GrowZone Depth was lost on rotate")
        collectText("232.2", "GrowZone Height was lost on rotate")
        collectSelected("Plot", "GrowZone type was lost on rotate (Plot not selected)")
        collectNotSelected("Greenhouse", "GrowZone type reset to default on rotate")
        assertNoFailures()
    }
}
