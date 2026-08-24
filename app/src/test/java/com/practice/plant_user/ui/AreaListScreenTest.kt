package com.practice.plant_user.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoerceAreaNameInputTest {

    @Test
    fun `input longer than the limit is rejected, keeping the previous value`() {
        val current = "Backyar"
        val candidate = current + "dd" // 9 chars, over an 8 char limit

        val result = coerceAreaNameInput(current, candidate, maxLength = 8)

        assertEquals(current, result)
    }

    @Test
    fun `input exactly at the limit is accepted`() {
        // Boundary check: off by one errors in "<=" vs "<" show up exactly here.
        val candidate = "12345678" // exactly 8 chars

        val result = coerceAreaNameInput(current = "", candidate = candidate, maxLength = 8)

        assertEquals(candidate, result)
    }

    @Test
    fun `input under the limit is accepted unchanged`() {
        val candidate = "Greenhouse"

        val result = coerceAreaNameInput(current = "", candidate = candidate, maxLength = 50)

        assertEquals(candidate, result)
    }
}

class CanAddAreaTest {

    @Test
    fun `returns true when current count is below the max`() {
        assertTrue(canAddArea(currentCount = 99, max = 100))
    }

    @Test
    fun `returns false at exactly the max`() {
        // Boundary check: this is the count at which the Add button should become disabled.
        assertFalse(canAddArea(currentCount = 100, max = 100))
    }

    @Test
    fun `returns false above the max`() {
        // Shouldn't be reachable through the app's own UI once the cap is enforced, but pinning it
        // means the predicate itself stays safe even if some future caller skips the UI gate.
        assertFalse(canAddArea(currentCount = 101, max = 100))
    }
}
