package com.practice.plantUser.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.plantUser.data.GrowZoneDao
import com.practice.plantUser.data.GrowZoneEntity
import com.practice.plantUser.model.GrowZone
import com.practice.plantUser.model.GrowZoneType
import com.practice.plantUser.model.PositionCm
import com.practice.plantUser.model.canNestGrowZone
import com.practice.plantUser.model.heightRejectionReason
import com.practice.plantUser.model.nextNestedPositionCm
import com.practice.plantUser.model.nextTopLevelXCm
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
            .map { entities ->
                entities.map {
                    GrowZone(
                        id = it.id,
                        parentGrowZoneId = it.parentGrowZoneId,
                        type = it.type,
                        name = it.name,
                        widthCm = it.widthCm,
                        depthCm = it.depthCm,
                        heightCm = it.heightCm,
                        xCm = it.xCm,
                        yCm = it.yCm
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
     * if any fails. Top-left corner stays fixed: position columns are not touched. */
    fun resizeGrowZone(id: Long, widthCm: Double, depthCm: Double, heightCm: Double?) {
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
            val rows = growZoneDao.updateSize(id, widthCm, depthCm, heightCm)
            if (rows == 0) {
                Log.e(TAG, "resizeGrowZone: no zone updated, zoneId=$id not found in areaId=$areaId")
            } else {
                Log.d(TAG, "resizeGrowZone: zoneId=$id -> ${widthCm}x$depthCm cm, heightCm=$heightCm")
            }
        }
    }
}
