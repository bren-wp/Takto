package hr.takto.app.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class ShiftType(
    val code: String,
    val name: String,
    val color: Color,
    val isPreset: Boolean = true
)

data class ShiftEntry(
    val date: LocalDate,
    val code: String,
    val label: String,
    val colorArgb: Long,
    val note: String = "",
    /** Minute od 00:00. null znači da radno vrijeme nije zadano. */
    val startMinute: Int? = null,
    /** Minute od 00:00. Vrijednost <= početku znači završetak sljedeći dan. */
    val endMinute: Int? = null,
    val breakMinutes: Int = 0
) {
    val color: Color get() = Color(colorArgb)
    val hasWorkTime: Boolean get() = startMinute != null && endMinute != null
    val workMinutes: Int? get() = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)
}

object DefaultShiftTypes {
    val day = ShiftType("D", "Dan", Color(0xFF2488FF))
    val night = ShiftType("N", "Noć", Color(0xFF8B46F6))
    val annual = ShiftType("GO", "Godišnji odmor", Color(0xFF13D7A0))
    val sick = ShiftType("BO", "Bolovanje", Color(0xFFFFB21D))
    val paid = ShiftType("PD", "Plaćeni dopust", Color(0xFFFF4B55))

    val presets = listOf(day, night, annual, sick, paid)
}

data class CustomShiftPreset(
    val code: String,
    val name: String,
    val colorArgb: Long
)

data class SavedPattern(
    val id: String,
    val name: String,
    val codes: List<String?>
)


data class WorkTimePreset(
    val code: String,
    val startMinute: Int,
    val endMinute: Int,
    val breakMinutes: Int = 0
) {
    val durationMinutes: Int?
        get() = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)
}

