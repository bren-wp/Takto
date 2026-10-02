package hr.takto.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarUiLogicTest {
    @Test
    fun narrowCalendarStacksInteractiveControls() {
        assertTrue(CalendarUiLogic.shouldStack(359f, 1f))
        assertEquals(1, CalendarUiLogic.shiftChoiceColumns(359f, 1f))
    }

    @Test
    fun regularWidthKeepsTwoShiftColumns() {
        assertFalse(CalendarUiLogic.shouldStack(360f, 1f))
        assertEquals(2, CalendarUiLogic.shiftChoiceColumns(360f, 1f))
    }

    @Test
    fun largeFontStacksControlsEvenOnWiderScreens() {
        assertTrue(CalendarUiLogic.shouldStack(420f, 1.30f))
        assertEquals(1, CalendarUiLogic.shiftChoiceColumns(420f, 1.30f))
    }

    @Test
    fun fontScaleJustBelowThresholdKeepsRegularLayout() {
        assertFalse(CalendarUiLogic.shouldStack(420f, 1.29f))
        assertEquals(2, CalendarUiLogic.shiftChoiceColumns(420f, 1.29f))
    }

    @Test
    fun invalidMeasurementsUseSafeFallbacks() {
        assertTrue(CalendarUiLogic.shouldStack(Float.NaN, 1f))
        assertFalse(CalendarUiLogic.shouldStack(420f, Float.NaN))
    }
}
