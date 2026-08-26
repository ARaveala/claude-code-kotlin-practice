package com.practice.plant_user.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsPositiveDimensionTest {

    @Test
    fun `positive value is accepted`() {
        assertTrue(isPositiveDimension("100"))
    }

    @Test
    fun `zero is rejected`() {
        // Boundary check: this is exactly the 0x0 GrowZone case this function exists to prevent.
        assertFalse(isPositiveDimension("0"))
    }

    @Test
    fun `negative value is rejected`() {
        assertFalse(isPositiveDimension("-5"))
    }

    @Test
    fun `blank input is rejected`() {
        assertFalse(isPositiveDimension(""))
    }

    @Test
    fun `non numeric input is rejected`() {
        assertFalse(isPositiveDimension("abc"))
    }

    @Test
    fun `decimal value is accepted`() {
        assertTrue(isPositiveDimension("12.5"))
    }
}

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
