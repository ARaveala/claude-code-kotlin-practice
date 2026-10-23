package com.practice.plantUser.ui

import com.practice.plantUser.model.ValidationMessages
import org.junit.Assert.assertEquals
import org.junit.Test

// Written alongside checkSizeInput (not test-first): it is the junction where typed text becomes the
// Double that both the ghost preview and confirm use, so its parsing is pinned here.
private fun check(width: String, depth: String = "100", height: String = "") = checkSizeInput(width, depth, height)

private const val WHERE = "in ui/SizeFields.kt"

class CheckSizeInputTest {
    @Test
    fun `typed decimal is kept exactly as Double`() {
        // Delta 0.0: the ghost and Save both use this value, no Float round trip.
        assertEquals("checkSizeInput(\"353.3\").widthCm $WHERE", 353.3, check("353.3").widthCm!!, 0.0)
    }

    @Test
    fun `blank height is valid and null`() {
        val input = check("100", height = "")
        assertEquals("checkSizeInput(height=\"\").isValid $WHERE", true, input.isValid)
        assertEquals("checkSizeInput(height=\"\").heightCm $WHERE", null, input.heightCm)
    }

    @Test
    fun `half typed height is invalid without a message`() {
        val input = check("100", height = ".")
        assertEquals("checkSizeInput(height=\".\").isValid $WHERE", false, input.isValid)
        assertEquals("checkSizeInput(height=\".\").heightError $WHERE", null, input.heightError)
    }

    @Test
    fun `blank width is invalid without a message`() {
        val input = check("")
        assertEquals("checkSizeInput(width=\"\").isValid $WHERE", false, input.isValid)
        assertEquals("checkSizeInput(width=\"\").widthError $WHERE", null, input.widthError)
    }

    @Test
    fun `below minimum width is rejected with its message and no preview value`() {
        val input = check("0.5")
        assertEquals(
            "checkSizeInput(width=\"0.5\").widthError $WHERE",
            ValidationMessages.SIZING_BELOW_MINIMUM,
            input.widthError
        )
        assertEquals("checkSizeInput(width=\"0.5\").widthCm $WHERE", null, input.widthCm)
        assertEquals("checkSizeInput(width=\"0.5\").isValid $WHERE", false, input.isValid)
    }

    @Test
    fun `over soft threshold warns with the larger value`() {
        assertEquals(
            "checkSizeInput(6000 x 5500).softWarnValueCm $WHERE",
            6000.0,
            check("6000", depth = "5500").softWarnValueCm!!,
            0.0
        )
    }

    @Test
    fun `at soft threshold does not warn`() {
        assertEquals("checkSizeInput(5000).softWarnValueCm $WHERE", null, check("5000").softWarnValueCm)
    }

    @Test
    fun `invalid input never warns`() {
        // Width warns on its own, but the bad height makes the whole input invalid.
        assertEquals(
            "checkSizeInput(6000, height=2000).softWarnValueCm $WHERE",
            null,
            check("6000", height = "2000").softWarnValueCm
        )
    }
}
