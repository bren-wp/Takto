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
    val rolePresetId: String = "",
    val coefficient: Double = 0.0,
    val yearsOfService: Int = 0,
    val manualBaseEur: Double = 0.0,
    val taxLocalityPresetId: String = "",
    val lowerTaxRatePercent: Double = 0.0,
    val higherTaxRatePercent: Double = 0.0,
    val personalAllowanceEur: Double = 600.0,
    val pensionMode: PensionMode = PensionMode.PILLAR_I_AND_II,
    val additionalGrossEur: Double = 0.0,
    val nonTaxableEur: Double = 0.0,
    val overtimePercent: Double = 0.0,
    val nightPercent: Double = 0.0,
    val saturdayPercent: Double = 0.0,
    val sundayPercent: Double = 0.0,
    val holidayPercent: Double = 0.0,
    val otherEmployersGrossEur: Double = 0.0,
    val allAdjustmentsConfirmed: Boolean = false
)

data class PayrollRolePreset(
    val id: String,
    val system: PayrollSystem,
    val sector: String,
    val label: String,
    val officialName: String,
    val code: String,
    val coefficient: Double
)

/**
 * Referentni katalog koeficijenata prenesen iz projekta bren-wp/RASPORED.
 *
 * Katalog služi za brži i manje pogrešan unos koeficijenta. Ne zamjenjuje službeni
 * akt poslodavca: ako konkretno radno mjesto ili posebni uvjeti odstupaju, korisnik
 * i dalje može ručno promijeniti koeficijent i dodatke.
 */
object PayrollRoleCatalog2026 {
    const val SOURCE_LABEL =
        "Katalog koeficijenata: bren-wp/RASPORED · izvori u tom projektu: NN 22/2024 i pripadajući službeni akti"

