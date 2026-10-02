package hr.takto.app.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import kotlin.math.max

enum class PayrollSystem(val persistedValue: String, val label: String) {
    STATE_SERVICE("state_service", "Državna služba"),
    PUBLIC_SERVICE("public_service", "Javne službe"),
    OTHER("other", "Ostali sustavi / vlastita pravila");

    companion object {
        fun fromPersisted(value: String?): PayrollSystem =
            entries.firstOrNull { it.persistedValue == value } ?: OTHER
    }
}

enum class PensionMode(val persistedValue: String, val label: String) {
    PILLAR_I_ONLY("pillar_i", "Samo I. stup"),
    PILLAR_I_AND_II("pillar_i_ii", "I. i II. stup");

    companion object {
        fun fromPersisted(value: String?): PensionMode =
            entries.firstOrNull { it.persistedValue == value } ?: PILLAR_I_AND_II
    }
}

data class PayrollProfile(
    val enabled: Boolean = false,
    val system: PayrollSystem = PayrollSystem.OTHER,
    val coefficient: Double = 0.0,
    val yearsOfService: Int = 0,
    val manualBaseEur: Double = 0.0,
    val lowerTaxRatePercent: Double = 0.0,
    val higherTaxRatePercent: Double = 0.0,
    val personalAllowanceEur: Double = 600.0,
    val pensionMode: PensionMode = PensionMode.PILLAR_I_AND_II,
    val additionalGrossEur: Double = 0.0,
    val nonTaxableEur: Double = 0.0
)

data class PayrollInputs(
    val month: YearMonth,
    val monthlyFundMinutes: Int,
    val workedMinutes: Int,
    val overtimeMinutes: Int,
    val nightMinutes: Int,
    val saturdayMinutes: Int,
    val sundayMinutes: Int,
    val holidayMinutes: Int,
    val untimedWorkEntryCount: Int = 0,
    val sickLeaveDayCount: Int = 0
)

data class PayrollBreakdown(
    val complete: Boolean,
    val missing: List<String>,
    val officialBaseEur: Double?,
    val baseSalaryEur: Double,
    val seniorityEur: Double,
    val hourlyRateEur: Double,
    val overtimePayEur: Double,
    val nightSupplementEur: Double,
    val saturdaySupplementEur: Double,
    val sundaySupplementEur: Double,
    val holidaySupplementEur: Double,
    val additionalGrossEur: Double,
    val grossEur: Double,
    val pensionBaseReductionEur: Double,
    val pensionContributionBaseEur: Double,
    val pensionPillarIEur: Double,
    val pensionPillarIIEur: Double,
    val taxableIncomeEur: Double,
    val incomeTaxEur: Double,
    val netSalaryEur: Double,
    val nonTaxableEur: Double,
    val payoutEur: Double
)

object CroatianPayrollRules2026 {
    const val OVERTIME_PERCENT = 50.0
    const val NIGHT_PERCENT = 40.0
    const val SATURDAY_PERCENT = 25.0
    const val SUNDAY_PERCENT = 50.0
    const val HOLIDAY_PERCENT = 150.0
    const val SENIORITY_PERCENT_PER_YEAR = 0.5
    const val BASIC_PERSONAL_ALLOWANCE_EUR = 600.0
    const val MONTHLY_LOWER_TAX_BAND_EUR = 5_000.0
    const val FULL_PENSION_BASE_REDUCTION_LIMIT_EUR = 700.0
    const val PENSION_BASE_REDUCTION_END_EUR = 1_300.0
    const val FULL_PENSION_BASE_REDUCTION_EUR = 300.0

    const val SOURCE_LABEL =
        "Zakon o plaćama NN 155/2023 · TKU javne službe NN 29/2024 · KU državna služba NN 29/2024 · osnovica 2026 NN 11/2026"

    fun officialBase(month: YearMonth, system: PayrollSystem): Double? {
        if (system !in setOf(PayrollSystem.STATE_SERVICE, PayrollSystem.PUBLIC_SERVICE)) return null
        if (month.year != 2026) return null
        return when (month.monthValue) {
            in 1..3 -> 1004.87
            in 4..7 -> 1015.00
            in 8..11 -> 1025.00
            12 -> 1035.00
            else -> null
        }
    }
}

