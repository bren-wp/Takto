package hr.takto.app.model

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

enum class ScheduleSearchFilter {
    ALL,
    TODAY,
    UPCOMING,
    WITH_WORK_TIME,
    WITH_NOTE
}

object ScheduleSearch {
    private val croatianNumericDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d.M.uuuu", Locale.forLanguageTag("hr"))
    private val croatianPaddedDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.forLanguageTag("hr"))

    fun search(
        entries: Collection<ShiftEntry>,
        query: String,
        filter: ScheduleSearchFilter = ScheduleSearchFilter.ALL,
        today: LocalDate = LocalDate.now(),
        limit: Int = 30
    ): List<ShiftEntry> {
        val normalizedQuery = normalize(query)
        val safeLimit = limit.coerceIn(1, 100)

        return entries.asSequence()
            .filter { entry -> matchesFilter(entry, filter, today) }
            .map { entry -> entry to score(entry, normalizedQuery, today) }
            .filter { (_, score) -> normalizedQuery.isBlank() || score > 0 }
            .sortedWith(
                compareByDescending<Pair<ShiftEntry, Int>> { it.second }
                    .thenBy { (entry, _) -> abs(ChronoUnit.DAYS.between(today, entry.date)) }
                    .thenBy { (entry, _) -> entry.date }
            )
            .take(safeLimit)
            .map { it.first }
            .toList()
    }

    private fun matchesFilter(
        entry: ShiftEntry,
        filter: ScheduleSearchFilter,
        today: LocalDate
    ): Boolean = when (filter) {
        ScheduleSearchFilter.ALL -> true
        ScheduleSearchFilter.TODAY -> entry.date == today
        ScheduleSearchFilter.UPCOMING -> !entry.date.isBefore(today)
        ScheduleSearchFilter.WITH_WORK_TIME -> entry.hasWorkTime
        ScheduleSearchFilter.WITH_NOTE -> entry.note.isNotBlank()
    }

    private fun score(entry: ShiftEntry, query: String, today: LocalDate): Int {
        if (query.isBlank()) return 0

        when (query) {
            "danas" -> return if (entry.date == today) 500 else 0
            "sutra" -> return if (entry.date == today.plusDays(1)) 500 else 0
            "jucer" -> return if (entry.date == today.minusDays(1)) 500 else 0
        }

        val code = normalize(entry.code)
        val label = normalize(entry.label)
        val note = normalize(entry.note)
        val isoDate = entry.date.toString()
        val numericDate = entry.date.format(croatianNumericDate)
        val paddedDate = entry.date.format(croatianPaddedDate)
        val searchable = listOf(
            code,
            label,
            note,
            isoDate,
            numericDate,
            "$numericDate.",
            paddedDate,
            "$paddedDate."
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        var score = 0
        when {
            code == query -> score += 220
            code.startsWith(query) -> score += 150
            code.contains(query) -> score += 110
        }
        when {
            label == query -> score += 200
            label.startsWith(query) -> score += 140
            label.contains(query) -> score += 100
        }
        if (note.contains(query) && note.isNotBlank()) score += 70
        if (
            isoDate == query ||
            numericDate == query ||
            "$numericDate." == query ||
            paddedDate == query ||
            "$paddedDate." == query
        ) {
            score += 210
        } else if (
            isoDate.contains(query) ||
            numericDate.contains(query) ||
            paddedDate.contains(query)
        ) {
            score += 90
        }
        if (searchable.contains(query)) score += 35

        val terms = query.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.size > 1 && terms.all(searchable::contains)) score += 60

        return score
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('đ', 'd')
            .replace(Regex("\\s+"), " ")
}
