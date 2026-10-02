package hr.takto.app.model

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollCalculatorTest {
    @Test
    fun officialBaseUsesVerified2026Periods() {
        assertEquals(1004.87, CroatianPayrollRules2026.officialBase(YearMonth.of(2026, 1), PayrollSystem.PUBLIC_SERVICE)!!, 0.001)
        assertEquals(1015.0, CroatianPayrollRules2026.officialBase(YearMonth.of(2026, 4), PayrollSystem.STATE_SERVICE)!!, 0.001)
        assertEquals(1025.0, CroatianPayrollRules2026.officialBase(YearMonth.of(2026, 10), PayrollSystem.PUBLIC_SERVICE)!!, 0.001)
        assertEquals(1035.0, CroatianPayrollRules2026.officialBase(YearMonth.of(2026, 12), PayrollSystem.STATE_SERVICE)!!, 0.001)
    }

    @Test
    fun publicServiceStacksVerifiedSupplements() {
        val result = PayrollCalculator.calculate(
            PayrollProfile(
                enabled = true,
                system = PayrollSystem.PUBLIC_SERVICE,
                coefficient = 2.0,
                yearsOfService = 10,
                lowerTaxRatePercent = 20.0,
                higherTaxRatePercent = 30.0,
                personalAllowanceEur = 600.0
            ),
            PayrollInputs(
                month = YearMonth.of(2026, 10),
                monthlyFundMinutes = 176 * 60,
                workedMinutes = 184 * 60,
                overtimeMinutes = 8 * 60,
                nightMinutes = 16 * 60,
                saturdayMinutes = 8 * 60,
                sundayMinutes = 8 * 60,
                holidayMinutes = 0
            )
        )

        assertTrue(result.complete)
        assertTrue(result.grossEur > result.baseSalaryEur)
        assertTrue(result.overtimePayEur > 0.0)
        assertTrue(result.nightSupplementEur > 0.0)
        assertTrue(result.saturdaySupplementEur > 0.0)
        assertTrue(result.sundaySupplementEur > 0.0)
        assertTrue(result.netSalaryEur > 0.0)
    }

    @Test
    fun incompleteTaxProfileDoesNotPretendToBeExact() {
        val result = PayrollCalculator.calculate(
            PayrollProfile(
                enabled = true,
                system = PayrollSystem.STATE_SERVICE,
                coefficient = 2.0
            ),
            PayrollInputs(
                month = YearMonth.of(2026, 10),
                monthlyFundMinutes = 176 * 60,
                workedMinutes = 176 * 60,
                overtimeMinutes = 0,
                nightMinutes = 0,
                saturdayMinutes = 0,
                sundayMinutes = 0,
                holidayMinutes = 0
            )
        )
        assertFalse(result.complete)
        assertTrue("niža stopa poreza" in result.missing)
        assertTrue("viša stopa poreza" in result.missing)
    }

    @Test
    fun pensionContributionBaseReductionIsAppliedBelow1300Gross() {
        val result = PayrollCalculator.calculate(
            PayrollProfile(
                enabled = true,
                system = PayrollSystem.OTHER,
                manualBaseEur = 1_000.0,
                coefficient = 1.0,
                lowerTaxRatePercent = 20.0,
                higherTaxRatePercent = 30.0,
                personalAllowanceEur = 600.0
            ),
            PayrollInputs(
                month = YearMonth.of(2026, 10),
                monthlyFundMinutes = 176 * 60,
                workedMinutes = 176 * 60,
                overtimeMinutes = 0,
                nightMinutes = 0,
                saturdayMinutes = 0,
                sundayMinutes = 0,
                holidayMinutes = 0
            )
        )
        assertTrue(result.complete)
        assertEquals(150.0, result.pensionBaseReductionEur, 0.01)
        assertEquals(850.0, result.pensionContributionBaseEur, 0.01)
        assertEquals(127.5, result.pensionPillarIEur, 0.01)
        assertEquals(42.5, result.pensionPillarIIEur, 0.01)
    }

    @Test
    fun invalidTaxRateOrderIsRejected() {
        val result = PayrollCalculator.calculate(
            PayrollProfile(
                enabled = true,
                system = PayrollSystem.OTHER,
                manualBaseEur = 2_000.0,
                coefficient = 1.0,
                lowerTaxRatePercent = 30.0,
                higherTaxRatePercent = 20.0
            ),
            PayrollInputs(
                month = YearMonth.of(2026, 10),
                monthlyFundMinutes = 176 * 60,
                workedMinutes = 176 * 60,
                overtimeMinutes = 0,
                nightMinutes = 0,
                saturdayMinutes = 0,
                sundayMinutes = 0,
                holidayMinutes = 0
            )
        )
        assertFalse(result.complete)
        assertTrue("porezne stope nisu valjane" in result.missing)
    }

    @Test
    fun stateAndPublicServiceCoefficientMustBeInsideOfficialScale() {
        val result = PayrollCalculator.calculate(
            PayrollProfile(
                enabled = true,
                system = PayrollSystem.PUBLIC_SERVICE,
                coefficient = 8.5,
                lowerTaxRatePercent = 20.0,
                higherTaxRatePercent = 30.0
            ),
            PayrollInputs(
                month = YearMonth.of(2026, 10),
                monthlyFundMinutes = 176 * 60,
                workedMinutes = 176 * 60,
                overtimeMinutes = 0,
                nightMinutes = 0,
                saturdayMinutes = 0,
                sundayMinutes = 0,
                holidayMinutes = 0
            )
        )
        assertFalse(result.complete)
        assertTrue(result.missing.any { it.contains("1,00–8,00") })
    }

    @Test
    fun otherSystemRequiresManualBase() {
        val result = PayrollCalculator.calculate(
            PayrollProfile(
                enabled = true,
                system = PayrollSystem.OTHER,
                coefficient = 1.5,
                lowerTaxRatePercent = 20.0,
                higherTaxRatePercent = 30.0
            ),
            PayrollInputs(
                month = YearMonth.of(2026, 10),
                monthlyFundMinutes = 176 * 60,
                workedMinutes = 176 * 60,
                overtimeMinutes = 0,
                nightMinutes = 0,
                saturdayMinutes = 0,
                sundayMinutes = 0,
                holidayMinutes = 0
            )
        )
        assertFalse(result.complete)
        assertTrue("osnovica" in result.missing)
    }
}
