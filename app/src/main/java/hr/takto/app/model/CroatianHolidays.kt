package hr.takto.app.model

import java.time.LocalDate

/**
 * Državni i javni blagdani u Republici Hrvatskoj koji utječu na automatski fond sati.
 * Pomični blagdani računaju se iz datuma Uskrsa za svaku godinu.
 */
object CroatianHolidays {
    fun datesForYear(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)
        return buildSet {
            add(LocalDate.of(year, 1, 1))   // Nova godina
            add(LocalDate.of(year, 1, 6))   // Bogojavljenje
            add(easter)                      // Uskrs
            add(easter.plusDays(1))          // Uskrsni ponedjeljak
            add(LocalDate.of(year, 5, 1))    // Praznik rada
            add(LocalDate.of(year, 5, 30))   // Dan državnosti
            add(easter.plusDays(60))         // Tijelovo
            add(LocalDate.of(year, 6, 22))   // Dan antifašističke borbe
            add(LocalDate.of(year, 8, 5))    // Dan pobjede i domovinske zahvalnosti
            add(LocalDate.of(year, 8, 15))   // Velika Gospa
            add(LocalDate.of(year, 11, 1))   // Svi sveti
            add(LocalDate.of(year, 11, 18))  // Dan sjećanja
            add(LocalDate.of(year, 12, 25))  // Božić
            add(LocalDate.of(year, 12, 26))  // Sveti Stjepan
        }
    }

    fun isHoliday(date: LocalDate): Boolean = date in datesForYear(date.year)

    // Meeus/Jones/Butcher algoritam za gregorijanski kalendar.
    private fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
