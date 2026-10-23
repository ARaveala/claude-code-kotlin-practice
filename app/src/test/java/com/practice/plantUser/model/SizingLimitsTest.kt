package com.practice.plantUser.model

import org.junit.Assert.assertEquals
import org.junit.Test

// Boundary values come from phase_3.md's 3a exit criteria, same standard as the nesting depth cap.
//
// Side effect of test-first stubs: while the functions under test were stubs returning null/false,
// every "accepted" (null) and "does not warn" (false) test passed without testing anything; only
// the rejected/warns tests proved the stub failed. If these functions are ever stubbed again,
// a green accept test is not coverage.
//
// Each assert helper names the function, its input and its file, so a failure reads e.g.
// "widthDepthRejectionReason(0.99) in model/SizingLimits.kt expected:<...> but was:<null>".

private fun assertWidthDepth(expected: String?, valueCm: Double) = assertEquals(
    "widthDepthRejectionReason($valueCm) in model/SizingLimits.kt",
    expected,
    widthDepthRejectionReason(valueCm)
)

private fun assertHeight(expected: String?, valueCm: Double?) = assertEquals(
    "heightRejectionReason($valueCm) in model/SizingLimits.kt",
    expected,
    heightRejectionReason(valueCm)
)

private fun assertConfirm(expected: Boolean, valueCm: Double) = assertEquals(
    "needsSizeConfirm($valueCm) in model/SizingLimits.kt",
    expected,
    needsSizeConfirm(valueCm)
)

class WidthDepthRejectionReasonTest {
    @Test
    fun `just below minimum is rejected`() {
        assertWidthDepth(ValidationMessages.SIZING_BELOW_MINIMUM, 0.99)
    }

    @Test
    fun `minimum is accepted`() {
        assertWidthDepth(null, 1.0)
    }

    @Test
    fun `just below cap is accepted`() {
        assertWidthDepth(null, 99_999.0)
    }

    @Test
    fun `cap is accepted`() {
        assertWidthDepth(null, 100_000.0)
    }

    @Test
    fun `just above cap is rejected`() {
        assertWidthDepth(ValidationMessages.SIZING_CAP_EXCEEDED, 100_001.0)
    }

    @Test
    fun `zero is rejected`() {
        assertWidthDepth(ValidationMessages.SIZING_BELOW_MINIMUM, 0.0)
    }

    @Test
    fun `negative is rejected`() {
        assertWidthDepth(ValidationMessages.SIZING_BELOW_MINIMUM, -5.0)
    }

    @Test
    fun `NaN is rejected`() {
        // Every comparison with NaN is false, so a plain < / > check would let it through.
        assertWidthDepth(ValidationMessages.SIZE_NOT_A_NUMBER, Double.NaN)
    }

    @Test
    fun `positive infinity is rejected`() {
        assertWidthDepth(ValidationMessages.SIZE_NOT_A_NUMBER, Double.POSITIVE_INFINITY)
    }

    @Test
    fun `negative infinity is rejected`() {
        assertWidthDepth(ValidationMessages.SIZE_NOT_A_NUMBER, Double.NEGATIVE_INFINITY)
    }
}

class HeightRejectionReasonTest {
    @Test
    fun `no height is accepted`() {
        assertHeight(null, null)
    }

    @Test
    fun `cap is accepted`() {
        assertHeight(null, 1_000.0)
    }

    @Test
    fun `just above cap is rejected`() {
        assertHeight(ValidationMessages.SIZING_HEIGHT_CAP_EXCEEDED, 1_001.0)
    }

    @Test
    fun `just below minimum is rejected`() {
        assertHeight(ValidationMessages.SIZING_BELOW_MINIMUM, 0.99)
    }

    @Test
    fun `zero is rejected`() {
        assertHeight(ValidationMessages.SIZING_BELOW_MINIMUM, 0.0)
    }

    @Test
    fun `negative is rejected`() {
        assertHeight(ValidationMessages.SIZING_BELOW_MINIMUM, -5.0)
    }

    @Test
    fun `NaN is rejected`() {
        assertHeight(ValidationMessages.SIZE_NOT_A_NUMBER, Double.NaN)
    }

    @Test
    fun `infinity is rejected`() {
        assertHeight(ValidationMessages.SIZE_NOT_A_NUMBER, Double.POSITIVE_INFINITY)
    }
}

class NeedsSizeConfirmTest {
    @Test
    fun `soft threshold does not warn`() {
        assertConfirm(false, 5_000.0)
    }

    @Test
    fun `just above soft threshold warns`() {
        assertConfirm(true, 5_001.0)
    }

    @Test
    fun `cap still warns`() {
        assertConfirm(true, 100_000.0)
    }

    @Test
    fun `above cap does not warn, it is rejected instead`() {
        assertConfirm(false, 100_001.0)
    }

    @Test
    fun `NaN does not warn`() {
        assertConfirm(false, Double.NaN)
    }
}

class FormatCmTest {
    // Not a stub: formatCm is implemented, so these pass for real.
    @Test
    fun `whole number drops the decimal`() {
        assertEquals("formatCm(100000.0) in model/ValidationMessages.kt", "100000", formatCm(100_000.0))
    }

    @Test
    fun `fraction is kept`() {
        assertEquals("formatCm(12.5) in model/ValidationMessages.kt", "12.5", formatCm(12.5))
    }

    @Test
    fun `huge whole number does not saturate`() {
        // toLong() would clamp this to Long.MAX_VALUE and print a different number.
        assertEquals("formatCm(1e20) in model/ValidationMessages.kt", "1.0E20", formatCm(1e20))
    }
}
