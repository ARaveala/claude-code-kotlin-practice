package com.practice.plantUser.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CoerceDimensionInputTest {
    @Test
    fun `digits are accepted`() {
        assertEquals("123", coerceDimensionInput(current = "12", candidate = "123"))
    }

    @Test
    fun `single decimal point is accepted mid-typing`() {
        assertEquals("12.", coerceDimensionInput(current = "12", candidate = "12."))
    }

    @Test
    fun `second decimal point is rejected, keeping the previous value`() {
        assertEquals("12.5", coerceDimensionInput(current = "12.5", candidate = "12.5."))
    }

    @Test
    fun `letters are rejected, keeping the previous value`() {
        assertEquals("12", coerceDimensionInput(current = "12", candidate = "12a"))
    }

    @Test
    fun `clearing the field is accepted`() {
        assertEquals("", coerceDimensionInput(current = "12", candidate = ""))
    }
}
