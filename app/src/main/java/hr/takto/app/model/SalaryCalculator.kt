package hr.takto.app.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

enum class SalaryRegime(val persistedValue: String) {
    UNSET("unset"),
    STATE_SERVICE("state"),
    PUBLIC_SERVICE("public"),
    OTHER("other");

    companion object {
        fun fromPersisted(value: String?): SalaryRegime =
            entries.firstOrNull { it.persistedValue == value } ?: UNSET
    }
}

enum class WorkOrganization(val persistedValue: String) {
    STANDARD_WEEK("standard_week"),
    SHIFTS("shifts"),
    TURNUS("turnus"),
    OTHER("other");

    companion object {
        fun fromPersisted(value: String?): WorkOrganization =
            entries.firstOrNull { it.persistedValue == value } ?: STANDARD_WEEK
    }
}

data class SalaryProfile(
    val regime: SalaryRegime = SalaryRegime.UNSET,
    val workOrganization: WorkOrganization = WorkOrganization.STANDARD_WEEK,
    val coefficient: Double = 0.0,
    val completedYearsOfService: Int = 0,
    val lowerTaxRatePercent: Double = 0.0,
    val higherTaxRatePercent: Double = 0.0,
    val personalAllowanceEur: Double = 600.0,
    val regularShiftMinutes: Int = 8 * 60,
    val secondShiftEligible: Boolean = false,
    val otherGrossAdditionsEur: Double = 0.0
) {
    val isConfigured: Boolean
        get() = regime != SalaryRegime.UNSET &&
            coefficient > 0.0 &&
            lowerTaxRatePercent > 0.0 &&
            higherTaxRatePercent > 0.0 &&
            personalAllowanceEur >= 0.0
}

data class SalaryWorkSummary(
    val fundMinutes: Int,
    val workedMinutes: Int,
    val overtimeMinutes: Int,
    val nightMinutes: Int,
    val secondShiftMinutes: Int,
    val saturdayMinutes: Int,
    val sundayMinutes: Int,
    val holidayMinutes: Int,
    val turnusMinutes: Int = 0
)

data class SalaryResult(
    val baseAmountEur: BigDecimal,
    val basicSalaryEur: BigDecimal,
    val serviceAddEur: BigDecimal,
    val hourlyRateEur: BigDecimal,
    val overtimeGrossEur: BigDecimal,
    val nightAddEur: BigDecimal,
    val secondShiftAddEur: BigDecimal,
    val turnusAddEur: BigDecimal,
    val saturdayAddEur: BigDecimal,
    val sundayAddEur: BigDecimal,
    val holidayAddEur: BigDecimal,
    val otherGrossAdditionsEur: BigDecimal,
    val grossEur: BigDecimal,
    val pensionContributionsEur: BigDecimal,
    val taxableIncomeEur: BigDecimal,
    val incomeTaxEur: BigDecimal,
    val netEur: BigDecimal
)

object SalaryCalculator {
    private val ZERO = BigDecimal.ZERO.setScale(2)
    private val ONE_HUNDRED = BigDecimal("100")
    private val PENSION_RATE = BigDecimal("0.20")
    private val MONTHLY_HIGHER_RATE_THRESHOLD = BigDecimal("5000.00")

    fun publicServiceBaseAmount(month: YearMonth): BigDecimal? = when {
        month < YearMonth.of(2026, 1) -> null
        month <= YearMonth.of(2026, 3) -> BigDecimal("1004.87")
        month <= YearMonth.of(2026, 7) -> BigDecimal("1015.00")
        month <= YearMonth.of(2026, 11) -> BigDecimal("1025.00")
        else -> BigDecimal("1035.00")
    }

