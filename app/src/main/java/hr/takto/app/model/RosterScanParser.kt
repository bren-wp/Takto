package hr.takto.app.model

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class ScannedScheduleItem(
    val date: LocalDate,
    val code: String,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val breakMinutes: Int = 0
)

data class ScheduleScanParseResult(
    val items: List<ScannedScheduleItem>,
    val detectedMonth: YearMonth?,
    val usedReferenceMonth: Boolean
)

object RosterScanParser {
    private val yearRegex = Regex("\\b(20\\d{2})\\b")
    private val numericMonthYearRegex = Regex("\\b(0?[1-9]|1[0-2])[./-](20\\d{2})\\b")
    private val fullDateRegex = Regex("\\b(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?\\.?\\b")
    private val dayRegex = Regex("^([0-3]?\\d)\\.?$")
    private val timeRangeRegex = Regex("(\\d{1,2}[:.]\\d{2})\\s*[-–]\\s*(\\d{1,2}[:.]\\d{2})")
    private val breakRegex = Regex("(?i)(?:pauza|break)\\s*[:=]?\\s*(\\d{1,3})")

    private val monthNames = mapOf(
        "sijecanj" to 1, "sij" to 1,
        "veljaca" to 2, "velj" to 2,
        "ozujak" to 3, "ozu" to 3,
        "travanj" to 4, "tra" to 4,
        "svibanj" to 5, "svi" to 5,
        "lipanj" to 6, "lip" to 6,
        "srpanj" to 7, "srp" to 7,
        "kolovoz" to 8, "kol" to 8,
        "rujan" to 9, "ruj" to 9,
        "listopad" to 10, "lis" to 10,
        "studeni" to 11, "stu" to 11,
        "prosinac" to 12, "pro" to 12
    )

    private val ignoredWords = setOf(
        "PON", "UTO", "SRI", "CET", "PET", "SUB", "NED",
        "PONEDJELJAK", "UTORAK", "SRIJEDA", "CETVRTAK", "PETAK", "SUBOTA", "NEDJELJA",
        "DATUM", "DAN", "SMJENA", "SMJENE", "RASPORED", "MJESEC", "MJESECA",
        "SAT", "SATI", "VRIJEME", "PAUZA"
    )

    fun parseForPerson(
        text: String,
        personHint: String,
        referenceDate: LocalDate = LocalDate.now()
    ): ScheduleScanParseResult {
        val hint = normalizeSearch(personHint).trim()
        if (hint.isBlank()) return parse(text, referenceDate)

        val lines = text
            .replace('|', ' ')
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return ScheduleScanParseResult(emptyList(), null, false)

        val hintTokens = hint.split(Regex("\\s+")).filter { it.length >= 2 }
        val matchedIndex = lines.indexOfFirst { line ->
            val normalized = normalizeSearch(line)
            hintTokens.isNotEmpty() && hintTokens.all { token -> normalized.contains(token) }
        }
        if (matchedIndex < 0) return ScheduleScanParseResult(emptyList(), null, false)

        val row = lines[matchedIndex]
        val cleanedRow = tokenize(row)
            .filterNot { token -> normalizeSearch(token) in hintTokens }
            .joinToString(" ")
            .trim()

        val header = lines
            .take(matchedIndex)
            .asReversed()
            .firstOrNull { candidate ->
                tokenize(candidate).count { dayRegex.matchEntire(it) != null } >= 3
            }
        val monthLine = lines.firstOrNull { line ->
            numericMonthYearRegex.containsMatchIn(line) ||
                monthNames.keys.any { name ->
                    Regex("\\b" + Regex.escape(name) + "\\b").containsMatchIn(normalizeSearch(line))
                }
        }

        val selectedText = listOfNotNull(monthLine, header, cleanedRow)
            .distinct()
            .joinToString("\n")
        return parse(selectedText, referenceDate)
    }

