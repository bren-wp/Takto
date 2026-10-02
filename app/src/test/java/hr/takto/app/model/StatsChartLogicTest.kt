package hr.takto.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsChartLogicTest {
    @Test
    fun zeroValuesNeverProduceFakeBars() {
        assertEquals(0f, StatsChartLogic.barHeight(value = 0, maxValue = 10), 0f)
        assertEquals(0f, StatsChartLogic.barHeight(value = -1, maxValue = 10), 0f)
    }

    @Test
    fun positiveValuesRemainVisibleAndBounded() {
        assertEquals(4f, StatsChartLogic.barHeight(value = 1, maxValue = 100), 0f)
        assertEquals(60f, StatsChartLogic.barHeight(value = 50, maxValue = 100), 0.001f)
        assertEquals(120f, StatsChartLogic.barHeight(value = 100, maxValue = 100), 0.001f)
    }

    @Test
    fun detectsWhetherChartHasRealData() {
        assertFalse(StatsChartLogic.hasPositiveData(listOf(0, 0, 0)))
        assertTrue(StatsChartLogic.hasPositiveData(listOf(0, 2, 0)))
    }
    @Test
    fun compactDurationKeepsSubHourMinutes() {
        assertEquals("30m", StatsChartLogic.compactDurationLabel(30))
        assertEquals("1h", StatsChartLogic.compactDurationLabel(60))
        assertEquals("1h30", StatsChartLogic.compactDurationLabel(90))
    }
}
