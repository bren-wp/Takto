package hr.takto.app.model

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CroatianHolidaysTest {
    @Test
    fun calculatesMovableHolidaysFor2026() {
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 4, 5)))
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 4, 6)))
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 6, 4)))
    }

    @Test
    fun knowsFixedCroatianHolidays() {
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 1, 1)))
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 5, 30)))
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 11, 18)))
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 12, 25)))
        assertFalse(CroatianHolidays.isHoliday(LocalDate.of(2026, 10, 8)))
    }

    @Test
    fun automaticFundExcludesWeekdayHolidays() {
        val april = YearMonth.of(2026, 4)
        val weekdayCount = ScheduleLogic.monthWeekdays(april).size
        val weekdayHolidays = ScheduleLogic.monthWeekdays(april).count(CroatianHolidays::isHoliday)
        assertEquals(
            (weekdayCount - weekdayHolidays) * 8 * 60,
            ScheduleLogic.automaticMonthlyTargetMinutes(april, 8 * 60)
        )
    }
}