    fun calculate(
        month: YearMonth,
        profile: SalaryProfile,
        work: SalaryWorkSummary
    ): SalaryResult? {
        if (!profile.isConfigured || work.fundMinutes <= 0) return null
        if (profile.regime !in setOf(SalaryRegime.STATE_SERVICE, SalaryRegime.PUBLIC_SERVICE)) return null

        val baseAmount = publicServiceBaseAmount(month) ?: return null
        val coefficient = bd(profile.coefficient)
        val basicSalary = money(baseAmount.multiply(coefficient))
        val serviceRate = bd(profile.completedYearsOfService.coerceAtLeast(0))
            .multiply(BigDecimal("0.005"))
        val serviceAdd = money(basicSalary.multiply(serviceRate))
        val salaryWithService = basicSalary.add(serviceAdd)

        val fundHours = bd(work.fundMinutes).divide(BigDecimal("60"), 8, RoundingMode.HALF_UP)
        if (fundHours.signum() <= 0) return null
        val hourlyRate = salaryWithService.divide(fundHours, 8, RoundingMode.HALF_UP)

        // Prekovremeni sat je redovna cijena sata + dodatak 50 %.
        val overtimeGross = hoursMoney(hourlyRate, work.overtimeMinutes, BigDecimal("1.50"))
        val nightAdd = hoursMoney(hourlyRate, work.nightMinutes, BigDecimal("0.40"))
        val secondShiftAdd = if (profile.secondShiftEligible) {
            hoursMoney(hourlyRate, work.secondShiftMinutes, BigDecimal("0.10"))
        } else ZERO
        val turnusAdd = if (
            profile.regime == SalaryRegime.STATE_SERVICE &&
            profile.workOrganization == WorkOrganization.TURNUS
        ) {
            hoursMoney(hourlyRate, work.turnusMinutes, BigDecimal("0.05"))
        } else ZERO
        val saturdayAdd = hoursMoney(hourlyRate, work.saturdayMinutes, BigDecimal("0.25"))
        val sundayAdd = hoursMoney(hourlyRate, work.sundayMinutes, BigDecimal("0.50"))
        val holidayAdd = hoursMoney(hourlyRate, work.holidayMinutes, BigDecimal("1.50"))
        val otherGross = money(bd(profile.otherGrossAdditionsEur).max(BigDecimal.ZERO))

        val gross = money(
            salaryWithService
                .add(overtimeGross)
                .add(nightAdd)
                .add(secondShiftAdd)
                .add(turnusAdd)
                .add(saturdayAdd)
                .add(sundayAdd)
                .add(holidayAdd)
                .add(otherGross)
        )

        val pension = money(gross.multiply(PENSION_RATE))
        val incomeAfterPension = gross.subtract(pension)
        val allowance = money(bd(profile.personalAllowanceEur).max(BigDecimal.ZERO))
        val taxable = money(incomeAfterPension.subtract(allowance).max(BigDecimal.ZERO))

        val lowerRate = percent(profile.lowerTaxRatePercent)
        val higherRate = percent(profile.higherTaxRatePercent)
        val lowerPart = taxable.min(MONTHLY_HIGHER_RATE_THRESHOLD)
        val higherPart = taxable.subtract(MONTHLY_HIGHER_RATE_THRESHOLD).max(BigDecimal.ZERO)
        val tax = money(
            lowerPart.multiply(lowerRate)
                .add(higherPart.multiply(higherRate))
        )
        val net = money(gross.subtract(pension).subtract(tax))

        return SalaryResult(
            baseAmountEur = money(baseAmount),
            basicSalaryEur = basicSalary,
            serviceAddEur = serviceAdd,
            hourlyRateEur = money(hourlyRate),
            overtimeGrossEur = overtimeGross,
            nightAddEur = nightAdd,
            secondShiftAddEur = secondShiftAdd,
            turnusAddEur = turnusAdd,
            saturdayAddEur = saturdayAdd,
            sundayAddEur = sundayAdd,
            holidayAddEur = holidayAdd,
            otherGrossAdditionsEur = otherGross,
            grossEur = gross,
            pensionContributionsEur = pension,
            taxableIncomeEur = taxable,
            incomeTaxEur = tax,
            netEur = net
        )
    }

    private fun hoursMoney(rate: BigDecimal, minutes: Int, multiplier: BigDecimal): BigDecimal {
        if (minutes <= 0) return ZERO
        val hours = bd(minutes).divide(BigDecimal("60"), 8, RoundingMode.HALF_UP)
        return money(rate.multiply(hours).multiply(multiplier))
    }

    private fun percent(value: Double): BigDecimal =
        bd(value).divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP)

    private fun bd(value: Double): BigDecimal = BigDecimal.valueOf(value)
    private fun bd(value: Int): BigDecimal = BigDecimal.valueOf(value.toLong())
    private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}
