package com.practice.plant_user.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.plant_user.data.GrowZoneDao
import com.practice.plant_user.data.GrowZoneEntity
import com.practice.plant_user.data.GrowZoneType
import com.practice.plant_user.ui.GrowZone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val GROW_ZONE_GAP_CM = 50.0
private const val NESTED_ZONE_PADDING_CM = 20.0

// Flat cap, not type-specific — see domain_model.md's Nesting Rules for why type-pairing
// restrictions were dropped. A top-level zone is depth 1, so this allows 2 levels of nesting.
private const val MAX_NESTING_DEPTH = 3

/** Placeholder auto-layout (Phase 2): next top-level zone goes to the right of the current
 * rightmost zone's edge, plus a fixed gap. No drag-to-place UI exists yet (Phase 5). */
fun nextTopLevelXCm(existingZones: List<GrowZone>, gapCm: Double = GROW_ZONE_GAP_CM): Double {
    if (existingZones.isEmpty()) return 0.0
    val rightmostEdge = existingZones.maxOf { it.xCm + it.widthCm }
    return rightmostEdge + gapCm
}

/** Same stacking idea as [nextTopLevelXCm], scoped to siblings under the same parent zone instead
 * of an Area's top level, and inset from the parent's corner instead of starting flush at zero. */
fun nextNestedOffsetCm(existingSiblings: List<GrowZone>, paddingCm: Double = NESTED_ZONE_PADDING_CM): Offset {
    if (existingSiblings.isEmpty()) return Offset(paddingCm.toFloat(), paddingCm.toFloat())
    val rightmostEdge = existingSiblings.maxOf { it.xCm + it.widthCm }
    return Offset((rightmostEdge + paddingCm).toFloat(), paddingCm.toFloat())
}

/** [zone]'s depth in its nesting chain, counting itself — a top-level zone (no parent) is 1. */
private fun nestingDepth(zone: GrowZone, allZones: List<GrowZone>): Int {
    var depth = 1
    var parentId = zone.parentGrowZoneId
    while (parentId != null) {
        val parent = allZones.firstOrNull { it.id == parentId } ?: break
        depth++
        parentId = parent.parentGrowZoneId
    }
    return depth
}

/**
 * Why a new zone can't nest inside [parent], or null if it's fine. No type-pairing restrictions
 * (domain_model.md's Nesting Rules explains why — they fought real use cases like a Box overlapping
 * a Plot, and would need to scale to user-defined zone types later). Only a flat depth cap remains;
 * size/fit is deferred to Phase 3's real `bounds_enforced` containment, not checked here.
 * [parent] null means top-level in the Area, always valid.
 */
fun nestingRejectionReason(parent: GrowZone?, allZones: List<GrowZone>): String? {
    if (parent == null) return null
    if (nestingDepth(parent, allZones) + 1 > MAX_NESTING_DEPTH) {
        return "GrowZones can only nest $MAX_NESTING_DEPTH levels deep"
    }
    return null
}

fun canNestGrowZone(parent: GrowZone?, allZones: List<GrowZone>): Boolean =
    nestingRejectionReason(parent, allZones) == null

class GrowZoneViewModel(private val growZoneDao: GrowZoneDao, private val areaId: Long) : ViewModel() {
    val growZones: StateFlow<List<GrowZone>> = growZoneDao.getByArea(areaId)
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
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            val (xCm, yCm) = if (parentGrowZoneId == null) {
                nextTopLevelXCm(siblings) to 0.0
            } else {
                val offset = nextNestedOffsetCm(siblings)
                offset.x.toDouble() to offset.y.toDouble()
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
                )
            )
        }
    }
}
