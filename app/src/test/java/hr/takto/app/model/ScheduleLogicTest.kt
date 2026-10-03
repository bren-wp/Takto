package hr.takto.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ScheduleLogicTest {
    @Test
    fun normalizeReusableCode_trimsUppercasesAndCollapsesWhitespace() {
        assertEquals("12-20", ScheduleLogic.normalizeReusableCode(" 12-20 "))
        assertEquals("TEREN 1", ScheduleLogic.normalizeReusableCode(" teren   1 "))
    }

    @Test
    fun parsePatternSequence_supportsFreeDaysAndSemicolon() {
        val result = ScheduleLogic.parsePatternSequence("D; D; N; N; -; slobodno; GO")
        assertEquals(listOf("D", "D", "N", "N", null, null, "GO"), result)
    }

    @Test
    fun parsePatternSequence_limitsSteps() {
        val input = (1..40).joinToString(",") { "D" }
        assertEquals(ScheduleLogic.MAX_PATTERN_STEPS, ScheduleLogic.parsePatternSequence(input).size)
    }

    @Test
    fun startOfWeek_usesMonday() {
        val thursday = LocalDate.of(2026, 10, 1)
        assertEquals(LocalDate.of(2026, 9, 28), ScheduleLogic.startOfWeek(thursday))
        assertEquals(LocalDate.of(2026, 9, 28), ScheduleLogic.startOfWeek(LocalDate.of(2026, 9, 28)))
    }

    @Test
    fun weekDates_returnsMondayThroughSunday() {
        val dates = ScheduleLogic.weekDates(LocalDate.of(2026, 10, 1))
        assertEquals(7, dates.size)
        assertEquals(LocalDate.of(2026, 9, 28), dates.first())
        assertEquals(LocalDate.of(2026, 10, 4), dates.last())
    }

    @Test
    fun monthWeekdays_excludesWeekend() {
        val dates = ScheduleLogic.monthWeekdays(YearMonth.of(2026, 10))
        assertTrue(dates.all { it.dayOfWeek.value in 1..5 })
        assertTrue(LocalDate.of(2026, 10, 1) in dates)
        assertTrue(LocalDate.of(2026, 10, 3) !in dates)
    }

    @Test
    fun datesInclusive_isSafeAndInclusive() {
        val dates = ScheduleLogic.datesInclusive(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3))
        assertEquals(3, dates.size)
        assertEquals(LocalDate.of(2026, 10, 3), dates.last())
        assertTrue(ScheduleLogic.datesInclusive(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 1)).isEmpty())
    }
    @Test
    fun parseClock_supportsCommonFormats() {
        assertEquals(7 * 60 + 30, ScheduleLogic.parseClock("07:30"))
        assertEquals(19 * 60, ScheduleLogic.parseClock("19"))
        assertEquals(6 * 60 + 5, ScheduleLogic.parseClock("6.05"))
        assertEquals(7 * 60 + 30, ScheduleLogic.parseClock("730"))
        assertEquals(7 * 60 + 30, ScheduleLogic.parseClock("0730"))
        assertEquals(18 * 60 + 45, ScheduleLogic.parseClock("18,45"))
        assertEquals(6 * 60 + 5, ScheduleLogic.parseClock("6 05"))
        assertNull(ScheduleLogic.parseClock("25:00"))
        assertNull(ScheduleLogic.parseClock("12:99"))
        assertNull(ScheduleLogic.parseClock("2360"))
    }

    @Test
    fun workDurationMinutes_handlesOvernightAndBreak() {
        assertEquals(7 * 60 + 30, ScheduleLogic.workDurationMinutes(7 * 60, 15 * 60, 30))
        assertEquals(11 * 60 + 30, ScheduleLogic.workDurationMinutes(19 * 60, 7 * 60, 30))
        assertEquals(0, ScheduleLogic.workDurationMinutes(8 * 60, 8 * 60, 0))
    }

    @Test
    fun overtimeMinutes_usesDailyStandard() {
        assertEquals(0, ScheduleLogic.overtimeMinutes(7 * 60, 8 * 60))
        assertEquals(4 * 60, ScheduleLogic.overtimeMinutes(12 * 60, 8 * 60))
    }

    @Test
    fun regularWorkMinutesSubtractsOnlyConfirmedOvertime() {
        assertEquals(8 * 60, ScheduleLogic.regularWorkMinutes(10 * 60, 2 * 60))
        assertEquals(10 * 60, ScheduleLogic.regularWorkMinutes(10 * 60, 0))
        assertEquals(0, ScheduleLogic.regularWorkMinutes(60, 120))
        assertEquals(0, ScheduleLogic.regularWorkMinutes(-30, 0))
    }

    @Test
    fun automaticMonthlyTarget_usesWeekdays() {
        val month = YearMonth.of(2026, 10)
        assertEquals(ScheduleLogic.monthWeekdays(month).size * 8 * 60, ScheduleLogic.automaticMonthlyTargetMinutes(month, 8 * 60))
    }

    @Test
    fun nightWorkMinutes_handlesOvernightAndMorningShift() {
        assertEquals(8 * 60, ScheduleLogic.nightWorkMinutes(22 * 60, 6 * 60, 0))
        assertEquals(4 * 60, ScheduleLogic.nightWorkMinutes(2 * 60, 10 * 60, 0))
        assertEquals(0, ScheduleLogic.nightWorkMinutes(7 * 60, 15 * 60, 0))
    }

    @Test
    fun weekendAndSundayMinutes_followCalendarAcrossMidnight() {
        val saturday = LocalDate.of(2026, 10, 3)
        assertEquals(8 * 60, ScheduleLogic.weekendWorkMinutes(saturday, 20 * 60, 4 * 60, 0))
        assertEquals(4 * 60, ScheduleLogic.saturdayWorkMinutes(saturday, 20 * 60, 4 * 60, 0))
        assertEquals(4 * 60, ScheduleLogic.sundayWorkMinutes(saturday, 20 * 60, 4 * 60, 0))

        val friday = LocalDate.of(2026, 10, 2)
        assertEquals(4 * 60, ScheduleLogic.weekendWorkMinutes(friday, 20 * 60, 4 * 60, 0))
        assertEquals(4 * 60, ScheduleLogic.saturdayWorkMinutes(friday, 20 * 60, 4 * 60, 0))
        assertEquals(0, ScheduleLogic.sundayWorkMinutes(friday, 20 * 60, 4 * 60, 0))
    }

    @Test
    fun workTimeValidation_rejectsInvalidBreakInsteadOfSilentlyClamping() {
        val start = 7 * 60
        val end = 15 * 60
        assertTrue(ScheduleLogic.isValidWorkTime(start, end, 30))
        assertTrue(!ScheduleLogic.isValidWorkTime(start, end, 8 * 60))
        assertTrue(!ScheduleLogic.isValidWorkTime(start, end, ScheduleLogic.MAX_BREAK_MINUTES + 1))
    }

    @Test
    fun overnightDetection_distinguishesCrossMidnightWork() {
        assertTrue(ScheduleLogic.isOvernightWork(22 * 60, 6 * 60))
        assertTrue(!ScheduleLogic.isOvernightWork(7 * 60, 15 * 60))
        assertTrue(!ScheduleLogic.isOvernightWork(8 * 60, 8 * 60))
    }

    @Test
    fun grossDuration_isAvailableForClearValidationMessages() {
        assertEquals(8 * 60, ScheduleLogic.grossWorkDurationMinutes(7 * 60, 15 * 60))
        assertEquals(8 * 60, ScheduleLogic.grossWorkDurationMinutes(22 * 60, 6 * 60))
        assertNull(ScheduleLogic.grossWorkDurationMinutes(null, 15 * 60))
    }

}
