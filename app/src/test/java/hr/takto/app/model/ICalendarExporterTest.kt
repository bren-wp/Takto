package hr.takto.app.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ICalendarExporterTest {
    @Test
    fun `timed overnight shift crosses midnight`() {
        val entry = ShiftEntry(
            date = LocalDate.of(2026, 10, 3),
            code = "N",
            label = "Noć",
            colorArgb = 0xFF8B46F6L,
            startMinute = 19 * 60,
            endMinute = 7 * 60,
            breakMinutes = 30
        )
        val ics = ICalendarExporter.export(listOf(entry), Instant.parse("2026-10-01T00:00:00Z"))
        assertTrue(ics.contains("DTSTART:20261003T190000"))
        assertTrue(ics.contains("DTEND:20261004T070000"))
        assertTrue(ics.contains("SUMMARY:N · Noć"))
        assertTrue(ics.contains("pauza 30 min"))
    }

    @Test
    fun `leave without work time is all day`() {
        val entry = ShiftEntry(
            date = LocalDate.of(2026, 10, 4),
            code = "GO",
            label = "Godišnji odmor",
            colorArgb = 0xFF13D7A0L
        )
        val ics = ICalendarExporter.export(listOf(entry), Instant.EPOCH)
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261004"))
        assertTrue(ics.contains("DTEND;VALUE=DATE:20261005"))
        assertFalse(ics.contains("DTSTART:20261004T"))
    }

    @Test
    fun `special characters are escaped`() {
        val entry = ShiftEntry(
            date = LocalDate.of(2026, 10, 5),
            code = "TEREN",
            label = "Teren, centar",
            colorArgb = 0xFF22B8CFL,
            note = "Ulaz A; nazovi\\portu"
        )
        val ics = ICalendarExporter.export(listOf(entry), Instant.EPOCH)
        assertTrue(ics.contains("SUMMARY:TEREN · Teren\\, centar"))
        assertTrue(ics.contains("Napomena: Ulaz A\\; nazovi\\\\portu"))
    }
}
