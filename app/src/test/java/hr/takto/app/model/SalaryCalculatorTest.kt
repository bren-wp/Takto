package hr.takto.app.model

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SalaryCalculatorTest {
    @Test
    fun uses2026PublicServiceBaseForOctober() {
        assertEquals("1025.00", SalaryCalculator.publicServiceBaseAmount(YearMonth.of(2026, 10))?.toPlainString())
    }

    @Test
    fun calculatesBaseServiceAndNetFromConfiguredInputs() {
        val profile = SalaryProfile(
            regime = SalaryRegime.PUBLIC_SERVICE,
            coefficient = 2.0,
            completedYearsOfService = 10,
            lowerTaxRatePercent = 20.0,
            higherTaxRatePercent = 30.0,
            personalAllowanceEur = 600.0
        )
        val result = SalaryCalculator.calculate(
            YearMonth.of(2026, 10),
            profile,
            SalaryWorkSummary(
                fundMinutes = 176 * 60,
                workedMinutes = 176 * 60,
                overtimeMinutes = 0,
                nightMinutes = 0,
                secondShiftMinutes = 0,
                saturdayMinutes = 0,
                sundayMinutes = 0,
                holidayMinutes = 0
            )
        )
        assertNotNull(result)
        result!!
        assertEquals("2050.00", result.basicSalaryEur.toPlainString())
        assertEquals("102.50", result.serviceAddEur.toPlainString())
        assertEquals("2152.50", result.grossEur.toPlainString())
        assertEquals("1497.60", result.netEur.toPlainString())
    }

    @Test
    fun standardWeekTreatsWeekendHoursAsOvertime() {
        val profile = SalaryProfile(
            regime = SalaryRegime.STATE_SERVICE,
            workOrganization = WorkOrganization.STANDARD_WEEK,
            coefficient = 1.0,
            lowerTaxRatePercent = 20.0,
            higherTaxRatePercent = 30.0
        )
        val saturday = ShiftEntry(
            date = java.time.LocalDate.of(2026, 10, 3),
            code = "D",
            label = "Dan",
            colorArgb = 0xFF2488FF,
            startMinute = 8 * 60,
            endMinute = 16 * 60
        )
        val summary = SalaryCalculator.workSummary(listOf(saturday), profile, 176 * 60)
        assertEquals(8 * 60, summary.overtimeMinutes)
        assertEquals(8 * 60, summary.saturdayMinutes)
    }
}
