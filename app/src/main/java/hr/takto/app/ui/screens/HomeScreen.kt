package hr.takto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.MonthCalendar
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.croatianDate
import hr.takto.app.ui.components.monthTitle
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoGreen
import hr.takto.app.ui.theme.TaktoMuted
import hr.takto.app.ui.theme.TaktoPurple
import hr.takto.app.ui.theme.TaktoText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    store: ScheduleStore,
    contentPadding: PaddingValues,
    onOpenCalendar: (LocalDate?) -> Unit,
    onOpenStats: () -> Unit,
    onOpenPatterns: () -> Unit,
    onOpenLeave: () -> Unit
) {
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)
    val weekEntries = store.entriesBetween(weekStart, weekEnd)
    val workCount = weekEntries.count { it.code.equals("D", true) || it.code.equals("N", true) }
    val nightCount = weekEntries.count { it.code.equals("N", true) }
    val annualCount = weekEntries.count { it.code.equals("GO", true) }
    val weekWorkMinutes = store.totalWorkMinutes(weekEntries)
    val weekOvertimeMinutes = store.totalOvertimeMinutes(weekEntries)
    val currentMonth = YearMonth.now()
    val monthEntries = store.entriesForMonth(currentMonth)
    val monthWorkMinutes = store.totalWorkMinutes(monthEntries)
    val monthTargetMinutes = store.monthlyTargetMinutes(currentMonth)
    val monthBalanceMinutes = monthWorkMinutes - monthTargetMinutes
    val next = store.nextEntry(today)
    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "Dobro jutro"
        in 11..17 -> "Dobar dan"
        else -> "Dobra večer"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaktoLogo(iconSize = 36.dp)
            Spacer(Modifier.weight(1f))
            Text("Hrvatski", color = TaktoMuted, style = MaterialTheme.typography.labelMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("$greeting ☀️", style = MaterialTheme.typography.headlineLarge)
            Text(
                if (workCount == 1) "Imaš 1 radnu smjenu ovaj tjedan." else "Imaš $workCount radnih smjena ovaj tjedan.",
                color = TaktoMuted
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                value = workCount.toString(),
                label = "Radne smjene",
                color = TaktoBlue,
                icon = { Icon(Icons.Default.CalendarMonth, null, tint = TaktoBlue) }
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                value = nightCount.toString(),
                label = "Noćne smjene",
                color = TaktoPurple,
                icon = { Icon(Icons.Default.Nightlight, null, tint = TaktoPurple) }
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                value = annualCount.toString(),
                label = "Godišnji odmor",
                color = TaktoGreen,
                icon = { Icon(Icons.Default.Umbrella, null, tint = TaktoGreen) }
            )
        }

        if (weekWorkMinutes > 0) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp), corner = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Evidentirani radni sati ovaj tjedan", color = TaktoMuted, fontSize = 12.sp)
                        Text(ScheduleLogic.formatDuration(weekWorkMinutes), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TaktoBlue)
                    }
                    if (weekOvertimeMinutes > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Prekovremeno", color = TaktoMuted, fontSize = 11.sp)
                            Text(ScheduleLogic.formatDuration(weekOvertimeMinutes), fontWeight = FontWeight.Bold, color = Color(0xFFFFB21D))
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp), corner = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mjesečni fond · ${monthTitle(currentMonth)}", color = TaktoMuted, fontSize = 12.sp)
                    Text(
                        "${ScheduleLogic.formatDuration(monthWorkMinutes)} / ${ScheduleLogic.formatDuration(monthTargetMinutes)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF22B8CF)
                    )
                    Text(
                        if (store.hasMonthlyTargetOverride(currentMonth)) "Ručni fond" else "Automatski fond pon–pet",
                        color = TaktoMuted,
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Razlika", color = TaktoMuted, fontSize = 11.sp)
                    val balanceText = when {
                        monthBalanceMinutes > 0 -> "+${ScheduleLogic.formatDuration(monthBalanceMinutes)}"
                        monthBalanceMinutes < 0 -> "−${ScheduleLogic.formatDuration(-monthBalanceMinutes)}"
                        else -> "0 min"
                    }
                    Text(
                        balanceText,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (monthBalanceMinutes >= 0) TaktoGreen else Color(0xFFFF6570)
                    )
                }
            }
        }

        if (next != null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().clickable { onOpenCalendar(next.date) },
                padding = PaddingValues(14.dp),
                corner = 18.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(54.dp).background(next.color, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(next.code, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (next.code.length <= 2) 20.sp else 14.sp)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(if (next.date == today) "Danas" else "Sljedeći unos", color = TaktoMuted, fontSize = 12.sp)
                        Text(next.label, fontWeight = FontWeight.Bold)
                        Text(croatianDate(next.date), color = TaktoMuted, fontSize = 12.sp)
                        if (next.hasWorkTime) {
                            Text(
                                "${ScheduleLogic.formatClock(next.startMinute)} – ${ScheduleLogic.formatClock(next.endMinute)} · ${ScheduleLogic.formatDuration(next.workMinutes ?: 0)}",
                                color = TaktoMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        if (next.note.isNotBlank()) Text(next.note, color = TaktoMuted, fontSize = 11.sp, maxLines = 1)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TaktoBlue)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OVAJ TJEDAN", color = TaktoMuted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(7) { index ->
                        val date = weekStart.plusDays(index.toLong())
                        val entry = store.entryFor(date)
                        val color = entry?.color ?: Color(0xFF142136)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(86.dp)
                                .background(color, RoundedCornerShape(13.dp))
                                .clickable { onOpenCalendar(date) }
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")[index], fontSize = 10.sp, color = if (entry != null) Color.White else TaktoMuted)
                                Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, color = if (entry != null) Color.White else TaktoText)
                                Text(entry?.code.orEmpty(), fontWeight = FontWeight.ExtraBold, fontSize = if ((entry?.code?.length ?: 0) <= 2) 20.sp else 12.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onOpenCalendar(today) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Dodaj smjenu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Uzorci smjena",
                icon = Icons.Default.Link,
                color = TaktoPurple,
                onClick = onOpenPatterns
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Statistika",
                icon = Icons.Default.Insights,
                color = TaktoBlue,
                onClick = onOpenStats
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Odsustva",
                icon = Icons.Default.Umbrella,
                color = TaktoGreen,
                onClick = onOpenLeave
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(monthTitle(currentMonth).uppercase(), fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Prazno = slobodan dan", color = TaktoMuted, fontSize = 11.sp)
                }
                MonthCalendar(
                    month = currentMonth,
                    entries = store.entries,
                    compact = true,
                    onDayClick = { onOpenCalendar(it) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        padding = PaddingValues(horizontal = 10.dp, vertical = 13.dp),
        corner = 18.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                color = TaktoText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    value: String,
    label: String,
    color: Color,
    icon: @Composable () -> Unit
) {
    GlassCard(modifier = modifier, padding = PaddingValues(12.dp), corner = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            icon()
            Text(value, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TaktoMuted, maxLines = 2)
        }
    }
}
