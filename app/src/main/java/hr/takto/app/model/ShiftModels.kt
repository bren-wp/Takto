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
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val breakMinutes: Int = 0,
    val overtimeMinutes: Int = 0
) {
    val color: Color get() = Color(colorArgb)
    val hasWorkTime: Boolean get() = startMinute != null && endMinute != null
    val workMinutes: Int? get() = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)
}

object DefaultShiftTypes {
    // D i N su namjerno prvi: na novoj instalaciji postaju dvije početne brze oznake.
    // Svaka ugrađena oznaka ima vlastitu, dovoljno udaljenu boju radi brzog čitanja kalendara.
    val day = ShiftType("D", "Dnevna smjena", Color(0xFF2563EB))
    val night = ShiftType("N", "Noćna smjena", Color(0xFF7C3AED))
    val morning = ShiftType("J", "Jutarnja smjena", Color(0xFF0891B2))
    val annual = ShiftType("GO", "Godišnji odmor", Color(0xFF16A34A))
    val free = ShiftType("SD", "Slobodan dan", Color(0xFF64748B))
    val sick = ShiftType("BO", "Bolovanje", Color(0xFFF59E0B))
    val paid = ShiftType("PD", "Plaćeni dopust", Color(0xFFDC2626))
    val presets = listOf(day, night, morning, annual, free, sick, paid)
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
