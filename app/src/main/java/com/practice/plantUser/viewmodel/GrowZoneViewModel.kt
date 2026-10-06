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
import com.practice.plantUser.model.nextNestedPositionCm
import com.practice.plantUser.model.nextTopLevelXCm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GrowZoneViewModel(
    private val growZoneDao: GrowZoneDao,
    private val areaId: Long,
) : ViewModel() {
    init {
        Log.d("DEBUG::GrowZoneViewModel", "GrowZoneViewModel being created for areaId=$areaId")
    }

    // debugging
    override fun onCleared() {
        Log.d("DEBUG::GrowZoneViewModel", "cleared for areaId=$areaId")
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
                        yCm = it.yCm,
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
        parentGrowZoneId: Long?,
    ) {
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
                    yCm = yCm,
                ),
            )
        }
    }
}
