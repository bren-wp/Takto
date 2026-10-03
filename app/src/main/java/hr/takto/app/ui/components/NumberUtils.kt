package hr.takto.app.ui.components

import java.util.Locale

fun formatEuro(value: Double): String =
    String.format(Locale("hr", "HR"), "%,.2f €", value)
