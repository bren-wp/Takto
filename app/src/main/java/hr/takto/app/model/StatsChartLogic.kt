package hr.takto.app.model

object StatsChartLogic {
    fun hasPositiveData(values: Collection<Int>): Boolean =
        values.any { it > 0 }

    fun compactDurationLabel(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val minutes = safe % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h${minutes}"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun barHeight(
        value: Int,
        maxValue: Int,
        maxHeight: Float = 120f,
        minVisibleHeight: Float = 4f
    ): Float {
        if (value <= 0 || maxValue <= 0 || maxHeight <= 0f) return 0f

        val safeMin = minVisibleHeight.coerceIn(0f, maxHeight)
        return (maxHeight * value.toFloat() / maxValue.toFloat())
            .coerceIn(safeMin, maxHeight)
    }
}
