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
        ),
        ShiftEntry(
            date = today.plusDays(5),
            code = "MO",
            label = "Međuodjel",
            colorArgb = 0xFF64748B
        )
    )

    @Test
    fun search_isAccentInsensitive() {
        val result = ScheduleSearch.search(entries, "godisnji", today = today)
        assertEquals("GO", result.first().code)
    }

    @Test
    fun search_normalizesCroatianDStroke() {
        val result = ScheduleSearch.search(entries, "meduodjel", today = today)
        assertEquals("MO", result.first().code)
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

    @Test
    fun search_acceptsCroatianMonthNameAndYear() {
        val result = ScheduleSearch.search(entries, "listopad 2026", today = today)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.date.year == 2026 && it.date.monthValue == 10 })
    }

    @Test
    fun search_acceptsCroatianWeekdayName() {
        val result = ScheduleSearch.search(entries, "petak", today = today)
        assertEquals(today, result.first().date)
    }

    @Test
    fun search_findsExactWorkTime() {
        val result = ScheduleSearch.search(entries, "07:00", today = today)
        assertEquals(listOf("D"), result.map { it.code })
    }

    @Test
    fun typedSemanticAliasesApplyUsefulLocalFilters() {
        val future = ScheduleSearch.search(entries, "buduće", today = today)
        assertTrue(future.isNotEmpty())
        assertTrue(future.all { !it.date.isBefore(today) })

        val timed = ScheduleSearch.search(entries, "radno vrijeme", today = today)
        assertEquals(listOf("D"), timed.map { it.code })

        val noted = ScheduleSearch.search(entries, "s napomenom", today = today)
        assertEquals(setOf("D", "TER", "EDU"), noted.map { it.code }.toSet())
    }

    @Test
    fun extendedRelativeDayQueriesAreSupported() {
        val dayAfterTomorrow = ShiftEntry(
            date = today.plusDays(2),
            code = "A",
            label = "Administracija",
            colorArgb = 0xFF2488FF
        )
        val result = ScheduleSearch.search(entries + dayAfterTomorrow, "prekosutra", today = today)
        assertEquals(today.plusDays(2), result.single().date)

        val yesterday = ShiftEntry(
            date = today.minusDays(1),
            code = "B",
            label = "Obveza",
            colorArgb = 0xFF64748B
        )
        assertEquals(
            today.minusDays(1),
            ScheduleSearch.search(entries + yesterday, "jučer", today = today).single().date
        )
    }
}
