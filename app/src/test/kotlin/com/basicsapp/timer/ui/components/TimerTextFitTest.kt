package com.basicsapp.timer.ui.components

import androidx.compose.ui.unit.TextUnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerTextFitTest {

    private fun assertFloat(expected: Float, actual: Float, delta: Float = 0.0001f) =
        assertEquals(expected, actual, delta)

    @Test
    fun fitIsOneWhenTextFits() {
        assertFloat(1f, timerTextFit(measuredWidthPx = 800f, availWidthPx = 1000f))
    }

    @Test
    fun fitIsOneWhenTextExactlyEqualsAvailableWidth() {
        assertFloat(1f, timerTextFit(measuredWidthPx = 1000f, availWidthPx = 1000f))
    }

    @Test
    fun fitNeverGrowsText() {
        assertFloat(1f, timerTextFit(measuredWidthPx = 500f, availWidthPx = 2000f))
    }

    @Test
    fun fitShrinksProportionallyWhenTextOverflows() {
        assertFloat(0.8f, timerTextFit(measuredWidthPx = 1000f, availWidthPx = 800f))
        assertFloat(0.5f, timerTextFit(measuredWidthPx = 2000f, availWidthPx = 1000f))
    }

    @Test
    fun degenerateMeasuredWidthNeverShrinks() {
        assertFloat(1f, timerTextFit(measuredWidthPx = 0f, availWidthPx = 1000f))
        assertFloat(1f, timerTextFit(measuredWidthPx = 1f, availWidthPx = 1000f))
    }

    @Test
    fun zeroOrNegativeAvailableWidthYieldsOne() {
        assertFloat(1f, timerTextFit(measuredWidthPx = 1000f, availWidthPx = 0f))
        assertFloat(1f, timerTextFit(measuredWidthPx = 1000f, availWidthPx = -1f))
    }

    @Test
    fun trackingEmRatioIsMinusTwoOverSeventyTwo() {
        // -2sp against the 72sp nominal font, expressed in em so the spacing
        // stays proportional when the font auto-shrinks.
        assertEquals(-2f / 72f, TimerTextTrackingEm.value, 0.00001f)
        assertEquals(TextUnitType.Em, TimerTextTrackingEm.type)
    }
}
