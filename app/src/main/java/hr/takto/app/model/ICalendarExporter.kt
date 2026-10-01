package hr.takto.app.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Minimalan, potpuno offline iCalendar (RFC 5545) izvoz Takto rasporeda.
 *
 * Smjene s početkom i krajem izvoze se kao vremenski događaji. GO/BO/PD i
 * ostali unosi bez radnog vremena izvoze se kao cjelodnevni događaji.
 * Vrijeme smjene namjerno je "floating" lokalno vrijeme kako uvezeni kalendar
 * ne bi neočekivano pomicao 07:00 na 08:00 zbog druge vremenske zone.
 */
object ICalendarExporter {
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT)
    private val stampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT)
        .withZone(ZoneOffset.UTC)

    fun export(
        entries: Collection<ShiftEntry>,
        generatedAt: Instant = Instant.now(),
        calendarName: String = "Takto raspored"
    ): String {
        val stamp = stampFormatter.format(generatedAt)
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Takto//Raspored rada//HR",
            "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH",
            "X-WR-CALNAME:${escapeText(calendarName)}"
        )

        entries.sortedBy { it.date }.forEach { entry ->
            lines += "BEGIN:VEVENT"
            lines += "UID:${entry.date.toString().replace("-", "")}@takto.local"
            lines += "DTSTAMP:$stamp"
            lines += "SUMMARY:${escapeText(summary(entry))}"

            if (entry.hasWorkTime) {
                val startMinute = entry.startMinute ?: 0
                val endMinute = entry.endMinute ?: startMinute
                val start = entry.date.atStartOfDay().plusMinutes(startMinute.toLong())
                var end = entry.date.atStartOfDay().plusMinutes(endMinute.toLong())
                if (!end.isAfter(start)) end = end.plusDays(1)
                lines += "DTSTART:${dateTimeFormatter.format(start)}"
                lines += "DTEND:${dateTimeFormatter.format(end)}"
            } else {
                lines += "DTSTART;VALUE=DATE:${dateFormatter.format(entry.date)}"
                lines += "DTEND;VALUE=DATE:${dateFormatter.format(entry.date.plusDays(1))}"
            }

            val description = buildDescription(entry)
            if (description.isNotBlank()) lines += "DESCRIPTION:${escapeText(description)}"
            lines += "CATEGORIES:${escapeText("Takto")},${escapeText(entry.code)}"
            lines += "END:VEVENT"
        }

        lines += "END:VCALENDAR"
        return lines.joinToString("\r\n") { foldLine(it) } + "\r\n"
    }

    private fun summary(entry: ShiftEntry): String = when {
        entry.label.isBlank() || entry.label.equals(entry.code, ignoreCase = true) -> entry.code
        else -> "${entry.code} · ${entry.label}"
    }

    private fun buildDescription(entry: ShiftEntry): String = buildString {
        if (entry.hasWorkTime) {
            append("Radno vrijeme: ")
            append(ScheduleLogic.formatClock(entry.startMinute))
            append("–")
            append(ScheduleLogic.formatClock(entry.endMinute))
            if (entry.breakMinutes > 0) append(" · pauza ${entry.breakMinutes} min")
        }
        if (entry.note.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append("Napomena: ").append(entry.note)
        }
    }

    private fun escapeText(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    private fun foldLine(line: String): String {
        if (line.toByteArray(Charsets.UTF_8).size <= 75) return line
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        var bytes = 0
        var index = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val text = String(Character.toChars(codePoint))
            val charBytes = text.toByteArray(Charsets.UTF_8).size
            val limit = if (chunks.isEmpty()) 75 else 74
            if (bytes + charBytes > limit && current.isNotEmpty()) {
                chunks += current.toString()
                current.clear()
                bytes = 0
            }
            current.append(text)
            bytes += charBytes
            index += Character.charCount(codePoint)
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks.joinToString("\r\n ")
    }
}
