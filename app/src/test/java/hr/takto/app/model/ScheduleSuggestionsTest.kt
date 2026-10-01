package hr.takto.app.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScheduleSuggestionsTest {
    private val today = LocalDate.of(2026, 10, 2)

    @Test
    fun recentUsageCanOutrankOlderFrequentUsage() {
        val older = ShiftType("A", "Stariji", Color.Blue)
        val recent = ShiftType("B", "Nedavni", Color.Green)
        val entries = buildList {
            repeat(6) { index ->
                add(
                    ShiftEntry(
                        date = today.minusDays((50 + index).toLong()),
                        code = "A",
                        label = "Stariji",
                        colorArgb = 0xFF0000FF
                    )
                )
            }
            add(
                ShiftEntry(
                    date = today.minusDays(1),
                    code = "B",
                    label = "Nedavni",
                    colorArgb = 0xFF00FF00
                )
            )
        }

        val ranked = ScheduleSuggestions.rank(
            types = listOf(older, recent),
            entries = entries,
            referenceDate = today
        )

        assertEquals("B", ranked.first().code)
    }

    @Test
    fun customTypeWinsTieAgainstUnusedPreset() {
        val preset = ShiftType("D", "Dan", Color.Blue, isPreset = true)
        val custom = ShiftType("EDU", "Edukacija", Color.Green, isPreset = false)

        val ranked = ScheduleSuggestions.rank(
            types = listOf(preset, custom),
            entries = emptyList(),
            referenceDate = today
        )

        assertEquals("EDU", ranked.first().code)
    }

    @Test
    fun mostRecentlyUsedBreaksEqualScoreTie() {
        val first = ShiftType("A", "A", Color.Blue)
        val second = ShiftType("B", "B", Color.Green)
        val entries = listOf(
            ShiftEntry(today.minusDays(10), "A", "A", 0xFF0000FF),
            ShiftEntry(today.minusDays(9), "B", "B", 0xFF00FF00)
        )

        val ranked = ScheduleSuggestions.rank(
            types = listOf(first, second),
            entries = entries,
            referenceDate = today
        )

        assertEquals("B", ranked.first().code)
    }
}
