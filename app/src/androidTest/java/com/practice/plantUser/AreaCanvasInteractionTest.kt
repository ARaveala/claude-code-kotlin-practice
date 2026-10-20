package com.practice.plantUser

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.practice.plantUser.data.AreaEntity
import com.practice.plantUser.data.GardenDatabase
import com.practice.plantUser.data.GrowZoneEntity
import com.practice.plantUser.model.GrowZoneType
import com.practice.plantUser.model.ValidationMessages
import com.practice.plantUser.ui.AREA_CANVAS_TEST_TAG
import com.practice.plantUser.ui.PlantUserApp
import com.practice.plantUser.ui.theme.PlantUserTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 3a canvas interaction: tap selects, action bar (Enter / Edit size), name box breadcrumb,
 * inline edit panel with soft warning, back order, rotation.
 *
 * Geometry: at the default transform (scale 1, no pan) 1 cm = 1 dp from the canvas's top-left.
 * BED sits at 200..400 x 100..300 cm, clear of the FABs on the left edge's vertical centre.
 *
 * Side effect of test-first: while the 3a UI doesn't exist, the "does not exist" checks pass
 * vacuously, `panStartingOnZone_doesNotSelect` is green before the action bar exists.
 * Ghost preview drawing is not checked here (it's pixels on a Canvas): manual check M2.
 */
@RunWith(AndroidJUnit4::class)
class AreaCanvasInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var db: GardenDatabase
    private var bedId = 0L

    companion object {
        private const val AREA_ID = 1L
        private const val TEST_AREA = "Test area"
        private const val BED = "Bed"
        private const val POT = "Pot"
        private const val BED_SIZE_CM = 200.0
        private const val WHERE = "AreaCanvasScreen (ui/AreaCanvasScreen.kt)"
        private const val BREADCRUMB = "ZoneBreadcrumb (ui/ZoneControls.kt)"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder<GardenDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        runBlocking {
            db.areaDao().insert(AreaEntity(AREA_ID, TEST_AREA))
            bedId =
                db.growZoneDao().insert(
                    GrowZoneEntity(
                        areaId = AREA_ID,
                        parentGrowZoneId = null,
                        type = GrowZoneType.PLOT,
                        name = BED,
                        widthCm = BED_SIZE_CM,
                        depthCm = BED_SIZE_CM,
                        heightCm = null,
                        xCm = 200.0,
                        yCm = 100.0
                    )
                )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- helpers ----

    private fun openCanvas(): StateRestorationTester {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            PlantUserTheme { PlantUserApp(areaDao = db.areaDao(), growZoneDao = db.growZoneDao()) }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(TEST_AREA).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(TEST_AREA).performClick()
        // Zones arrive from Room asynchronously; wait until the canvas would have them drawn.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription("Add GrowZone")).fetchSemanticsNodes().isNotEmpty() &&
                runBlocking { db.growZoneDao().getByArea(AREA_ID).first().isNotEmpty() }
        }
        composeRule.waitForIdle()
        return restorationTester
    }

    /** Taps the canvas at a point in cm, valid only at the default transform (1 cm = 1 dp). */
    private fun tapCanvasAtCm(xCm: Float, yCm: Float) {
        val (xPx, yPx) = with(composeRule.density) { Pair(xCm.dp.toPx(), yCm.dp.toPx()) }
        // A tap outside the canvas is silently lost, which reads like an app bug; fail as a test bug instead.
        val canvasPx = composeRule.onNodeWithTag(AREA_CANVAS_TEST_TAG).fetchSemanticsNode().size
        if (xPx >= canvasPx.width || yPx >= canvasPx.height) {
            throw AssertionError(
                "test geometry: tapCanvasAtCm($xCm, $yCm) = ($xPx, $yPx) px is outside the canvas " +
                    "(${canvasPx.width}x${canvasPx.height} px)"
            )
        }
        composeRule.onNodeWithTag(AREA_CANVAS_TEST_TAG).performTouchInput { click(Offset(xPx, yPx)) }
        composeRule.waitForIdle()
    }

    private fun tapBed() = tapCanvasAtCm(300f, 200f)

    private fun assertShown(text: String, msg: String) = composeRule.onNodeWithText(text).assertExists("$msg [$WHERE]")

    // assertDoesNotExist() has no message overload, so check the node list directly.
    private fun assertGone(text: String, msg: String) {
        if (composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) {
            throw AssertionError("$msg [$WHERE]")
        }
    }

    private fun typeInto(label: String, text: String) =
        composeRule.onNode(hasSetTextAction() and hasText(label)).performTextReplacement(text)

    private fun storedZones(): List<GrowZoneEntity> = runBlocking { db.growZoneDao().getByArea(AREA_ID).first() }

    private fun storedBed(): GrowZoneEntity = storedZones().first { it.id == bedId }

    /** Room writes on its own thread: poll until [condition] holds or time out, then the caller's
     * exact assert reports the actual stored value. */
    private fun awaitStored(condition: () -> Boolean) {
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) { condition() }
        } catch (_: ComposeTimeoutException) {
            // Fall through: the exact assertEquals that follows names the actual value.
        }
    }

    /** POT nested in BED at local (20, 20), 50 x 50: absolute 220..270 x 120..170 cm. */
    private fun insertPotInBed() {
        runBlocking {
            db.growZoneDao().insert(
                GrowZoneEntity(
                    areaId = AREA_ID,
                    parentGrowZoneId = bedId,
                    type = GrowZoneType.BOX,
                    name = POT,
                    widthCm = 50.0,
                    depthCm = 50.0,
                    heightCm = null,
                    xCm = 20.0,
                    yCm = 20.0
                )
            )
        }
    }

    private fun openEditPanel() {
        tapBed()
        composeRule.onNodeWithText("Edit size").performClick()
    }

    // ---- selection and action bar ----

    @Test
    fun tapZone_selectsAndShowsActionBar() {
        openCanvas()
        tapBed()
        assertShown("Edit size", "tap on a zone did not show the action bar")
        assertShown("Enter", "action bar has no Enter button")
        composeRule
            .onNode(hasText(BED) and isSelectable())
            .assertIsSelected()
    }

    @Test
    fun panStartingOnZone_doesNotSelect() {
        openCanvas()
        val (startX, startY, endX) = with(composeRule.density) { Triple(300.dp.toPx(), 200.dp.toPx(), 100.dp.toPx()) }
        composeRule.onNodeWithTag(AREA_CANVAS_TEST_TAG).performTouchInput {
            swipe(start = Offset(startX, startY), end = Offset(endX, startY), durationMillis = 300)
        }
        composeRule.waitForIdle()
        assertGone("Edit size", "a pan that started on a zone selected it")
    }

    @Test
    fun tapEmptyCanvas_clearsSelection() {
        openCanvas()
        tapBed()
        assertShown("Edit size", "setup: tap did not select")
        // Must stay on screen: 1 cm = 1 dp here and a Pixel 8 is ~412 dp wide (600 cm missed the canvas).
        tapCanvasAtCm(100f, 50f) // left of and above BED, inside no zone
        assertGone("Edit size", "tap on empty canvas did not clear the selection")
    }

    @Test
    fun enter_zoomsIntoSelectedZone() {
        openCanvas()
        tapBed()
        composeRule.onNodeWithText("Enter").performClick()
        composeRule.onNodeWithContentDescription("Zoom out").assertExists("Enter did not focus the zone [$WHERE]")
        assertGone("Edit size", "selection kept after entering the zone")
    }

    @Test
    fun breadcrumb_clickSelectsZone() {
        openCanvas()
        tapBed()
        composeRule.onNodeWithText("Enter").performClick()
        assertGone("Edit size", "setup: selection kept after entering")
        // Inside BED: the breadcrumb shows it; clicking selects it (focused zones can't be tapped on canvas).
        composeRule.onNode(hasText(BED) and isSelectable()).performClick()
        composeRule.onNode(hasText(BED) and isSelectable()).assertIsSelected()
        assertShown("Edit size", "breadcrumb click did not select the zone")
    }

    @Test
    fun breadcrumb_selectedNestedZone_showsFullPath() {
        insertPotInBed()
        openCanvas()
        tapCanvasAtCm(245f, 145f) // inside POT (and BED); smallest wins

        // Expected name box: Test area › Bed › Pot, with Pot highlighted.
        composeRule
            .onNode(hasText(POT) and isSelectable())
            .assertExists("setup: tap inside POT did not select it [$WHERE]")
            .assertIsSelected()
        composeRule
            .onNode(hasText(BED) and isSelectable())
            .assertExists("breadcrumb is missing POT's parent BED, expected the full path [$BREADCRUMB]")
            .assertIsNotSelected()
    }

    // ---- edit size panel ----

    @Test
    fun editSize_confirm_appliesExactDouble() {
        openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "353.3")
        typeInto("Depth (cm)", "231.1")
        typeInto("Height (cm, optional)", "12.5")
        composeRule.onNodeWithText("Save").performClick()

        awaitStored { storedBed().widthCm == 353.3 }
        val bed = storedBed()
        // Delta 0.0: exact Double equality, a Float round trip would store 353.29998...
        assertEquals("stored widthCm after Save [$WHERE -> resizeGrowZone]", 353.3, bed.widthCm, 0.0)
        assertEquals("stored depthCm after Save [$WHERE -> resizeGrowZone]", 231.1, bed.depthCm, 0.0)
        assertEquals("stored heightCm after Save [$WHERE -> resizeGrowZone]", 12.5, bed.heightCm!!, 0.0)
        assertEquals("xCm moved on resize, top-left must stay fixed", 200.0, bed.xCm, 0.0)
        assertEquals("yCm moved on resize, top-left must stay fixed", 100.0, bed.yCm, 0.0)
    }

    @Test
    fun editSize_rejectedValue_showsMessageAndDisablesSave() {
        openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "0.5")
        assertShown(ValidationMessages.SIZING_BELOW_MINIMUM, "width 0.5 showed no rejection message")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun editSize_cancelPanel_discardsDraft() {
        openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "300")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertGone("Save", "Cancel did not close the edit panel")
        assertEquals("stored widthCm after panel Cancel", BED_SIZE_CM, storedBed().widthCm, 0.0)
    }

    @Test
    fun editSize_softWarning_cancel_leavesSizeUnchanged() {
        openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "6000")
        composeRule.onNodeWithText("Save").performClick()
        assertShown(ValidationMessages.sizeSoftWarning(6000.0), "width 6000 did not show the soft warning")

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertEquals("stored widthCm after warning Cancel", BED_SIZE_CM, storedBed().widthCm, 0.0)
        assertShown("Save", "warning Cancel should return to editing, panel closed instead")
        assertShown("6000", "warning Cancel discarded the draft")
    }

    @Test
    fun editSize_softWarning_accept_applies() {
        openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "6000")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Accept").performClick()

        awaitStored { storedBed().widthCm == 6000.0 }
        assertEquals("stored widthCm after warning Accept [$WHERE -> resizeGrowZone]", 6000.0, storedBed().widthCm, 0.0)
    }

    // ---- create dialog shares the soft warning ----

    @Test
    fun addGrowZone_softWarning_acceptCreates() {
        openCanvas()
        composeRule.onNodeWithContentDescription("Add GrowZone").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("Zone name")).performTextInput("Field")
        typeInto("Width (cm)", "6000")
        composeRule.onNodeWithText("Create").performClick()
        assertShown(ValidationMessages.sizeSoftWarning(6000.0), "create dialog skipped the soft warning")
        assertNull("zone created before Accept", storedZones().firstOrNull { it.name == "Field" })

        composeRule.onNodeWithText("Accept").performClick()
        awaitStored { storedZones().any { it.name == "Field" } }
        assertEquals(
            "stored widthCm of created zone after Accept [AddGrowZoneDialog -> addGrowZone]",
            6000.0,
            storedZones().first { it.name == "Field" }.widthCm,
            0.0
        )
    }

    // ---- back order (decisions.md 2026-10-17) ----

    @Test
    fun toolbarBack_clearsSelectionThenZoomsOutThenLeavesArea() {
        // Same order as system back: the top-left arrow and the system gesture must not disagree.
        openCanvas()
        tapBed()
        composeRule.onNodeWithText("Enter").performClick()
        composeRule.onNode(hasText(BED) and isSelectable()).performClick() // select while focused

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription("Zoom out")
            .assertExists("toolbar back 1 left the Area instead of only clearing the selection [$WHERE]")
        assertGone("Edit size", "toolbar back 1 did not clear the selection")

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription("Add GrowZone")
            .assertExists("toolbar back 2 left the Area instead of zooming out [$WHERE]")
        composeRule.onNodeWithContentDescription("Zoom out").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription("Add Area")
            .assertExists("toolbar back 3 did not return to the Area list [$WHERE]")
    }

    @Test
    fun back_clearsSelectionThenZoomsOutThenLeavesArea() {
        openCanvas()
        tapBed()
        composeRule.onNodeWithText("Enter").performClick()
        composeRule.onNode(hasText(BED) and isSelectable()).performClick() // select while focused

        Espresso.pressBack()
        composeRule.waitForIdle()
        assertGone("Edit size", "back 1 did not clear the selection")
        composeRule
            .onNodeWithContentDescription("Zoom out")
            .assertExists("back 1 zoomed out instead of only clearing [$WHERE]")

        Espresso.pressBack()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription("Add GrowZone")
            .assertExists("back 2 left the Area instead of zooming out [$WHERE]")
        composeRule.onNodeWithContentDescription("Zoom out").assertDoesNotExist()

        Espresso.pressBack()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription("Add Area")
            .assertExists("back 3 did not return to the Area list [$WHERE]")
    }

    // ---- rotation ----

    @Test
    fun stateRestore_rotate_editPanel() {
        val restorationTester = openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "321.5")

        restorationTester.emulateSavedInstanceStateRestore() // "rotation"

        assertShown("Save", "edit panel closed on rotate")
        assertShown("321.5", "edit panel draft lost on rotate")
        composeRule.onNode(hasText(BED) and isSelectable()).assertIsSelected()
    }

    @Test
    fun stateRestore_rotate_softWarningShowing() {
        val restorationTester = openCanvas()
        openEditPanel()
        typeInto("Width (cm)", "6000")
        composeRule.onNodeWithText("Save").performClick()

        restorationTester.emulateSavedInstanceStateRestore() // "rotation"

        assertShown(ValidationMessages.sizeSoftWarning(6000.0), "soft warning lost on rotate")
        assertShown("Accept", "soft warning Accept button lost on rotate")
    }
}
