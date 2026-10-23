package com.practice.plantUser

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

/**
 * known_issues.md: in landscape the add-GrowZone dialog shows its buttons, but the size fields are
 * cut off (width/depth partly, height not at all), whether opened before or after rotating. M3
 * AlertDialog gives its text slot only the height left above the buttons, and our Column hands each
 * child what remains: the fields are squeezed (laid out shorter), not clipped. So the check is on
 * laid-out height, not visible pixels (a squeezed field's visible and layout bounds are equal).
 * Opens the dialog in landscape and types nothing, so no IME.
 *
 * "Fully shown" = at least TextFieldDefaults.MinHeight, the minimum for a single-line labelled
 * TextField. If the content is scrollable, each field is scrolled into view first: it checks
 * "reachable at full size", not how it's fixed.
 */
@RunWith(AndroidJUnit4::class)
class AddGrowZoneDialogLandscapeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: GardenDatabase

    companion object {
        private const val AREA_ID = 1L
        private const val TEST_AREA = "Test area"
        private const val WHERE = "AddGrowZoneDialog (ui/AreaCanvasScreen.kt)"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder<GardenDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        runBlocking { db.areaDao().insert(AreaEntity(AREA_ID, TEST_AREA)) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun forceLandscape() {
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        // The activity is recreated; composeRule.activity fetches the current one each call.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
    }

    @Test
    fun landscape_sizeFieldsFullyShown() {
        forceLandscape()
        composeRule.setContent {
            PlantUserTheme { PlantUserApp(areaDao = db.areaDao(), growZoneDao = db.growZoneDao()) }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(TEST_AREA).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(TEST_AREA).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription("Add GrowZone")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Add GrowZone").performClick()
        composeRule.waitForIdle()

        assertFieldFullyShown("Width (cm)")
        assertFieldFullyShown("Depth (cm)")
        assertFieldFullyShown("Height (cm, optional)")
    }

    private fun assertFieldFullyShown(label: String) {
        val field = hasSetTextAction() and hasText(label)
        if (composeRule.onAllNodes(field and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNode(field).performScrollTo()
        }
        val node =
            composeRule
                .onNode(field)
                .assertExists("field '$label' missing from the dialog [$WHERE]")
                .fetchSemanticsNode()
        // Layout size, not clipped bounds: the dialog squeezes the fields rather than clipping them.
        val heightPx = node.size.height
        val minHeightPx = with(composeRule.density) { TextFieldDefaults.MinHeight.roundToPx() }
        if (heightPx < minHeightPx) {
            throw AssertionError(
                "landscape, dialog just opened: field '$label' laid out ${heightPx}px tall, below the " +
                    "TextField minimum ${minHeightPx}px, so it's squeezed and not scrollable into view [$WHERE]"
            )
        }
    }
}
