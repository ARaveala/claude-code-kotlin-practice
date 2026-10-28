package com.practice.plantUser.viewmodel

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.practice.plantUser.data.AreaEntity
import com.practice.plantUser.data.GardenDatabase
import com.practice.plantUser.data.GrowZoneEntity
import com.practice.plantUser.model.Anchor
import com.practice.plantUser.model.GrowZoneType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 3b save path: resizeGrowZone with an anchor, against an in-memory Room DB.
 *
 * Bug (found on the emulator): Save ignored the selected handle and always resized from the
 * top-left, so the stored zone didn't land where the ghost showed it.
 */
@RunWith(AndroidJUnit4::class)
class GrowZoneViewModelResizeTest {
    private lateinit var db: GardenDatabase
    private lateinit var viewModel: GrowZoneViewModel
    private var zoneId = 0L

    companion object {
        private const val AREA_ID = 1L
        private const val TOLERANCE_CM = 1e-9
        private const val WHERE = "GrowZoneViewModel.resizeGrowZone (viewmodel/GrowZoneViewModel.kt)"
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
            db.areaDao().insert(AreaEntity(AREA_ID, "Test area"))
            // x=10, y=20, 100 x 50: bottom-right corner at (110, 70).
            zoneId =
                db.growZoneDao().insert(
                    GrowZoneEntity(
                        areaId = AREA_ID,
                        parentGrowZoneId = null,
                        type = GrowZoneType.PLOT,
                        name = "Bed",
                        widthCm = 100.0,
                        depthCm = 50.0,
                        heightCm = null,
                        xCm = 10.0,
                        yCm = 20.0
                    )
                )
        }
        viewModel = GrowZoneViewModel(db.growZoneDao(), AREA_ID)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Waits until the row with [id] has the new width (written with or without the bug), then returns it. */
    private fun storedAfterResize(id: Long, widthCm: Double): GrowZoneEntity = runBlocking {
        withTimeout(5_000) {
            db.growZoneDao().getByArea(AREA_ID).first { rows -> rows.single { it.id == id }.widthCm == widthCm }
        }.single { it.id == id }
    }

    @Test
    fun saveWithBottomRightAnchor_storesTheGhostPosition() {
        // Same numbers as ResizeTest: anchored bottom-right, 150 x 80 grows left and up to (-40, -10).
        viewModel.resizeGrowZone(zoneId, 150.0, 80.0, null, Anchor.BOTTOM_RIGHT)

        val stored = storedAfterResize(zoneId, 150.0)
        val call = "resizeGrowZone(100x50 at (10, 20) -> 150x80, BOTTOM_RIGHT)"
        assertEquals("$call stored xCm [$WHERE]", -40.0, stored.xCm, TOLERANCE_CM)
        assertEquals("$call stored yCm [$WHERE]", -10.0, stored.yCm, TOLERANCE_CM)
    }

    @Test
    fun saveWithBottomRightAnchor_childStaysPutOnScreen() {
        // Bug (found on the emulator): children are stored relative to the parent, so when a
        // non-top-left resize moves the parent's x/y, the child moved with it on screen.
        // Child at (5, 5) relative = (15, 25) absolute. Parent moves by (-50, -30), so the child's
        // relative position must become (55, 35) to stay at (15, 25).
        val childId = runBlocking {
            db.growZoneDao().insert(
                GrowZoneEntity(
                    areaId = AREA_ID,
                    parentGrowZoneId = zoneId,
                    type = GrowZoneType.BOX,
                    name = "Pot",
                    widthCm = 20.0,
                    depthCm = 10.0,
                    heightCm = null,
                    xCm = 5.0,
                    yCm = 5.0
                )
            )
        }

        viewModel.resizeGrowZone(zoneId, 150.0, 80.0, null, Anchor.BOTTOM_RIGHT)

        val parent = storedAfterResize(zoneId, 150.0)
        val child = runBlocking { db.growZoneDao().getById(childId) }!!
        val call = "resizeGrowZone(parent 100x50 at (10, 20) -> 150x80, BOTTOM_RIGHT), child at (5, 5)"
        assertEquals("$call child absolute xCm [$WHERE]", 15.0, parent.xCm + child.xCm, TOLERANCE_CM)
        assertEquals("$call child absolute yCm [$WHERE]", 25.0, parent.yCm + child.yCm, TOLERANCE_CM)
    }
}
