package hr.takto.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleRecoveryTest {
    private val date = LocalDate.of(2026, 10, 2)

    private fun entry(code: String, label: String = code) = ShiftEntry(
        date = date,
        code = code,
        label = label,
        colorArgb = 0xFF2488FF
    )

    @Test
    fun appliesRevisionWhenCurrentMatchesBefore() {
        val before = entry("D")
        val after = entry("EDU", "Edukacija")

        val result = ScheduleRecovery.resolve(
            current = before,
            before = before,
            after = after
        )

        assertTrue(result.shouldApply)
        assertEquals(after, result.next)
    }

    @Test
    fun appliesDeletionWhenCurrentMatchesBefore() {
        val before = entry("D")

        val result = ScheduleRecovery.resolve(
            current = before,
            before = before,
            after = null
        )

        assertTrue(result.shouldApply)
        assertNull(result.next)
    }

    @Test
    fun skipsRevisionWhenCurrentAlreadyMatchesAfter() {
        val before = entry("D")
        val after = entry("GO")

        val result = ScheduleRecovery.resolve(
            current = after,
            before = before,
            after = after
        )

        assertFalse(result.shouldApply)
        assertEquals(after, result.next)
    }

    @Test
    fun doesNotOverwriteDivergentNewerState() {
        val before = entry("D")
        val archiveAfter = entry("N")
        val newerCurrent = entry("TER", "Teren")

        val result = ScheduleRecovery.resolve(
            current = newerCurrent,
            before = before,
            after = archiveAfter
        )

        assertFalse(result.shouldApply)
        assertEquals(newerCurrent, result.next)
    }

    @Test
    fun canCreateEntryFromNullBaseline() {
        val after = entry("BO", "Bolovanje")

        val result = ScheduleRecovery.resolve(
            current = null,
            before = null,
            after = after
        )

        assertTrue(result.shouldApply)
        assertEquals(after, result.next)
    }
}
