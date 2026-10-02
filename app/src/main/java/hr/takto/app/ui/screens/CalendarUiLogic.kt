package hr.takto.app.ui.screens

/**
 * Čista responzivna pravila za interaktivne kontrole Kalendara.
 *
 * Pravila su izdvojena iz Composea kako bi se mogla regresijski testirati.
 */
internal object CalendarUiLogic {
    private const val SINGLE_COLUMN_WIDTH_DP = 360f
    private const val LARGE_FONT_SCALE = 1.30f

    fun shouldStack(availableWidthDp: Float, fontScale: Float): Boolean {
        val safeWidth = availableWidthDp.takeIf { it.isFinite() } ?: 0f
        val safeFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        return safeWidth < SINGLE_COLUMN_WIDTH_DP || safeFontScale >= LARGE_FONT_SCALE
    }

    fun shiftChoiceColumns(availableWidthDp: Float, fontScale: Float): Int =
        if (shouldStack(availableWidthDp, fontScale)) 1 else 2
}
