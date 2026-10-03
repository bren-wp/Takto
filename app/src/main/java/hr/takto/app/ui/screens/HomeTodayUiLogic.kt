package hr.takto.app.ui.screens

/**
 * Čista logika za responzivni raspored i pristupačne opise kartice Danas.
 *
 * Nema Android/Compose ovisnosti pa se pravila mogu regresijski testirati.
 */
internal object HomeTodayUiLogic {
    private const val SINGLE_COLUMN_WIDTH_DP = 360f
    private const val LARGE_FONT_SCALE = 1.30f

    fun shouldStackMetrics(availableWidthDp: Float, fontScale: Float): Boolean {
        val safeWidth = availableWidthDp.takeIf { it.isFinite() } ?: 0f
        val safeFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        return safeWidth < 360f || safeFontScale >= 1.25f
    }

    fun quickActionColumns(availableWidthDp: Float, fontScale: Float): Int {
        val safeWidth = availableWidthDp.takeIf { it.isFinite() } ?: 0f
        val safeFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        return if (safeWidth < SINGLE_COLUMN_WIDTH_DP || safeFontScale >= LARGE_FONT_SCALE) 1 else 2
    }

    fun quickActionDescription(code: String, name: String): String =
        "Dodaj $name, oznaka $code, za danas"

    fun todayEntryDescription(
        dateText: String,
        code: String,
        label: String,
        startText: String?,
        endText: String?,
        durationText: String?,
        note: String
    ): String = buildList {
        add("Danas")
        add(dateText)
        add(label)
        add("oznaka $code")

        if (startText != null && endText != null && durationText != null) {
            add("radno vrijeme $startText do $endText")
            add("trajanje $durationText")
        } else {
            add("radno vrijeme nije upisano")
        }

        note.trim().takeIf { it.isNotEmpty() }?.let { add("napomena $it") }
    }.joinToString(", ")
}