    fun parse(text: String, referenceDate: LocalDate = LocalDate.now()): ScheduleScanParseResult {
        if (text.isBlank()) return ScheduleScanParseResult(emptyList(), null, false)

        val lines = text
            .replace('|', ' ')
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val normalizedAll = normalizeSearch(text)
        val detectedYear = yearRegex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val numericMonthYear = numericMonthYearRegex.find(text)
        val detectedMonthNumber = numericMonthYear?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: monthNames.entries.firstOrNull { (name, _) ->
                Regex("\\b" + Regex.escape(name) + "\\b").containsMatchIn(normalizedAll)
            }?.value
        val monthYear = numericMonthYear?.groupValues?.getOrNull(2)?.toIntOrNull() ?: detectedYear
        val fullDates = fullDateRegex.findAll(text)
            .mapNotNull { match ->
                parseFullDateToken(match.value, detectedYear ?: referenceDate.year)
            }
            .toList()

        val detectedMonth = if (detectedMonthNumber != null && monthYear != null) {
            runCatching { YearMonth.of(monthYear, detectedMonthNumber) }.getOrNull()
        } else {
            fullDates.map(YearMonth::from).distinct().singleOrNull()
        }
        val fallbackMonth = detectedMonth ?: YearMonth.from(referenceDate)
        val usedReferenceMonth = detectedMonth == null

        val found = linkedMapOf<LocalDate, ScannedScheduleItem>()

        lines.forEach { line ->
            val time = parseTime(line)
            val tokens = tokenize(line)
            tokens.forEachIndexed { index, token ->
                val fullDate = parseFullDateToken(token, detectedYear ?: referenceDate.year)
                if (fullDate != null) {
                    val code = nextCode(tokens, index + 1)
                    if (code != null) {
                        found[fullDate] = ScannedScheduleItem(
                            date = fullDate,
                            code = code,
                            startMinute = time?.first,
                            endMinute = time?.second,
                            breakMinutes = parseBreak(line)
                        )
                    }
                    return@forEachIndexed
                }

                val day = dayRegex.matchEntire(token)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (day != null && day in 1..fallbackMonth.lengthOfMonth()) {
                    val code = nextCode(tokens, index + 1)
                    if (code != null) {
                        val date = fallbackMonth.atDay(day)
                        found[date] = ScannedScheduleItem(
                            date = date,
                            code = code,
                            startMinute = time?.first,
                            endMinute = time?.second,
                            breakMinutes = parseBreak(line)
                        )
                    }
                }
            }
        }

        lines.zipWithNext().forEach { (daysLine, codesLine) ->
            val days = tokenize(daysLine)
                .mapNotNull { dayRegex.matchEntire(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
                .filter { it in 1..fallbackMonth.lengthOfMonth() }
            val codes = tokenize(codesLine).mapNotNull(::normalizeCode)
            if (days.size >= 3 && codes.size >= days.size) {
                days.forEachIndexed { index, day ->
                    val date = fallbackMonth.atDay(day)
                    if (!found.containsKey(date)) {
                        found[date] = ScannedScheduleItem(date = date, code = codes[index])
                    }
                }
            }
        }

        return ScheduleScanParseResult(
            items = found.values.sortedBy { it.date },
            detectedMonth = detectedMonth,
            usedReferenceMonth = usedReferenceMonth
        )
    }

    private fun tokenize(line: String): List<String> =
        line.split(Regex("\\s+"))
            .map { it.trim(',', ';', ':', '(', ')', '[', ']') }
            .filter { it.isNotBlank() }

    private fun nextCode(tokens: List<String>, startIndex: Int): String? {
        val end = minOf(tokens.size, startIndex + 4)
        for (index in startIndex until end) {
            normalizeCode(tokens[index])?.let { return it }
        }
        return null
    }

    private fun normalizeCode(value: String): String? {
        val cleaned = value
            .trim()
            .trim('.', ',', ';', ':', '-', '–', '/', '\\', '(', ')', '[', ']')
        if (cleaned.isBlank() || cleaned.all(Char::isDigit)) return null

        val upper = ScheduleLogic.normalizeReusableCode(cleaned)
        if (upper.isBlank()) return null
        if (normalizeSearch(upper) in monthNames.keys) return null
        if (normalizeSearch(upper).uppercase(Locale.ROOT) in ignoredWords) return null
        if (!upper.any(Char::isLetter)) return null
        return upper
    }

    private fun parseFullDateToken(token: String, fallbackYear: Int): LocalDate? {
        val match = fullDateRegex.find(token) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val rawYear = match.groupValues.getOrNull(3).orEmpty()
        val year = when (rawYear.length) {
            2 -> 2000 + (rawYear.toIntOrNull() ?: return null)
            4 -> rawYear.toIntOrNull() ?: return null
            else -> fallbackYear
        }
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun parseTime(line: String): Pair<Int, Int>? {
        val match = timeRangeRegex.find(line) ?: return null
        val start = ScheduleLogic.parseClock(match.groupValues[1]) ?: return null
        val end = ScheduleLogic.parseClock(match.groupValues[2]) ?: return null
        return start to end
    }

    private fun parseBreak(line: String): Int =
        breakRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES)
            ?: 0

    private fun normalizeSearch(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace('č', 'c')
            .replace('ć', 'c')
            .replace('ž', 'z')
            .replace('š', 's')
            .replace('đ', 'd')
}
