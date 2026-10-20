package com.practice.plantUser.model

/**
 * Named validation messages (CLAUDE.md: never ad hoc strings). A new validation case means
 * a new constant here. Limits come from their rule files so text and rule cannot drift.
 */
object ValidationMessages {
    const val NESTING_DEPTH_EXCEEDED = "GrowZones can only nest ${GrowZoneLimits.MAX_NESTING_DEPTH} levels deep"

    // val, not const: built once when the object loads, so formatCm runs once per message.
    val SIZING_BELOW_MINIMUM = "Size must be at least ${formatCm(GrowZoneLimits.MIN_DIMENSION_CM)} cm"
    val SIZING_CAP_EXCEEDED = "Width and depth can be at most ${formatCm(GrowZoneLimits.MAX_WIDTH_DEPTH_CM)} cm"
    val SIZING_HEIGHT_CAP_EXCEEDED = "Height can be at most ${formatCm(GrowZoneLimits.MAX_HEIGHT_CM)} cm"
    const val SIZE_NOT_A_NUMBER = "Size must be a finite number"

    /** Soft warning text, not a rejection. Builds a new String, so callers cache it per value. */
    fun sizeSoftWarning(valueCm: Double): String =
        "That's very large for a single zone (${formatCm(valueCm)} cm), is that correct?"
}

// Beyond this a Double no longer holds every whole number exactly, and toLong() would saturate.
private const val MAX_EXACT_WHOLE_CM = 1e15

/** cm value for user-facing text: whole numbers drop the ".0" (100000.0 -> "100000"). */
fun formatCm(valueCm: Double): String =
    if (valueCm % 1.0 == 0.0 && valueCm in -MAX_EXACT_WHOLE_CM..MAX_EXACT_WHOLE_CM) {
        valueCm.toLong().toString()
    } else {
        valueCm.toString()
    }
