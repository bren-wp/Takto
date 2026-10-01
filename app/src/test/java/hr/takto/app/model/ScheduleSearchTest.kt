package hr.takto.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleSearchTest {
    private val today = LocalDate.of(2026, 10, 2)

    private val entries = listOf(
        ShiftEntry(
            date = today,
            code = "D",
            label = "Dan",
            colorArgb = 0xFF2488FF,
            note = "Ured Rijeka",
            startMinute = 7 * 60,
            endMinute = 15 * 60
        ),
        ShiftEntry(
            date = today.plusDays(1),
            code = "GO",
            label = "Godišnji odmor",
            colorArgb = 0xFF13D7A0
        ),
        ShiftEntry(
            date = today.plusDays(3),
            code = "TER",
            label = "Teren",
            colorArgb = 0xFF22B8CF,
            note = "Rijeka centar"
        ),
        ShiftEntry(
            date = today.minusDays(2),
            code = "EDU",
            label = "Edukacija",
            colorArgb = 0xFF8B46F6,
            note = "Interna edukacija"
        )
    )

    @Test
    fun search_isAccentInsensitive() {
        val result = ScheduleSearch.search(entries, "godisnji", today = today)
        assertEquals("GO", result.first().code)
    }

    @Test
    fun exactCodeRanksBeforeLooseTextMatches() {
        val result = ScheduleSearch.search(entries, "D", today = today)
        assertEquals("D", result.first().code)
    }

    @Test
    fun multipleTermsCanMatchAcrossLabelAndNote() {
        val result = ScheduleSearch.search(entries, "teren rijeka", today = today)
        assertEquals("TER", result.first().code)
    }

    @Test
    fun relativeDayQueriesResolveAgainstProvidedToday() {
        assertEquals(today, ScheduleSearch.search(entries, "danas", today = today).single().date)
        assertEquals(today.plusDays(1), ScheduleSearch.search(entries, "sutra", today = today).single().date)
    }

    @Test
    fun search_acceptsPaddedCroatianDateWithTrailingDot() {
        val result = ScheduleSearch.search(entries, "02.10.2026.", today = today)
        assertEquals(today, result.first().date)
    }

    @Test
    fun filtersWorkWithoutTextQuery() {
        val timed = ScheduleSearch.search(
            entries = entries,
            query = "",
            filter = ScheduleSearchFilter.WITH_WORK_TIME,
            today = today
        )
        assertEquals(listOf("D"), timed.map { it.code })

        val upcoming = ScheduleSearch.search(
            entries = entries,
            query = "",
            filter = ScheduleSearchFilter.UPCOMING,
            today = today
        )
        assertTrue(upcoming.all { !it.date.isBefore(today) })
    }

    @Test
    fun noteFilterOnlyReturnsEntriesWithNotes() {
        val result = ScheduleSearch.search(
            entries = entries,
            query = "",
            filter = ScheduleSearchFilter.WITH_NOTE,
            today = today
        )
        assertEquals(setOf("D", "TER", "EDU"), result.map { it.code }.toSet())
    }
}
