package com.practice.plantUser.model

/**
 * Named validation messages (CLAUDE.md: never ad hoc strings). A new validation case means
 * a new constant here. Limits come from their rule files so text and rule cannot drift.
 */
object ValidationMessages {
    const val NESTING_DEPTH_EXCEEDED = "GrowZones can only nest ${GrowZoneLimits.MAX_NESTING_DEPTH} levels deep"
    const val SIZING_BELOW_MINIMUM = "Size must be at least ${GrowZoneLimits.MIN_DIMENSION_CM} cm"
    const val SIZING_CAP_EXCEEDED = "Width and depth can be at most ${GrowZoneLimits.MAX_WIDTH_DEPTH_CM} cm"
    const val SIZING_HEIGHT_CAP_EXCEEDED = "Height can be at most ${GrowZoneLimits.MAX_HEIGHT_CM} cm"
    const val SIZE_NOT_A_NUMBER = "Size must be a finite number"
}
