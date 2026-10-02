package hr.takto.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTodayUiLogicTest {
    @Test
    fun quickActionsUseSingleColumnOnNarrowScreens() {
        assertEquals(1, HomeTodayUiLogic.quickActionColumns(359f, 1f))
        assertEquals(2, HomeTodayUiLogic.quickActionColumns(360f, 1f))
    }

    @Test
    fun quickActionsUseSingleColumnForLargeFontScale() {
        assertEquals(1, HomeTodayUiLogic.quickActionColumns(420f, 1.30f))
        assertEquals(2, HomeTodayUiLogic.quickActionColumns(420f, 1.29f))
    }

    @Test
    fun invalidMeasurementsFallBackSafely() {
        assertEquals(1, HomeTodayUiLogic.quickActionColumns(Float.NaN, 1f))
        assertEquals(2, HomeTodayUiLogic.quickActionColumns(420f, Float.NaN))
    }

    @Test
    fun quickActionDescriptionNamesActionCodeAndLabel() {
        assertEquals(
            "Dodaj Godišnji odmor, oznaka GO, za danas",
            HomeTodayUiLogic.quickActionDescription("GO", "Godišnji odmor")
        )
    }

    @Test
    fun todayDescriptionIncludesWorkTimeDurationAndNote() {
        assertEquals(
            "Danas, petak, 2. listopada 2026., Ured, oznaka U, radno vrijeme 07:30 do 15:30, trajanje 7 h 30 min, napomena sastanak u 10",
            HomeTodayUiLogic.todayEntryDescription(
                dateText = "petak, 2. listopada 2026.",
                code = "U",
                label = "Ured",
                startText = "07:30",
                endText = "15:30",
                durationText = "7 h 30 min",
                note = "sastanak u 10"
            )
        )
    }

    @Test
    fun todayDescriptionExplainsMissingWorkTime() {
        assertEquals(
            "Danas, petak, 2. listopada 2026., Godišnji odmor, oznaka GO, radno vrijeme nije upisano",
            HomeTodayUiLogic.todayEntryDescription(
                dateText = "petak, 2. listopada 2026.",
                code = "GO",
                label = "Godišnji odmor",
                startText = null,
                endText = null,
                durationText = null,
                note = "  "
            )
        )
    }
}
