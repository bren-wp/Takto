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
    val usedReferenceMonth: Boolean,
    val ambiguousDateCount: Int = 0
)

object RosterScanParser {
    const val FREE_DAY_CODE = "__TAKTO_FREE_DAY__"

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
        val hintTokens = normalizeSearch(personHint)
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
        if (hintTokens.isEmpty()) return parse(text, referenceDate)

        val lines = text
            .replace('|', ' ')
            .lineSequence()
            .map(String::trim)
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return ScheduleScanParseResult(emptyList(), null, false)

        data class PersonCandidate(
            val index: Int,
            val line: String,
            val matchedTokens: Int,
            val surnameMatched: Boolean
        ) {
            val score: Int
                get() = matchedTokens + if (surnameMatched) 3 else 0
        }

        val surname = hintTokens.last()
        val candidates = lines.mapIndexedNotNull { index, line ->
            val normalized = normalizeSearch(line)
            val words = normalized.split(Regex("\\s+")).filter(String::isNotBlank)
            val matched = hintTokens.count { token -> token in words }
            val surnameMatched = surname in words
            val enoughNameEvidence =
                matched == hintTokens.size ||
                    surnameMatched ||
                    (hintTokens.size >= 3 && matched >= hintTokens.size - 1)
            if (enoughNameEvidence) {
                PersonCandidate(index, line, matched, surnameMatched)
            } else {
                null
            }
        }

        val bestScore = candidates.maxOfOrNull { it.score }
        val best = candidates.filter { it.score == bestScore }
        if (best.size != 1) {
            val full = parse(text, referenceDate)
            val potentialRows = countPotentialHorizontalScheduleRows(lines, referenceDate.year)
            if (
                best.isEmpty() &&
                potentialRows <= 1 &&
                full.items.isNotEmpty() &&
                full.ambiguousDateCount == 0
            ) {
                return full
            }
            return ScheduleScanParseResult(
                items = emptyList(),
                detectedMonth = full.detectedMonth,
                usedReferenceMonth = full.usedReferenceMonth,
                ambiguousDateCount = maxOf(full.ambiguousDateCount, if (potentialRows > 1) 1 else 0)
            )
        }

        val selected = best.single()
        val header = lines
            .take(selected.index)
            .asReversed()
            .firstOrNull { candidate ->
                val tokens = tokenize(candidate)
                tokens.count { dayRegex.matchEntire(it) != null } >= 3 ||
                    tokens.count { parseFullDateToken(it, referenceDate.year) != null } >= 3
            }

        val monthLine = lines.firstOrNull { line ->
            numericMonthYearRegex.containsMatchIn(line) ||
                monthNames.keys.any { name ->
                    Regex("\\b" + Regex.escape(name) + "\\b")
                        .containsMatchIn(normalizeSearch(line))
                }
        }

        val cleanedCurrent = removePersonTokens(selected.line, hintTokens)
        val currentScheduleCount = tokenize(cleanedCurrent).count { normalizeScheduleToken(it) != null }
        val expectedDayCount = header
            ?.let(::tokenize)
            ?.count { token ->
                dayRegex.matchEntire(token) != null ||
                    parseFullDateToken(token, referenceDate.year) != null
            }
            ?: 0
        val continuation = if (currentScheduleCount == 0 && expectedDayCount >= 3) {
            lines.getOrNull(selected.index + 1)
                ?.takeIf { line ->
                    val tokens = tokenize(line)
                    val scheduleCount = tokens.count { normalizeScheduleToken(it) != null }
                    tokens.size in expectedDayCount..(expectedDayCount + 1) &&
                        scheduleCount >= expectedDayCount
                }
        } else {
            null
        }

        val selectedRow = listOfNotNull(cleanedCurrent, continuation)
            .joinToString(" ")
            .trim()

        val selectedText = listOfNotNull(monthLine, header, selectedRow.takeIf { it.isNotBlank() })
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
        val ambiguousDates = mutableSetOf<LocalDate>()

        fun addCandidate(item: ScannedScheduleItem) {
            if (item.date in ambiguousDates) return
            val existing = found[item.date]
            if (existing == null) {
                found[item.date] = item
                return
            }
            val same = existing.code.equals(item.code, ignoreCase = true) &&
                existing.startMinute == item.startMinute &&
                existing.endMinute == item.endMinute
            if (!same) {
                found.remove(item.date)
                ambiguousDates += item.date
            }
        }

