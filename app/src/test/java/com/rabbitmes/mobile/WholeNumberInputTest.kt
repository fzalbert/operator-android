package com.rabbitmes.mobile

import com.rabbitmes.mobile.ui.operations.normalizeWholeNumberInput
import org.junit.Assert.assertEquals
import org.junit.Test

class WholeNumberInputTest {
    @Test
    fun `replaces leading zero when user enters a number`() {
        assertEquals("7", normalizeWholeNumberInput("07"))
    }

    @Test
    fun `keeps a single zero and allows an empty value`() {
        assertEquals("0", normalizeWholeNumberInput("000"))
        assertEquals("", normalizeWholeNumberInput(""))
    }
}
