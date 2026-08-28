package com.basicsapp.timer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun `no millis hides minutes until a minute passes`() {
        assertEquals("0", TimeFormatter.formatTimeNoMillis(0))
        assertEquals("45", TimeFormatter.formatTimeNoMillis(45_123))
        assertEquals("59", TimeFormatter.formatTimeNoMillis(59_999))
        assertEquals("1:00", TimeFormatter.formatTimeNoMillis(60_000))
        assertEquals("1:05", TimeFormatter.formatTimeNoMillis(65_432))
        assertEquals("12:34", TimeFormatter.formatTimeNoMillis(754_000))
    }
}