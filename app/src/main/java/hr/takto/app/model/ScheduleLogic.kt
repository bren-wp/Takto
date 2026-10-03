package hr.takto.app.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Čista logika koja ne ovisi o Androidu/Composeu pa se može testirati zasebno.
 */
object ScheduleLogic {
    const val MAX_REUSABLE_CODE_LENGTH = 12
    const val MAX_REUSABLE_NAME_LENGTH = 40
    const val MAX_PATTERN_NAME_LENGTH = 48
    const val MAX_PATTERN_STEPS = 28
    const val MAX_MULTI_SELECT_DAYS = 366
    const val MINUTES_PER_DAY = 24 * 60
    const val MAX_BREAK_MINUTES = 12 * 60
    const val NIGHT_START_MINUTE = 22 * 60
    const val NIGHT_END_MINUTE = 6 * 60
    const val MAX_MONTHLY_TARGET_MINUTES = 400 * 60

    private val freeTokens = setOf("-", "SLOBODNO")

    fun normalizeReusableCode(value: String): String =
        normalizeWhitespace(value)
            .uppercase(Locale.ROOT)
            .take(MAX_REUSABLE_CODE_LENGTH)

    fun normalizeDisplayName(value: String): String =
        normalizeWhitespace(value).take(MAX_REUSABLE_NAME_LENGTH)

    fun normalizePatternName(value: String): String =
        normalizeWhitespace(value).take(MAX_PATTERN_NAME_LENGTH)

    fun parsePatternSequence(value: String): List<String?> {
        if (value.isBlank()) return emptyList()
        val rawTokens = value
            .replace('\n', ',')
            .replace(';', ',')
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_PATTERN_STEPS)

