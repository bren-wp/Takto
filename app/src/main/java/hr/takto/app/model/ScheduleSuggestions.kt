package hr.takto.app.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

object ScheduleSuggestions {
    fun rank(
        types: List<ShiftType>,
        entries: Collection<ShiftEntry>,
        referenceDate: LocalDate,
        lookbackDays: Long = 90L
    ): List<ShiftType> {
        if (types.size <= 1) return types

        val windowStart = referenceDate.minusDays(lookbackDays.coerceAtLeast(1L))
        val scoreByCode = mutableMapOf<String, Int>()
        val lastUsedByCode = mutableMapOf<String, LocalDate>()

        entries.asSequence()
            .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(referenceDate) }
            .forEach { entry ->
                val code = entry.code.uppercase(Locale.ROOT)
                val age = ChronoUnit.DAYS.between(entry.date, referenceDate).coerceAtLeast(0L)
                val weight = when {
                    age <= 7 -> 8
                    age <= 30 -> 4
                    else -> 1
                }
                scoreByCode[code] = (scoreByCode[code] ?: 0) + weight
                val previous = lastUsedByCode[code]
                if (previous == null || entry.date.isAfter(previous)) {
                    lastUsedByCode[code] = entry.date
                }
            }

        return types.sortedWith(
            compareByDescending<ShiftType> { scoreByCode[it.code.uppercase(Locale.ROOT)] ?: 0 }
                .thenByDescending {
                    lastUsedByCode[it.code.uppercase(Locale.ROOT)]?.toEpochDay() ?: Long.MIN_VALUE
                }
                .thenBy { if (it.isPreset) 1 else 0 }
                .thenBy { it.name.lowercase(Locale.forLanguageTag("hr")) }
        )
    }
}