        lines.forEach { line ->
            val time = parseTime(line)
            val tokens = tokenize(line)
            tokens.forEachIndexed { index, token ->
                val fullDate = parseFullDateToken(token, detectedYear ?: referenceDate.year)
                if (fullDate != null) {
                    val code = nextCode(tokens, index + 1)
                    if (code != null) {
                        addCandidate(
                            ScannedScheduleItem(
                                date = fullDate,
                                code = code,
                                startMinute = time?.first,
                                endMinute = time?.second,
                                breakMinutes = parseBreak(line)
                            )
                        )
                    }
                    return@forEachIndexed
                }

                val day = dayRegex.matchEntire(token)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (day != null && day in 1..fallbackMonth.lengthOfMonth()) {
                    val code = nextCode(tokens, index + 1)
                    if (code != null) {
                        val date = fallbackMonth.atDay(day)
                        addCandidate(
                            ScannedScheduleItem(
                                date = date,
                                code = code,
                                startMinute = time?.first,
                                endMinute = time?.second,
                                breakMinutes = parseBreak(line)
                            )
                        )
                    }
                }
            }
        }

        lines.zipWithNext().forEach { (daysLine, codesLine) ->
            val days = tokenize(daysLine)
                .mapNotNull { dayRegex.matchEntire(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
                .filter { it in 1..fallbackMonth.lengthOfMonth() }
            val scheduleTokens = tokenize(codesLine)
                .mapNotNull(::normalizeScheduleToken)
            val alignedCodes = if (scheduleTokens.size >= days.size) {
                scheduleTokens.takeLast(days.size)
            } else {
                emptyList()
            }
            if (days.size >= 3 && alignedCodes.size == days.size) {
                days.forEachIndexed { index, day ->
                    val date = fallbackMonth.atDay(day)
                    addCandidate(ScannedScheduleItem(date = date, code = alignedCodes[index]))
                }
            }
        }

        return ScheduleScanParseResult(
            items = found.values.sortedBy { it.date },
            detectedMonth = detectedMonth,
            usedReferenceMonth = usedReferenceMonth,
            ambiguousDateCount = ambiguousDates.size
        )
    }

    private fun countPotentialHorizontalScheduleRows(
        lines: List<String>,
        fallbackYear: Int
    ): Int {
        val headerIndex = lines.indexOfFirst { line ->
            val tokens = tokenize(line)
            tokens.count { token ->
                dayRegex.matchEntire(token) != null ||
                    parseFullDateToken(token, fallbackYear) != null
            } >= 3
        }
        if (headerIndex < 0) return 0

        val expectedDayCount = tokenize(lines[headerIndex]).count { token ->
            dayRegex.matchEntire(token) != null ||
                parseFullDateToken(token, fallbackYear) != null
        }
        if (expectedDayCount < 3) return 0

        return lines
            .drop(headerIndex + 1)
            .count { line ->
                tokenize(line).count { normalizeScheduleToken(it) != null } >= expectedDayCount
            }
    }

    private fun removePersonTokens(line: String, hintTokens: List<String>): String =
        tokenize(line)
            .filterNot { token -> normalizeSearch(token) in hintTokens }
            .joinToString(" ")

    private fun tokenize(line: String): List<String> =
        line.split(Regex("\\s+"))
            .map { it.trim(',', ';', ':', '(', ')', '[', ']') }
            .filter { it.isNotBlank() }

    private fun nextCode(tokens: List<String>, startIndex: Int): String? {
        val end = minOf(tokens.size, startIndex + 4)
        for (index in startIndex until end) {
            normalizeScheduleToken(tokens[index])?.let { return it }
        }
        return null
    }

    private fun normalizeScheduleToken(value: String): String? {
        val raw = value.trim().trim(',', ';', ':', '(', ')', '[', ']')
        val normalized = normalizeSearch(raw)
            .trim('.', ',', ';', ':', '/', '\\', '(', ')', '[', ']')
            .uppercase(Locale.ROOT)
        if (
            raw == "-" || raw == "–" || raw == "—" ||
            normalized in setOf("SLOBODNO", "SLOB", "OFF")
        ) {
            return FREE_DAY_CODE
        }
        return normalizeCode(value)
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
