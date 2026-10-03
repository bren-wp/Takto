package hr.takto.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultShiftTypesTest {
    @Test
    fun freshInstallStartsWithDayAndNightShortcuts() {
        assertEquals(listOf("D", "N"), DefaultShiftTypes.presets.take(2).map { it.code })
    }

    @Test
    fun builtInCodesHaveUniqueDefaultColors() {
        val codes = DefaultShiftTypes.presets.map { it.code }
        val colors = DefaultShiftTypes.presets.map { it.color.value }
        assertEquals(listOf("D", "N", "J", "GO", "SD", "BO", "PD"), codes)
        assertEquals(colors.size, colors.distinct().size)
    }

    @Test
    fun leaveAndFreeStatusesDoNotRepresentWorkedShifts() {
        listOf("GO", "SD", "BO", "PD").forEach { code ->
            assertTrue("$code should be treated as non-working status", ScheduleLogic.isLeaveCode(code))
        }
    }
}