object PayrollCalculator {
    fun calculate(profile: PayrollProfile, input: PayrollInputs): PayrollBreakdown {
        val missing = mutableListOf<String>()
        val officialBase = CroatianPayrollRules2026.officialBase(input.month, profile.system)
        val base = when (profile.system) {
            PayrollSystem.STATE_SERVICE, PayrollSystem.PUBLIC_SERVICE -> officialBase
            PayrollSystem.OTHER -> profile.manualBaseEur.takeIf { it > 0.0 }
        }

        if (!profile.enabled) missing += "obračun plaće nije uključen"
        if (base == null || base <= 0.0) missing += "osnovica"
        if (profile.coefficient <= 0.0) {
            missing += "koeficijent"
        } else if (
            profile.system in setOf(PayrollSystem.STATE_SERVICE, PayrollSystem.PUBLIC_SERVICE) &&
            profile.coefficient !in 1.0..8.0
        ) {
            missing += "koeficijent izvan dopuštenog raspona 1,00–8,00"
        }
        if (input.monthlyFundMinutes <= 0) missing += "mjesečni fond sati"
        if (input.untimedWorkEntryCount > 0) {
            missing += "radno vrijeme za ${input.untimedWorkEntryCount} radnih unosa"
        }
        if (input.sickLeaveDayCount > 0) {
            missing += "obračun naknade za bolovanje"
        }
        if (profile.lowerTaxRatePercent <= 0.0) missing += "niža stopa poreza"
        if (profile.higherTaxRatePercent <= 0.0) missing += "viša stopa poreza"
        if (
            profile.lowerTaxRatePercent > 0.0 &&
            profile.higherTaxRatePercent > 0.0 &&
            profile.higherTaxRatePercent < profile.lowerTaxRatePercent
        ) missing += "porezne stope nisu valjane"
        if (profile.personalAllowanceEur < 0.0) missing += "osobni odbitak"

        if (missing.isNotEmpty()) {
            return PayrollBreakdown(
                complete = false,
                missing = missing.distinct(),
                officialBaseEur = officialBase,
                baseSalaryEur = 0.0,
                seniorityEur = 0.0,
                hourlyRateEur = 0.0,
                overtimePayEur = 0.0,
                nightSupplementEur = 0.0,
                saturdaySupplementEur = 0.0,
                sundaySupplementEur = 0.0,
                holidaySupplementEur = 0.0,
                additionalGrossEur = money(profile.additionalGrossEur),
                grossEur = 0.0,
                pensionBaseReductionEur = 0.0,
                pensionContributionBaseEur = 0.0,
                pensionPillarIEur = 0.0,
                pensionPillarIIEur = 0.0,
                taxableIncomeEur = 0.0,
                incomeTaxEur = 0.0,
                netSalaryEur = 0.0,
                nonTaxableEur = money(profile.nonTaxableEur),
                payoutEur = 0.0
            )
        }

        val basic = bd(base!!) * bd(profile.coefficient)
        val seniority = basic * bd(profile.yearsOfService.coerceAtLeast(0)) * bd(0.005)
        val baseWithSeniority = basic + seniority
        val fundHours = bd(input.monthlyFundMinutes) / bd(60)
        val hourly = if (fundHours.signum() > 0) {
            baseWithSeniority.divide(fundHours, 12, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        fun hours(minutes: Int): BigDecimal = bd(minutes.coerceAtLeast(0)).divide(bd(60), 12, RoundingMode.HALF_UP)
        fun supplement(minutes: Int, percent: Double): BigDecimal =
            hourly * hours(minutes) * bd(percent / 100.0)

        // Osnovna mjesečna plaća pokriva redovni fond. Za prekovremene sate iznad
        // fonda dodaje se puna cijena dodatnog sata + 50 % uvećanja.
        val overtimePay = hourly * hours(input.overtimeMinutes) * bd(1.0 + CroatianPayrollRules2026.OVERTIME_PERCENT / 100.0)
        val night = supplement(input.nightMinutes, CroatianPayrollRules2026.NIGHT_PERCENT)
        val saturday = supplement(input.saturdayMinutes, CroatianPayrollRules2026.SATURDAY_PERCENT)
        val sunday = supplement(input.sundayMinutes, CroatianPayrollRules2026.SUNDAY_PERCENT)
        val holiday = supplement(input.holidayMinutes, CroatianPayrollRules2026.HOLIDAY_PERCENT)
        val extraGross = bd(profile.additionalGrossEur.coerceAtLeast(0.0))

        val gross = baseWithSeniority + overtimePay + night + saturday + sunday + holiday + extraGross

        val pensionBaseReduction = pensionBaseReduction(gross)
        val pensionContributionBase = (gross - pensionBaseReduction).max(BigDecimal.ZERO)
        val pillarI = if (profile.pensionMode == PensionMode.PILLAR_I_AND_II) {
            pensionContributionBase * bd(0.15)
        } else {
            pensionContributionBase * bd(0.20)
        }
        val pillarII = if (profile.pensionMode == PensionMode.PILLAR_I_AND_II) {
            pensionContributionBase * bd(0.05)
        } else {
            BigDecimal.ZERO
        }
        val incomeAfterPension = gross - pillarI - pillarII
        val taxable = (incomeAfterPension - bd(profile.personalAllowanceEur.coerceAtLeast(0.0))).max(BigDecimal.ZERO)
        val lowerBand = taxable.min(bd(CroatianPayrollRules2026.MONTHLY_LOWER_TAX_BAND_EUR))
        val higherBand = (taxable - lowerBand).max(BigDecimal.ZERO)
        val incomeTax =
            lowerBand * bd(profile.lowerTaxRatePercent / 100.0) +
                higherBand * bd(profile.higherTaxRatePercent / 100.0)
        val net = incomeAfterPension - incomeTax
        val nonTaxable = bd(profile.nonTaxableEur.coerceAtLeast(0.0))
        val payout = net + nonTaxable

        return PayrollBreakdown(
            complete = true,
            missing = emptyList(),
            officialBaseEur = officialBase,
            baseSalaryEur = money(basic),
            seniorityEur = money(seniority),
            hourlyRateEur = money(hourly),
            overtimePayEur = money(overtimePay),
            nightSupplementEur = money(night),
            saturdaySupplementEur = money(saturday),
            sundaySupplementEur = money(sunday),
            holidaySupplementEur = money(holiday),
            additionalGrossEur = money(extraGross),
            grossEur = money(gross),
            pensionBaseReductionEur = money(pensionBaseReduction),
            pensionContributionBaseEur = money(pensionContributionBase),
            pensionPillarIEur = money(pillarI),
            pensionPillarIIEur = money(pillarII),
            taxableIncomeEur = money(taxable),
            incomeTaxEur = money(incomeTax),
            netSalaryEur = money(net),
            nonTaxableEur = money(nonTaxable),
            payoutEur = money(payout)
        )
    }

    private fun pensionBaseReduction(gross: BigDecimal): BigDecimal {
        val grossValue = gross.toDouble()
        return when {
            grossValue <= 0.0 -> BigDecimal.ZERO
            grossValue <= CroatianPayrollRules2026.FULL_PENSION_BASE_REDUCTION_LIMIT_EUR ->
                bd(CroatianPayrollRules2026.FULL_PENSION_BASE_REDUCTION_EUR)
                    .min(gross)
            grossValue <= CroatianPayrollRules2026.PENSION_BASE_REDUCTION_END_EUR ->
                bd(0.5) * (
                    bd(CroatianPayrollRules2026.PENSION_BASE_REDUCTION_END_EUR) - gross
                )
            else -> BigDecimal.ZERO
        }.max(BigDecimal.ZERO)
    }

    private fun bd(value: Int): BigDecimal = BigDecimal.valueOf(value.toLong())
    private fun bd(value: Double): BigDecimal = BigDecimal.valueOf(value)
    private fun money(value: BigDecimal): Double = value.setScale(2, RoundingMode.HALF_UP).toDouble()
    private fun money(value: Double): Double = money(bd(max(0.0, value)))
}