        return rawTokens.map { token ->
            val canonical = token.uppercase(Locale.ROOT)
            if (canonical in freeTokens) null else normalizeReusableCode(token).takeIf { it.isNotBlank() }
        }
    }

    fun patternSequenceText(codes: List<String?>): String =
        codes.take(MAX_PATTERN_STEPS).joinToString(", ") { it ?: "-" }

    fun startOfWeek(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - 1).toLong())

    fun weekDates(date: LocalDate): List<LocalDate> {
        val start = startOfWeek(date)
        return List(7) { index -> start.plusDays(index.toLong()) }
    }

    fun monthDates(month: YearMonth): List<LocalDate> =
        (1..month.lengthOfMonth()).map(month::atDay)

    fun monthWeekdays(month: YearMonth): List<LocalDate> =
        monthDates(month).filter { it.dayOfWeek.value in 1..5 }

    fun datesInclusive(start: LocalDate, endInclusive: LocalDate): List<LocalDate> {
        if (endInclusive.isBefore(start)) return emptyList()
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, endInclusive) + 1
        return List(days.coerceAtMost(MAX_MULTI_SELECT_DAYS.toLong()).toInt()) { index ->
            start.plusDays(index.toLong())
        }
    }

    fun parseClock(value: String): Int? {
        val clean = value
            .trim()
            .replace('.', ':')
            .replace(',', ':')
            .replace(" ", "")
        if (clean.isBlank()) return null

        if (clean.all(Char::isDigit)) {
            val (hourText, minuteText) = when (clean.length) {
                1, 2 -> clean to "0"
                3 -> clean.take(1) to clean.takeLast(2)
                4 -> clean.take(2) to clean.takeLast(2)
                else -> return null
            }
            val hour = hourText.toIntOrNull() ?: return null
            val minute = minuteText.toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }

        val parts = clean.split(':')
        if (parts.size != 2 || parts.any { it.isBlank() }) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    fun formatClock(minutes: Int?): String {
        if (minutes == null || minutes !in 0 until MINUTES_PER_DAY) return "—"
        return "%02d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60)
    }

    fun grossWorkDurationMinutes(startMinute: Int?, endMinute: Int?): Int? {
        val start = startMinute?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
        val end = endMinute?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
        return when {
            end > start -> end - start
            end < start -> (MINUTES_PER_DAY - start) + end
            else -> 0
        }
    }

    fun isOvernightWork(startMinute: Int?, endMinute: Int?): Boolean {
        val start = startMinute?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return false
        val end = endMinute?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return false
        return end < start
    }

    fun isValidWorkTime(startMinute: Int?, endMinute: Int?, breakMinutes: Int = 0): Boolean {
        if (breakMinutes !in 0..MAX_BREAK_MINUTES) return false
        val gross = grossWorkDurationMinutes(startMinute, endMinute) ?: return false
        return gross > 0 && breakMinutes < gross
    }

    fun workDurationMinutes(startMinute: Int?, endMinute: Int?, breakMinutes: Int = 0): Int? {
        val gross = grossWorkDurationMinutes(startMinute, endMinute) ?: return null
        val safeBreak = breakMinutes.coerceIn(0, minOf(MAX_BREAK_MINUTES, gross))
        return (gross - safeBreak).coerceAtLeast(0)
    }

    fun formatDuration(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val minutes = safe % 60
        return when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            else -> "$minutes min"
        }
    }

    fun overtimeMinutes(workMinutes: Int, standardDailyMinutes: Int): Int =
        (workMinutes - standardDailyMinutes.coerceAtLeast(0)).coerceAtLeast(0)

    fun regularWorkMinutes(totalWorkMinutes: Int, confirmedOvertimeMinutes: Int): Int =
        (totalWorkMinutes.coerceAtLeast(0) - confirmedOvertimeMinutes.coerceAtLeast(0))
            .coerceAtLeast(0)

    fun automaticMonthlyTargetMinutes(month: YearMonth, standardDailyMinutes: Int): Int =
        monthWeekdays(month)
            .count { !CroatianHolidays.isHoliday(it) } * standardDailyMinutes.coerceAtLeast(0)

    fun nightWorkMinutes(
        startMinute: Int?,
        endMinute: Int?,
        breakMinutes: Int = 0
    ): Int {
        val span = absoluteShiftSpan(startMinute, endMinute) ?: return 0
        val gross = span.second - span.first
        if (gross <= 0) return 0
        val overlap = listOf(
            -120 to NIGHT_END_MINUTE,
            NIGHT_START_MINUTE to (MINUTES_PER_DAY + NIGHT_END_MINUTE),
            (MINUTES_PER_DAY + NIGHT_START_MINUTE) to (2 * MINUTES_PER_DAY + NIGHT_END_MINUTE)
        ).sumOf { (windowStart, windowEnd) ->
            overlapMinutes(span.first, span.second, windowStart, windowEnd)
        }
        return proportionalNetMinutes(overlap, gross, breakMinutes)
    }

    fun weekendWorkMinutes(
        date: LocalDate,
        startMinute: Int?,
        endMinute: Int?,
        breakMinutes: Int = 0
    ): Int = dateCategoryWorkMinutes(date, startMinute, endMinute, breakMinutes) { day ->
        day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
    }

    fun saturdayWorkMinutes(
        date: LocalDate,
        startMinute: Int?,
        endMinute: Int?,
        breakMinutes: Int = 0
    ): Int = dateCategoryWorkMinutes(date, startMinute, endMinute, breakMinutes) {
        it.dayOfWeek == DayOfWeek.SATURDAY
    }

    fun sundayWorkMinutes(
        date: LocalDate,
        startMinute: Int?,
        endMinute: Int?,
        breakMinutes: Int = 0
    ): Int = dateCategoryWorkMinutes(date, startMinute, endMinute, breakMinutes) {
        it.dayOfWeek == DayOfWeek.SUNDAY
    }

    fun holidayWorkMinutes(
        date: LocalDate,
        startMinute: Int?,
        endMinute: Int?,
        breakMinutes: Int = 0
    ): Int = dateCategoryWorkMinutes(date, startMinute, endMinute, breakMinutes, CroatianHolidays::isHoliday)

    private fun dateCategoryWorkMinutes(
        date: LocalDate,
        startMinute: Int?,
        endMinute: Int?,
        breakMinutes: Int,
        predicate: (LocalDate) -> Boolean
    ): Int {
        val span = absoluteShiftSpan(startMinute, endMinute) ?: return 0
        val gross = span.second - span.first
        if (gross <= 0) return 0
        var overlap = 0
        for (dayOffset in 0..1) {
            val segmentStart = dayOffset * MINUTES_PER_DAY
            val segmentEnd = segmentStart + MINUTES_PER_DAY
            if (predicate(date.plusDays(dayOffset.toLong()))) {
                overlap += overlapMinutes(span.first, span.second, segmentStart, segmentEnd)
            }
        }
        return proportionalNetMinutes(overlap, gross, breakMinutes)
    }

    private fun absoluteShiftSpan(startMinute: Int?, endMinute: Int?): Pair<Int, Int>? {
        val start = startMinute?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
        val end = endMinute?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
        if (start == end) return null
        val absoluteEnd = if (end > start) end else MINUTES_PER_DAY + end
        return start to absoluteEnd
    }

    private fun overlapMinutes(start: Int, end: Int, windowStart: Int, windowEnd: Int): Int =
        (minOf(end, windowEnd) - maxOf(start, windowStart)).coerceAtLeast(0)

    private fun proportionalNetMinutes(overlap: Int, gross: Int, breakMinutes: Int): Int {
        if (overlap <= 0 || gross <= 0) return 0
        val safeBreak = breakMinutes.coerceIn(0, minOf(MAX_BREAK_MINUTES, gross))
        val net = gross - safeBreak
        return (overlap * (net.toDouble() / gross.toDouble())).roundToInt().coerceIn(0, net)
    }

    fun isLeaveCode(code: String): Boolean =
        code.equals("GO", true) ||
            code.equals("BO", true) ||
            code.equals("PD", true) ||
            code.equals("SD", true)

    private fun normalizeWhitespace(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")
}
