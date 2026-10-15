package com.practice.plantUser.model

// model/SizingLimits.kt
object SizingLimits {
    /** All values in cm. can these be separated into specifics, e,g GrowZones own */
    const val MIN_DIMENSION_CM = 1.0
    const val SOFT_WARN_WIDTH_DEPTH_CM = 5_000.0
    const val MAX_WIDTH_DEPTH_CM = 100_000.0
    const val MAX_HEIGHT_CM = 1_000.0
}

/*
* sealed interface SizeCheck {
    data object Ok : SizeCheck
    data class NeedsConfirm(val valueCm: Double) : SizeCheck
    data class Rejected(val reason: String) : SizeCheck
}

fun checkWidthDepthCm(valueCm: Double): SizeCheck = when {
    valueCm.isNaN() || valueCm.isInfinite() -> SizeCheck.Rejected(SIZE_NOT_A_NUMBER)
    valueCm < SizingLimits.MIN_DIMENSION_CM -> SizeCheck.Rejected(SIZING_BELOW_MINIMUM)
    valueCm > SizingLimits.MAX_WIDTH_DEPTH_CM -> SizeCheck.Rejected(SIZING_CAP_EXCEEDED)
    valueCm > SizingLimits.SOFT_WARN_WIDTH_DEPTH_CM -> SizeCheck.NeedsConfirm(valueCm)
    else -> SizeCheck.Ok
}
* */