    val roles: List<PayrollRolePreset> = listOf(
        PayrollRolePreset("health-transport-sss", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Nosač bolesnika / transportni radnik — Radnik III. vrste", "Radnik III. vrste", "10.1.23", 1.25),
        PayrollRolePreset("health-transport-nss", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Nosač bolesnika / pomoćni radnik — posebni uvjeti", "Pomoćni radnik u sustavu s posebnim uvjetima rada", "10.1.24", 1.15),
        PayrollRolePreset("health-portir", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Portir / stručni radnik na tehničkom održavanju", "Stručni radnik na tehničkom održavanju", "10.1.20", 1.39),
        PayrollRolePreset("health-cleaner-special", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Spremač/čistač — posebni uvjeti", "Čistač – spremač u sustavu s posebnim uvjetima rada", "10.1.25", 1.15),
        PayrollRolePreset("health-cleaner", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Spremač/čistač", "Čistač – spremač", "10.1.26", 1.06),
        PayrollRolePreset("health-caregiver", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Njegovatelj / njegovateljica", "Njegovatelj", "16.15.1", 1.35),
        PayrollRolePreset("health-bolnicar", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Bolničar / bolničarka", "Bolničar", "16.15.2", 1.35),
        PayrollRolePreset("health-nurse-sss-1", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Medicinska sestra/tehničar — bolnica 1", "Medicinska sestra/medicinski tehničar / zdravstveni radnik u bolnici 1", "16.13.1", 1.78),
        PayrollRolePreset("health-nurse-sss-2", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Medicinska sestra/tehničar — bolnica 2", "Medicinska sestra/medicinski tehničar / zdravstveni radnik u bolnici 2", "16.13.2", 1.70),
        PayrollRolePreset("health-nurse-bacc-1", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Viša medicinska sestra / prvostupnik sestrinstva — bolnica 1", "Zdravstveni radnik prvostupnik u bolnici 1", "16.11.1", 1.95),
        PayrollRolePreset("health-nurse-bacc-2", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Viša medicinska sestra / prvostupnik sestrinstva — bolnica 2", "Zdravstveni radnik prvostupnik u bolnici 2", "16.11.2", 1.87),
        PayrollRolePreset("health-nurse-master", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Magistra sestrinstva / dipl. medicinska sestra — posebni poslovi", "Magistra sestrinstva/diplomirana medicinska sestra na propisanim posebnim poslovima", "16.10.1", 2.45),
        PayrollRolePreset("health-physio-1", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Viši fizioterapeut / prvostupnik fizioterapije — bolnica 1", "Zdravstveni radnik prvostupnik u bolnici 1", "16.11.1", 1.95),
        PayrollRolePreset("health-physio-2", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Viši fizioterapeut / prvostupnik fizioterapije — bolnica 2", "Zdravstveni radnik prvostupnik u bolnici 2", "16.11.2", 1.87),
        PayrollRolePreset("health-doctor-1", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine — 1", "Doktor medicine i doktor dentalne medicine 1", "16.4.3", 2.92),
        PayrollRolePreset("health-doctor-2", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine — 2", "Doktor medicine i doktor dentalne medicine 2", "16.4.6", 2.83),
        PayrollRolePreset("health-doctor-3", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine — 3", "Doktor medicine i doktor dentalne medicine 3", "16.4.9", 2.81),
        PayrollRolePreset("health-doctor-specialization", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine na specijalizaciji", "Doktor medicine/dentalne medicine na specijalizaciji", "16.4.10", 2.81),
        PayrollRolePreset("health-doctor-specialist-1", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine specijalist — 1", "Doktor medicine specijalist 1", "16.4.2", 3.82),
        PayrollRolePreset("health-doctor-specialist-2", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine specijalist — 2", "Doktor medicine specijalist 2", "16.4.5", 3.74),
        PayrollRolePreset("health-doctor-specialist-3", PayrollSystem.PUBLIC_SERVICE, "Zdravstvo", "Doktor medicine specijalist — 3", "Doktor medicine specijalist 3", "16.4.8", 3.65),

        PayrollRolePreset("edu-teacher", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Učitelj", "Učitelj", "12.5.22", 2.01),
        PayrollRolePreset("edu-secondary-teacher", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Nastavnik", "Nastavnik", "12.5.21", 2.01),
        PayrollRolePreset("edu-educator", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Odgajatelj u učeničkom domu", "Odgajatelj", "12.5.20", 2.01),
        PayrollRolePreset("edu-professional", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Stručni suradnik", "Stručni suradnik", "12.5.23", 2.01),
        PayrollRolePreset("edu-mentor", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Učitelj/nastavnik/odgajatelj — mentor", "Mentor", "12.5.15–18", 2.17),
        PayrollRolePreset("edu-adviser", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Učitelj/nastavnik/odgajatelj — savjetnik", "Savjetnik", "12.5.8–11", 2.38),
        PayrollRolePreset("edu-secretary-1", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Tajnik školske ustanove 1", "Tajnik školske ustanove 1", "12.5.25", 2.01),
        PayrollRolePreset("edu-secretary-2", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Tajnik školske ustanove 2", "Tajnik školske ustanove 2", "12.5.32", 1.77),
        PayrollRolePreset("edu-accounting-1", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Voditelj računovodstva u školi 1", "Voditelj računovodstva u školi 1", "12.5.26", 2.01),
        PayrollRolePreset("edu-night-watch", PayrollSystem.PUBLIC_SERVICE, "Školstvo i obrazovanje", "Noćni pazitelj u učeničkom domu", "Noćni pazitelj u učeničkom domu", "12.5.38", 1.30),

        PayrollRolePreset("state-senior-adviser", PayrollSystem.STATE_SERVICE, "Državna služba", "Viši savjetnik", "Viši savjetnik", "JRM", 2.10),
        PayrollRolePreset("state-associate", PayrollSystem.STATE_SERVICE, "Državna služba", "Suradnik", "Suradnik", "JRM", 1.80),
        PayrollRolePreset("state-senior-referent", PayrollSystem.STATE_SERVICE, "Državna služba", "Viši referent", "Viši referent", "JRM", 1.70),
        PayrollRolePreset("state-it-technician", PayrollSystem.STATE_SERVICE, "Državna služba", "Informatički tehničar", "Informatički tehničar", "JRM", 1.50),
        PayrollRolePreset("state-admin-secretary", PayrollSystem.STATE_SERVICE, "Državna služba", "Administrativni tajnik čelnika tijela", "Administrativni tajnik čelnika tijela", "JRM", 1.44),
        PayrollRolePreset("state-referent", PayrollSystem.STATE_SERVICE, "Državna služba", "Referent", "Referent", "JRM", 1.43),
        PayrollRolePreset("state-driver", PayrollSystem.STATE_SERVICE, "Državna služba", "Vozač", "Vozač", "JRM", 1.37),
        PayrollRolePreset("state-employee-iii", PayrollSystem.STATE_SERVICE, "Državna služba", "Namještenik III. vrste", "Namještenik – III. vrste", "JRM", 1.25),
        PayrollRolePreset("state-caretaker", PayrollSystem.STATE_SERVICE, "Državna služba", "Domar", "Domar", "JRM", 1.25),
        PayrollRolePreset("state-doorman", PayrollSystem.STATE_SERVICE, "Državna služba", "Portir", "Portir", "JRM", 1.06),
        PayrollRolePreset("state-cleaner", PayrollSystem.STATE_SERVICE, "Državna služba", "Spremač", "Spremač", "JRM", 1.06),

        PayrollRolePreset("police-station", PayrollSystem.STATE_SERVICE, "Policija", "Policijski službenik u policijskoj postaji", "Policijski službenik u policijskoj postaji", "MUP", 1.70),
        PayrollRolePreset("police-intervention", PayrollSystem.STATE_SERVICE, "Policija", "Policijski službenik interventne policije", "Policijski službenik interventne policije", "MUP", 1.70),
        PayrollRolePreset("police-contact", PayrollSystem.STATE_SERVICE, "Policija", "Kontakt policajac", "Kontakt policajac", "MUP", 1.70),
        PayrollRolePreset("police-patrol-lead", PayrollSystem.STATE_SERVICE, "Policija", "Vođa ophodnje u policijskoj postaji", "Vođa ophodnje u policijskoj postaji", "MUP", 1.65),
        PayrollRolePreset("police-border", PayrollSystem.STATE_SERVICE, "Policija", "Policijski službenik granične policije", "Policijski službenik granične policije", "MUP", 1.65),
        PayrollRolePreset("police-motorcycle", PayrollSystem.STATE_SERVICE, "Policija", "Policijski službenik — motociklist", "Policijski službenik – motociklist", "MUP", 1.75),
        PayrollRolePreset("police-dispatcher", PayrollSystem.STATE_SERVICE, "Policija", "Policijski službenik — dispečer", "Policijski službenik – dispečer", "MUP", 1.75)
    )

    fun rolesFor(system: PayrollSystem): List<PayrollRolePreset> =
        roles.filter { it.system == system }

    fun role(id: String?): PayrollRolePreset? =
        id?.takeIf { it.isNotBlank() }?.let { wanted -> roles.firstOrNull { it.id == wanted } }

    fun search(system: PayrollSystem, query: String, limit: Int = 8): List<PayrollRolePreset> {
        val needle = query.trim().lowercase()
        return rolesFor(system)
            .asSequence()
            .filter { role ->
                needle.isBlank() ||
                    role.label.lowercase().contains(needle) ||
                    role.officialName.lowercase().contains(needle) ||
                    role.sector.lowercase().contains(needle) ||
                    role.code.lowercase().contains(needle)
            }
            .take(limit.coerceIn(1, 20))
            .toList()
    }
}

data class PayrollTaxLocalityPreset(
    val id: String,
    val county: String,
    val name: String,
    val lowerRate: Double,
    val higherRate: Double,
    val source: String
)

object PayrollTaxCatalog2026 {
    const val SOURCE_LABEL = "Porezne stope 2026: katalog bren-wp/RASPORED"

    val localities: List<PayrollTaxLocalityPreset> = listOf(
        PayrollTaxLocalityPreset("zagreb", "Grad Zagreb", "Zagreb", 23.0, 33.0, "NN 28/2025"),
        PayrollTaxLocalityPreset("rijeka", "Primorsko-goranska", "Rijeka", 20.0, 25.0, "NN 149/2025"),
        PayrollTaxLocalityPreset("split", "Splitsko-dalmatinska", "Split", 21.5, 32.0, "RRiF 2026 / NN 35/2025"),
        PayrollTaxLocalityPreset("osijek", "Osječko-baranjska", "Osijek", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("zadar", "Zadarska", "Zadar", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("pazin", "Istarska", "Pazin", 22.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("pula", "Istarska", "Pula", 22.0, 32.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("karlovac", "Karlovačka", "Karlovac", 19.0, 29.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("varazdin", "Varaždinska", "Varaždin", 21.0, 32.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("sibenik", "Šibensko-kninska", "Šibenik", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("sisak", "Sisačko-moslavačka", "Sisak", 21.6, 31.6, "RRiF 2026"),
        PayrollTaxLocalityPreset("dubrovnik", "Dubrovačko-neretvanska", "Dubrovnik", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("cakovec", "Međimurska", "Čakovec", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("bjelovar", "Bjelovarsko-bilogorska", "Bjelovar", 18.0, 25.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("gospic", "Ličko-senjska", "Gospić", 22.0, 32.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("virovitica", "Virovitičko-podravska", "Virovitica", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("pozega", "Požeško-slavonska", "Požega", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("slavonski-brod", "Brodsko-posavska", "Slavonski Brod", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("vukovar", "Vukovarsko-srijemska", "Vukovar", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("krapina", "Krapinsko-zagorska", "Krapina", 20.0, 30.0, "RRiF 2026"),
        PayrollTaxLocalityPreset("koprivnica", "Koprivničko-križevačka", "Koprivnica", 20.0, 30.0, "RRiF 2026")
    )

    fun locality(id: String?): PayrollTaxLocalityPreset? =
        id?.takeIf { it.isNotBlank() }?.let { wanted -> localities.firstOrNull { it.id == wanted } }

    fun search(query: String, limit: Int = 6): List<PayrollTaxLocalityPreset> {
        val needle = query.trim().lowercase()
        return localities.asSequence()
            .filter { item ->
                needle.isBlank() ||
                    item.name.lowercase().contains(needle) ||
                    item.county.lowercase().contains(needle)
            }
            .take(limit.coerceIn(1, 20))
            .toList()
    }
}

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
        "Zakon o plaćama NN 155/2023 · TKU javne službe NN 29/2024 · državna služba NN 29/2024, 11/2026 i 84/2026"

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
        if (!profile.allAdjustmentsConfirmed) {
            missing += "potvrda svih dodataka i naknada"
        }

        if (profile.system == PayrollSystem.OTHER) {
            if (input.overtimeMinutes > 0 && profile.overtimePercent <= 0.0) {
                missing += "postotak dodatka za prekovremeni rad"
            }
            if (input.nightMinutes > 0 && profile.nightPercent <= 0.0) {
                missing += "postotak dodatka za noćni rad"
            }
            if (input.saturdayMinutes > 0 && profile.saturdayPercent <= 0.0) {
                missing += "postotak dodatka za subotu"
            }
            if (input.sundayMinutes > 0 && profile.sundayPercent <= 0.0) {
                missing += "postotak dodatka za nedjelju"
            }
            if (input.holidayMinutes > 0 && profile.holidayPercent <= 0.0) {
                missing += "postotak dodatka za blagdan"
            }
        }

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

        // Osnovna mjesečna plaća pokriva redovni fond. Za potvrđene prekovremene
        // sate dodaje se puna cijena dodatnog sata i primjenjivo uvećanje.
        val overtimePercent = if (profile.system == PayrollSystem.OTHER) {
            profile.overtimePercent
        } else {
            CroatianPayrollRules2026.OVERTIME_PERCENT
        }
        val nightPercent = if (profile.system == PayrollSystem.OTHER) {
            profile.nightPercent
        } else {
            CroatianPayrollRules2026.NIGHT_PERCENT
        }
        val saturdayPercent = if (profile.system == PayrollSystem.OTHER) {
            profile.saturdayPercent
        } else {
            CroatianPayrollRules2026.SATURDAY_PERCENT
        }
        val sundayPercent = if (profile.system == PayrollSystem.OTHER) {
            profile.sundayPercent
        } else {
            CroatianPayrollRules2026.SUNDAY_PERCENT
        }
        val holidayPercent = if (profile.system == PayrollSystem.OTHER) {
            profile.holidayPercent
        } else {
            CroatianPayrollRules2026.HOLIDAY_PERCENT
        }

        val overtimePay = hourly * hours(input.overtimeMinutes) * bd(1.0 + overtimePercent / 100.0)
        val night = supplement(input.nightMinutes, nightPercent)
        val saturday = supplement(input.saturdayMinutes, saturdayPercent)
        val sunday = supplement(input.sundayMinutes, sundayPercent)
        val holiday = supplement(input.holidayMinutes, holidayPercent)
        val extraGross = bd(profile.additionalGrossEur.coerceAtLeast(0.0))

        val gross = baseWithSeniority + overtimePay + night + saturday + sunday + holiday + extraGross

        val pensionBaseReduction = pensionBaseReduction(
            gross = gross,
            otherEmployersGross = bd(profile.otherEmployersGrossEur.coerceAtLeast(0.0))
        )
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

    private fun pensionBaseReduction(
        gross: BigDecimal,
        otherEmployersGross: BigDecimal
    ): BigDecimal {
        val totalGross = (gross + otherEmployersGross).max(BigDecimal.ZERO)
        val totalGrossValue = totalGross.toDouble()
        val totalReduction = when {
            totalGrossValue <= 0.0 -> BigDecimal.ZERO
            totalGrossValue <= CroatianPayrollRules2026.FULL_PENSION_BASE_REDUCTION_LIMIT_EUR ->
                bd(CroatianPayrollRules2026.FULL_PENSION_BASE_REDUCTION_EUR)
                    .min(totalGross)
            totalGrossValue <= CroatianPayrollRules2026.PENSION_BASE_REDUCTION_END_EUR ->
                bd(0.5) * (
                    bd(CroatianPayrollRules2026.PENSION_BASE_REDUCTION_END_EUR) - totalGross
                )
            else -> BigDecimal.ZERO
        }.max(BigDecimal.ZERO)

        if (totalReduction.signum() <= 0 || otherEmployersGross.signum() <= 0) {
            return totalReduction.min(gross)
        }
        if (totalGross.signum() <= 0) return BigDecimal.ZERO
        val ownShare = gross.divide(totalGross, 12, RoundingMode.HALF_UP)
        return (totalReduction * ownShare).min(gross).max(BigDecimal.ZERO)
    }

    private fun bd(value: Int): BigDecimal = BigDecimal.valueOf(value.toLong())
    private fun bd(value: Double): BigDecimal = BigDecimal.valueOf(value)
    private fun money(value: BigDecimal): Double = value.setScale(2, RoundingMode.HALF_UP).toDouble()
    private fun money(value: Double): Double = money(bd(max(0.0, value)))
}
