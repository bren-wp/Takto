package hr.takto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.model.ShiftEntry
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthCalendar(
    month: YearMonth,
    entries: Map<LocalDate, ShiftEntry>,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    selectedDates: Set<LocalDate> = emptySet(),
    compact: Boolean = false,
    onDayClick: (LocalDate) -> Unit
) {
    val days = daysForMonthGrid(month)
    val today = LocalDate.now()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayShort.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (compact) 9.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
        repeat(6) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 3.dp)
            ) {
                repeat(7) { colIndex ->
                    val date = days[rowIndex * 7 + colIndex]
                    val inCurrentMonth = YearMonth.from(date) == month
                    Box(modifier = Modifier.weight(1f)) {
                        CalendarDayCell(
                            date = date,
                            entry = entries[date],
                            isSelected = date == selectedDate || date in selectedDates,
                            today = date == today,
                            enabled = inCurrentMonth,
                            compact = compact,
                            onClick = { if (inCurrentMonth) onDayClick(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    entry: ShiftEntry?,
    isSelected: Boolean,
    today: Boolean,
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 8.dp else 13.dp)
    val colors = MaterialTheme.colorScheme
    val bg = when {
        !enabled -> colors.surface.copy(alpha = 0.32f)
        entry != null -> entry.color
        else -> colors.surfaceVariant
    }
    val borderColor = when {
        isSelected -> TaktoBlue
        today && enabled -> TaktoCyan.copy(alpha = 0.82f)
        enabled -> colors.outline.copy(alpha = 0.72f)
        else -> colors.outlineVariant.copy(alpha = 0.20f)
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 0.93f else 0.70f)
            .background(bg, shape)
            .border(borderWidth, borderColor, shape)
            .semantics {
                selected = isSelected
                contentDescription = when {
                    !enabled -> "${croatianDate(date)}, izvan odabranog mjeseca"
                    entry == null -> "${croatianDate(date)}, nema unosa"
                    else -> buildString {
                        append(croatianDate(date)).append(", ").append(entry.code).append(", ").append(entry.label)
                        if (entry.hasWorkTime) {
                            append(", radno vrijeme ")
                                .append(ScheduleLogic.formatClock(entry.startMinute))
                                .append(" do ")
                                .append(ScheduleLogic.formatClock(entry.endMinute))
                                .append(", trajanje ")
                                .append(ScheduleLogic.formatDuration(entry.workMinutes ?: 0))
                        }
                        if (entry.note.isNotBlank()) append(", napomena: ").append(entry.note)
                    }
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(if (compact) 3.dp else 4.dp)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = when {
                !enabled -> colors.onSurfaceVariant.copy(alpha = 0.38f)
                entry != null -> Color.White.copy(alpha = 0.94f)
                else -> colors.onSurfaceVariant
            },
            fontSize = if (compact) 8.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (entry != null && enabled) {
            Text(
                text = entry.code,
                color = Color.White,
                fontSize = if (compact) shiftCodeCompactFontSize(entry.code) else shiftCodeFontSize(entry.code),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = if (compact) 5.dp else 8.dp, start = 1.dp, end = 1.dp)
            )
            if (entry.note.isNotBlank() || entry.hasWorkTime) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp)
                ) {
                    if (entry.hasWorkTime) {
                        Box(
                            Modifier
                                .size(if (compact) 4.dp else 5.dp)
                                .background(TaktoCyan.copy(alpha = 0.95f), CircleShape)
                        )
                    }
                    if (entry.note.isNotBlank()) {
                        Box(
                            Modifier
                                .size(if (compact) 4.dp else 5.dp)
                                .background(Color.White.copy(alpha = 0.88f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
