package com.pauldavid74.ai_dnd.core.rules

import org.junit.Assert.*
import org.junit.Test

class DiceParserTest {
    private val parser = DiceParser()

    @Test
    fun testParseBasicNotation() {
        val roll = parser.parse("1d20")
        assertNotNull(roll)
        assertEquals(1, roll?.count)
        assertEquals(20, roll?.sides)
        assertEquals(0, roll?.modifier)
    }

    @Test
    fun testParseWithModifier() {
        val roll = parser.parse("2d6+4")
        assertNotNull(roll)
        assertEquals(2, roll?.count)
        assertEquals(6, roll?.sides)
        assertEquals(4, roll?.modifier)
    }

    @Test
    fun testParseWithNegativeModifier() {
        val roll = parser.parse("1d8-2")
        assertNotNull(roll)
        assertEquals(1, roll?.count)
        assertEquals(8, roll?.sides)
        assertEquals(-2, roll?.modifier)
    }

    @Test
    fun testParseWithSpaces() {
        val roll = parser.parse(" 3 d 10 + 5 ")
        assertNotNull(roll)
        assertEquals(3, roll?.count)
        assertEquals(10, roll?.sides)
        assertEquals(5, roll?.modifier)
    }

    @Test
    fun testParseInvalidNotation() {
        assertNull(parser.parse("invalid"))
        assertNull(parser.parse("d20")) // Requires count
    }
}
