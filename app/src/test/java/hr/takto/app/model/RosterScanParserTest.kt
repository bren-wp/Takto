package hr.takto.app.model

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterScanParserTest {
    @Test
    fun parsesCroatianMonthAndDayCodeRows() {
        val result = RosterScanParser.parse(
            "LISTOPAD 2026\n1 J\n2 J\n3 SD\n4 GO",
            LocalDate.of(2026, 10, 2)
        )
        assertEquals(YearMonth.of(2026, 10), result.detectedMonth)
        assertFalse(result.usedReferenceMonth)
        assertEquals(
            listOf("J", "J", "SD", "GO"),
            result.items.map { it.code }
        )
    }

    @Test
    fun parsesFullDatesAndWorkTime() {
        val result = RosterScanParser.parse(
            "02.10.2026. D 07:00-15:00 pauza 30\n03.10.2026. N 22:00-06:00",
            LocalDate.of(2026, 1, 1)
        )
        assertEquals(2, result.items.size)
        assertEquals(7 * 60, result.items[0].startMinute)
        assertEquals(15 * 60, result.items[0].endMinute)
        assertEquals(30, result.items[0].breakMinutes)
        assertEquals(22 * 60, result.items[1].startMinute)
        assertEquals(6 * 60, result.items[1].endMinute)
    }

    @Test
    fun parsesSimpleHorizontalTable() {
        val result = RosterScanParser.parse(
            "10/2026\n1 2 3 4 5\nJ J N N GO",
            LocalDate.of(2026, 1, 1)
        )
        assertEquals(5, result.items.size)
        assertEquals(listOf("J", "J", "N", "N", "GO"), result.items.map { it.code })
    }

    @Test
    fun infersMonthFromExplicitDatesWithoutHeader() {
        val result = RosterScanParser.parse(
            "02.10.2026. D\n03.10.2026. N",
            LocalDate.of(2026, 1, 1)
        )
        assertEquals(YearMonth.of(2026, 10), result.detectedMonth)
        assertFalse(result.usedReferenceMonth)
    }

    @Test
    fun conflictingCodesForSameDateAreMarkedAmbiguous() {
        val result = RosterScanParser.parse(
            "LISTOPAD 2026\n2 D\n2 N\n3 D",
            LocalDate.of(2026, 10, 1)
        )
        assertEquals(1, result.ambiguousDateCount)
        assertEquals(listOf(3), result.items.map { it.date.dayOfMonth })
    }


    @Test
    fun horizontalTableIgnoresLeadingPersonNameAndPreservesFreeDay() {
        val result = RosterScanParser.parse(
            "LISTOPAD 2026\n1 2 3 4 5\nBRANKO HORVAT J J - N GO",
            LocalDate.of(2026, 10, 1)
        )

        assertEquals(5, result.items.size)
        assertEquals(
            listOf(
                "J",
                "J",
                RosterScanParser.FREE_DAY_CODE,
                "N",
                "GO"
            ),
            result.items.map { it.code }
        )
        assertEquals(listOf(1, 2, 3, 4, 5), result.items.map { it.date.dayOfMonth })
    }

    @Test
    fun lineBasedFreeDayIsRecognized() {
        val result = RosterScanParser.parse(
            "LISTOPAD 2026\n2 -\n3 D",
            LocalDate.of(2026, 10, 1)
        )

        assertEquals(RosterScanParser.FREE_DAY_CODE, result.items.first { it.date.dayOfMonth == 2 }.code)
        assertEquals("D", result.items.first { it.date.dayOfMonth == 3 }.code)
    }

    @Test
    fun isolatesSelectedPersonFromMultiPersonRoster() {
        val result = RosterScanParser.parseForPerson(
            "LISTOPAD 2026\n1 2 3 4 5\nANA HORVAT J J N N GO\nMARKO MARIC N N J J BO",
            "Ana Horvat",
            LocalDate.of(2026, 10, 1)
        )

        assertEquals(5, result.items.size)
        assertEquals(listOf("J", "J", "N", "N", "GO"), result.items.map { it.code })
        assertEquals(listOf(1, 2, 3, 4, 5), result.items.map { it.date.dayOfMonth })
    }

    @Test
    fun selectedPersonMatchingIgnoresCroatianDiacritics() {
        val result = RosterScanParser.parseForPerson(
            "LISTOPAD 2026\n1 2 3\nŽELJKO ČORIĆ D N -\nIVAN HORVAT N D D",
            "Željko Čorić",
            LocalDate.of(2026, 10, 1)
        )

        assertEquals(3, result.items.size)
        assertEquals(
            listOf("D", "N", RosterScanParser.FREE_DAY_CODE),
            result.items.map { it.code }
        )
    }

    @Test
    fun supportsNameOnOneLineAndCodesOnFollowingLine() {
        val result = RosterScanParser.parseForPerson(
            "LISTOPAD 2026\n1 2 3 4\nANA HORVAT\nJ J N GO\nMARKO MARIC\nN N D BO",
            "Ana Horvat",
            LocalDate.of(2026, 10, 1)
        )

        assertEquals(4, result.items.size)
        assertEquals(listOf("J", "J", "N", "GO"), result.items.map { it.code })
    }

    @Test
    fun missingPersonDoesNotImportAnotherEmployeesRoster() {
        val result = RosterScanParser.parseForPerson(
            "LISTOPAD 2026\n1 2 3\nANA HORVAT J J N\nMARKO MARIC N N D",
            "Ivana Novak",
            LocalDate.of(2026, 10, 1)
        )

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun reportsReferenceMonthFallback() {
        val result = RosterScanParser.parse("2 D\n3 N", LocalDate.of(2026, 11, 10))
        assertTrue(result.usedReferenceMonth)
        assertEquals(listOf(2, 3), result.items.map { it.date.dayOfMonth })
    }
}
