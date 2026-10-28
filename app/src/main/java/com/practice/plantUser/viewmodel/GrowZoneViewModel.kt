package com.practice.plantUser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.plantUser.data.GrowZoneDao
import com.practice.plantUser.data.GrowZoneEntity
import com.practice.plantUser.data.toModel
import com.practice.plantUser.model.Anchor
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.GrowZoneType
import com.practice.plantUser.model.PositionCm
import com.practice.plantUser.model.canNestGrowZone
import com.practice.plantUser.model.heightRejectionReason
import com.practice.plantUser.model.nextNestedPositionCm
import com.practice.plantUser.model.nextTopLevelXCm
import com.practice.plantUser.model.resize
import com.practice.plantUser.model.widthDepthRejectionReason
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "GrowZoneViewModel"

class GrowZoneViewModel(private val growZoneDao: GrowZoneDao, private val areaId: Long) : ViewModel() {
    init {
        Log.d(TAG, "GrowZoneViewModel being created for areaId=$areaId")
    }

    // debugging
    override fun onCleared() {
        Log.d(TAG, "cleared for areaId=$areaId")
    }

    val growZones: StateFlow<List<GrowZone>> =
        growZoneDao
            .getByArea(areaId)
            .map { entities -> entities.map { it.toModel() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [parentGrowZoneId] is whichever zone the canvas is currently focused/zoomed into (null at
     * the Area's top level) — nesting comes entirely from "where you are," no picker needed. */
    fun addGrowZone(
        name: String,
        type: GrowZoneType,
        widthCm: Double,
        depthCm: Double,
        heightCm: Double?,
        parentGrowZoneId: Long?
    ) {
        val sizeReason =
            widthDepthRejectionReason(widthCm)
                ?: widthDepthRejectionReason(depthCm)
                ?: heightRejectionReason(heightCm)
        if (sizeReason != null) {
            Log.e(
                TAG,
                "addGrowZone: rejected name='$name' widthCm=$widthCm depthCm=$depthCm heightCm=$heightCm " +
                    "parentGrowZoneId=$parentGrowZoneId areaId=$areaId reason=\"$sizeReason\""
            )
            return
        }
        val parentZone = growZones.value.firstOrNull { it.id == parentGrowZoneId }
        if (!canNestGrowZone(parentZone, growZones.value)) return

        viewModelScope.launch {
            val siblings = growZones.value.filter { it.parentGrowZoneId == parentGrowZoneId }
            val (xCm, yCm) =
                if (parentGrowZoneId == null) {
                    PositionCm(nextTopLevelXCm(siblings), 0.0)
                } else {
                    nextNestedPositionCm(siblings)
                }
            growZoneDao.insert(
                GrowZoneEntity(
                    areaId = areaId,
                    parentGrowZoneId = parentGrowZoneId,
                    type = type,
                    name = name,
                    widthCm = widthCm,
                    depthCm = depthCm,
                    heightCm = heightCm,
                    xCm = xCm,
                    yCm = yCm
                )
            )
        }
    }

    /** Applies a confirmed size from the edit panel. The panel already validated and the user already
     * accepted any soft warning; this re-checks the hard limits (untrusted input) and writes nothing
     * if any fails. The [anchor] corner stays fixed: the new position comes from [resize], the same
     * function the ghost preview uses, so the stored zone lands where the ghost was. */
    fun resizeGrowZone(id: Long, widthCm: Double, depthCm: Double, heightCm: Double?, anchor: Anchor) {
        val reason =
            widthDepthRejectionReason(widthCm)
                ?: widthDepthRejectionReason(depthCm)
                ?: heightRejectionReason(heightCm)
        if (reason != null) {
            Log.e(
                TAG,
                "resizeGrowZone: rejected zoneId=$id widthCm=$widthCm depthCm=$depthCm " +
                    "heightCm=$heightCm areaId=$areaId reason=\"$reason\""
            )
            return
        }
        viewModelScope.launch {
            // Resize from the stored row, not the UI's copy, so the position is computed from what's in the DB.
            val before = growZoneDao.getById(id)?.toModel()
            if (before == null) {
                Log.e(TAG, "resizeGrowZone: zoneId=$id not found in areaId=$areaId, nothing written")
                return@launch
            }
            val after = resize(before, widthCm, depthCm, anchor)
            // How far the parent's origin moved; children get the opposite shift (zero for TOP_LEFT).
            val dxCm = after.xCm - before.xCm
            val dyCm = after.yCm - before.yCm
            val rows =
                growZoneDao.resizeKeepingChildrenInPlace(
                    id,
                    widthCm,
                    depthCm,
                    heightCm,
                    after.xCm,
                    after.yCm,
                    dxCm,
                    dyCm
                )
            if (rows == 0) {
                Log.e(TAG, "resizeGrowZone: no zone updated, zoneId=$id not found in areaId=$areaId")
            } else {
                Log.d(
                    TAG,
                    "resizeGrowZone: zoneId=$id -> ${widthCm}x$depthCm cm at (${after.xCm}, ${after.yCm}), " +
                        "heightCm=$heightCm anchor=$anchor, children shifted by (${-dxCm}, ${-dyCm})"
                )
            }
        }
    }
}
