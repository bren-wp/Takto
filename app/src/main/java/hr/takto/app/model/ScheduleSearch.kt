package hr.takto.app.model

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    private val hrLocale: Locale = Locale.forLanguageTag("hr")
    private val croatianNumericDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d.M.uuuu", hrLocale)
    private val croatianPaddedDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.uuuu", hrLocale)
    private val croatianLongDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM uuuu", hrLocale)
    private val croatianMonthYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM uuuu", hrLocale)

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
            "prekosutra", "preksutra" -> return if (entry.date == today.plusDays(2)) 500 else 0
            "jucer" -> return if (entry.date == today.minusDays(1)) 500 else 0
            "prekjucer" -> return if (entry.date == today.minusDays(2)) 500 else 0
            "buduce", "buduci" -> return if (!entry.date.isBefore(today)) 360 else 0
            "s vremenom", "radno vrijeme" -> return if (entry.hasWorkTime) 340 else 0
            "s napomenom" -> return if (entry.note.isNotBlank()) 340 else 0
        }

        val code = normalize(entry.code)
        val label = normalize(entry.label)
        val note = normalize(entry.note)
        val isoDate = entry.date.toString()
        val numericDate = entry.date.format(croatianNumericDate)
        val paddedDate = entry.date.format(croatianPaddedDate)
        val longDate = normalize(entry.date.format(croatianLongDate))
        val monthYear = normalize(entry.date.format(croatianMonthYear))
        val monthStandalone = normalize(entry.date.month.getDisplayName(TextStyle.FULL_STANDALONE, hrLocale))
        val weekday = normalize(entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, hrLocale))
        val weekdayShort = normalize(entry.date.dayOfWeek.getDisplayName(TextStyle.SHORT, hrLocale))
        val startTime = entry.startMinute?.let(ScheduleLogic::formatClock).orEmpty()
        val endTime = entry.endMinute?.let(ScheduleLogic::formatClock).orEmpty()
        val timeRange = if (startTime.isNotBlank() && endTime.isNotBlank()) "$startTime $endTime" else ""
        val duration = entry.workMinutes?.let(ScheduleLogic::formatDuration).orEmpty()

        val dateRepresentations = listOf(
            isoDate,
            numericDate,
            "$numericDate.",
            paddedDate,
            "$paddedDate.",
            longDate,
            monthYear,
            monthStandalone,
            weekday,
            weekdayShort
        ).filter { it.isNotBlank() }

        val searchable = listOf(
            code,
            label,
            note,
            *dateRepresentations.toTypedArray(),
            startTime,
            endTime,
            timeRange,
            duration
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        var score = 0
        when {
            code == query -> score += 240
            code.startsWith(query) -> score += 160
            code.contains(query) -> score += 115
        }
        when {
            label == query -> score += 215
            label.startsWith(query) -> score += 145
            label.contains(query) -> score += 105
        }
        when {
            note == query && note.isNotBlank() -> score += 120
            note.startsWith(query) && note.isNotBlank() -> score += 90
            note.contains(query) && note.isNotBlank() -> score += 70
        }

        val exactDate = dateRepresentations.any { it == query }
        if (exactDate) {
            score += 210
        } else if (dateRepresentations.any { it.contains(query) }) {
            score += 95
        }

        when {
            startTime == query || endTime == query -> score += 190
            timeRange.contains(query) && timeRange.isNotBlank() -> score += 120
            duration.contains(query) && duration.isNotBlank() -> score += 75
        }

        if (searchable.contains(query)) score += 35

        val terms = query.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.size > 1 && terms.all(searchable::contains)) {
            score += 60
            terms.forEach { term ->
                score += when {
                    code == term -> 30
                    label == term -> 25
                    label.contains(term) -> 15
                    note.contains(term) && note.isNotBlank() -> 10
                    dateRepresentations.any { it.contains(term) } -> 12
                    startTime.contains(term) || endTime.contains(term) -> 12
                    else -> 0
                }
            }
        }

        return score
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('đ', 'd')
            .replace(Regex("\\s+"), " ")
}
