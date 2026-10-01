package hr.takto.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateUtilsTest {
    @Test
    fun monthGridAlwaysHasSixWeeks() {
        val days = daysForMonthGrid(YearMonth.of(2026, 10))
        assertEquals(42, days.size)
        assertEquals(LocalDate.of(2026, 9, 28), days.first())
        assertEquals(LocalDate.of(2026, 11, 8), days.last())
    }

    @Test
    fun monthGridContainsWholeMonth() {
        val month = YearMonth.of(2026, 2)
        val days = daysForMonthGrid(month)
        assertTrue(days.contains(LocalDate.of(2026, 2, 1)))
        assertTrue(days.contains(LocalDate.of(2026, 2, 28)))
    }

    @Test
    fun croatianTitlesAreStable() {
        assertEquals("Ožujak 2026.", monthTitle(YearMonth.of(2026, 3)))
        assertEquals("1. listopada 2026.", croatianDate(LocalDate.of(2026, 10, 1)))
    }
}
