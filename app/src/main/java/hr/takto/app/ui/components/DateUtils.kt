package hr.takto.app.ui.components

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val months = listOf(
    "Siječanj", "Veljača", "Ožujak", "Travanj", "Svibanj", "Lipanj",
    "Srpanj", "Kolovoz", "Rujan", "Listopad", "Studeni", "Prosinac"
)

private val monthsGenitive = listOf(
    "siječnja", "veljače", "ožujka", "travnja", "svibnja", "lipnja",
    "srpnja", "kolovoza", "rujna", "listopada", "studenoga", "prosinca"
)

val weekDayShort = listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")

fun monthTitle(month: YearMonth): String = "${months[month.monthValue - 1]} ${month.year}."

fun croatianDate(date: LocalDate): String =
    "${date.dayOfMonth}. ${monthsGenitive[date.monthValue - 1]} ${date.year}."

fun daysForMonthGrid(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val gridStart = first.minusDays(leading.toLong())
    return List(42) { index -> gridStart.plusDays(index.toLong()) }
}
