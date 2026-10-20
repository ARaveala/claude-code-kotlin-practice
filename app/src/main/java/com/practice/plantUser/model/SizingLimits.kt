package com.practice.plantUser.model

/*
* hard caps: rejected with a named constant. NaN, infinite, zero and negative
  input are always rejected.
* */
object GrowZoneLimits {
    /** All values in cm. can these be separated into specifics, e,g GrowZones own */
    // To prevent loosing GrowZones
    const val MIN_DIMENSION_CM = 1.0

    // Check in with user "are you sure of this size", in case of fat finger input
    const val SOFT_WARN_WIDTH_DEPTH_CM = 5_000.0
    const val MAX_WIDTH_DEPTH_CM = 100_000.0

    // heigh is not visual
    const val MAX_HEIGHT_CM = 1_000.0
    const val MAX_NESTING_DEPTH = 3
}

// placeholder
object OtherLimits

/** Why [valueCm] can't be a GrowZone width or depth, or null if it's fine. Shared by the create
 * dialog and the edit panel. A value above the soft warning is still valid, see [needsSizeConfirm]. */
fun widthDepthRejectionReason(valueCm: Double): String? = when {
    // Must come first: every comparison with NaN is false, so it would slip past the range checks.
    !valueCm.isFinite() -> ValidationMessages.SIZE_NOT_A_NUMBER
    valueCm < GrowZoneLimits.MIN_DIMENSION_CM -> ValidationMessages.SIZING_BELOW_MINIMUM
    valueCm > GrowZoneLimits.MAX_WIDTH_DEPTH_CM -> ValidationMessages.SIZING_CAP_EXCEEDED
    else -> null
}

/** Why [valueCm] can't be a GrowZone height, or null if it's fine. Null input means no height was
 * entered, which is allowed (height is optional for every type). */
fun heightRejectionReason(valueCm: Double?): String? = when {
    valueCm == null -> null
    !valueCm.isFinite() -> ValidationMessages.SIZE_NOT_A_NUMBER
    valueCm < GrowZoneLimits.MIN_DIMENSION_CM -> ValidationMessages.SIZING_BELOW_MINIMUM
    valueCm > GrowZoneLimits.MAX_HEIGHT_CM -> ValidationMessages.SIZING_HEIGHT_CAP_EXCEEDED
    else -> null
}

/** Whether a valid width or depth is large enough to ask "is that correct?" (fat finger check).
 * Awareness only, never a rejection: false for anything [widthDepthRejectionReason] rejects. */
fun needsSizeConfirm(valueCm: Double): Boolean =
    valueCm > GrowZoneLimits.SOFT_WARN_WIDTH_DEPTH_CM && valueCm <= GrowZoneLimits.MAX_WIDTH_DEPTH_CM
