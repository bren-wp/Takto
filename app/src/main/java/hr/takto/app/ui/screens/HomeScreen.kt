package hr.takto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.ShiftType
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.MonthCalendar
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.croatianDate
import hr.takto.app.ui.components.monthTitle
import hr.takto.app.ui.components.shiftCodeCompactFontSize
import hr.takto.app.ui.theme.TaktoAmber
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoGreen
import hr.takto.app.ui.theme.TaktoPurple
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    store: ScheduleStore,
    contentPadding: PaddingValues,
    onOpenCalendar: (LocalDate?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)
    val weekEntries = store.entriesBetween(weekStart, weekEnd)
    val weekWorkMinutes = store.totalWorkMinutes(weekEntries)
    val weekEmptyDays = (7 - weekEntries.map { it.date }.distinct().size).coerceAtLeast(0)

    val currentMonth = YearMonth.now()
    val monthEntries = store.entriesForMonth(currentMonth)
    val monthWorkMinutes = store.totalWorkMinutes(monthEntries)
    val monthTargetMinutes = store.monthlyTargetMinutes(currentMonth)
    val monthRegularMinutes = minOf(monthWorkMinutes, monthTargetMinutes).coerceAtLeast(0)
    val monthOvertimeMinutes = (monthWorkMinutes - monthTargetMinutes).coerceAtLeast(0)
    val monthBalanceMinutes = monthWorkMinutes - monthTargetMinutes
    val todayEntry = store.entryFor(today)
    val todayQuickTypes = store.suggestedShiftTypes(today).take(4)
    val next = store.nextEntry(today.plusDays(1))
    val todayAccessibilityDescription = if (todayEntry == null) {
        "Danas, ${croatianDate(today)}, nema unosa"
    } else {
        HomeTodayUiLogic.todayEntryDescription(
            dateText = croatianDate(today),
            code = todayEntry.code,
            label = todayEntry.label,
            startText = todayEntry.startMinute?.let(ScheduleLogic::formatClock),
            endText = todayEntry.endMinute?.let(ScheduleLogic::formatClock),
            durationText = todayEntry.workMinutes?.let(ScheduleLogic::formatDuration),
            note = todayEntry.note
        )
    }

    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "Dobro jutro"
        in 11..17 -> "Dobar dan"
        in 18..21 -> "Dobra večer"
        else -> "Laku noć"
    }
    val firstName = store.userProfile.value.firstName

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
            Text(
                monthTitle(currentMonth),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                if (firstName.isBlank()) "$greeting 👋" else "$greeting, $firstName 👋",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                "Raspored, sati i važne obveze na jednom mjestu.",
                color = colors.onSurfaceVariant
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 380.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            value = weekEntries.size.toString(),
                            label = "Upisani dani",
                            color = TaktoBlue,
                            icon = { Icon(Icons.Default.CalendarMonth, null, tint = TaktoBlue) }
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            value = ScheduleLogic.formatDuration(weekWorkMinutes),
                            label = "Evidentirani sati",
                            color = TaktoPurple,
                            icon = { Icon(Icons.Default.AccessTime, null, tint = TaktoPurple) }
                        )
                    }
                    SummaryCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = weekEmptyDays.toString(),
                        label = "Dani bez unosa ovaj tjedan",
                        color = TaktoGreen,
                        icon = { Icon(Icons.Default.EventBusy, null, tint = TaktoGreen) }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        value = weekEntries.size.toString(),
                        label = "Upisani dani",
                        color = TaktoBlue,
                        icon = { Icon(Icons.Default.CalendarMonth, null, tint = TaktoBlue) }
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        value = ScheduleLogic.formatDuration(weekWorkMinutes),
                        label = "Evidentirani sati",
                        color = TaktoPurple,
                        icon = { Icon(Icons.Default.AccessTime, null, tint = TaktoPurple) }
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        value = weekEmptyDays.toString(),
                        label = "Bez unosa",
                        color = TaktoGreen,
                        icon = { Icon(Icons.Default.EventBusy, null, tint = TaktoGreen) }
                    )
                }
            }
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = todayEntry != null) { onOpenCalendar(today) }
                .semantics(mergeDescendants = true) {
                    contentDescription = todayAccessibilityDescription
                },
            padding = PaddingValues(16.dp),
            corner = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "DANAS",
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            fontSize = 11.sp
                        )
                        Text(
                            croatianDate(today),
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (todayEntry != null) colors.primaryContainer else colors.surfaceVariant,
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                if (todayEntry != null) "Unos spremljen" else "Nema unosa",
                                color = if (todayEntry != null) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (todayEntry != null) {
                            Text(
                                "Dodirni za uređivanje",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (todayEntry != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(todayEntry.color, RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                todayEntry.code,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = shiftCodeCompactFontSize(todayEntry.code),
                                maxLines = 1
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(todayEntry.label, fontWeight = FontWeight.Bold)
                            if (todayEntry.hasWorkTime) {
                                Text(
                                    "${ScheduleLogic.formatClock(todayEntry.startMinute)} – ${ScheduleLogic.formatClock(todayEntry.endMinute)} · ${ScheduleLogic.formatDuration(todayEntry.workMinutes ?: 0)}",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    "Radno vrijeme nije upisano.",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            if (todayEntry.note.isNotBlank()) {
                                Text(
                                    todayEntry.note,
                                    color = colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Uredi današnji unos",
                            tint = colors.primary
                        )
                    }
                } else {
                    Text(
                        "Još nema unosa za danas. Najrelevantnije oznake možeš dodati jednim dodirom.",
                        color = colors.onSurfaceVariant
                    )
                    if (todayQuickTypes.isNotEmpty()) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val fontScale = LocalDensity.current.fontScale
                            val columns = HomeTodayUiLogic.quickActionColumns(
                                availableWidthDp = maxWidth.value,
                                fontScale = fontScale
                            )
                            if (columns == 1) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    todayQuickTypes.forEach { type ->
                                        TodayQuickButton(
                                            type = type,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            store.setEntry(today, type)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    todayQuickTypes.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { type ->
                                                TodayQuickButton(
                                                    type = type,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    store.setEntry(today, type)
                                                }
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { onOpenCalendar(today) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.size(7.dp))
                        Text("Otvori detalje za danas", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp), corner = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Mjesečni fond · ${monthTitle(currentMonth)}",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            "${ScheduleLogic.formatDuration(monthWorkMinutes)} / ${ScheduleLogic.formatDuration(monthTargetMinutes)}",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Razlika", color = colors.onSurfaceVariant, fontSize = 11.sp)
                        val balanceText = when {
                            monthBalanceMinutes > 0 -> "+${ScheduleLogic.formatDuration(monthBalanceMinutes)}"
                            monthBalanceMinutes < 0 -> "−${ScheduleLogic.formatDuration(-monthBalanceMinutes)}"
                            else -> "0 min"
                        }
                        Text(
                            balanceText,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (monthBalanceMinutes >= 0) TaktoGreen else colors.error
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetric(
                        modifier = Modifier.weight(1f),
                        label = "Redovni",
                        value = ScheduleLogic.formatDuration(monthRegularMinutes),
                        accent = TaktoGreen
                    )
                    MiniMetric(
                        modifier = Modifier.weight(1f),
                        label = "Prekovremeni",
                        value = ScheduleLogic.formatDuration(monthOvertimeMinutes),
                        accent = TaktoAmber
                    )
                    MiniMetric(
                        modifier = Modifier.weight(1f),
                        label = "Unosi",
                        value = monthEntries.size.toString(),
                        accent = TaktoBlue
                    )
                }

                Text(
                    if (store.hasMonthlyTargetOverride(currentMonth)) {
                        "Koristi se ručno postavljeni fond za ovaj mjesec."
                    } else {
                        "Fond se trenutačno računa automatski prema postavkama."
                    },
                    color = colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        if (next != null) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCalendar(next.date) },
                padding = PaddingValues(14.dp),
                corner = 18.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .background(next.color, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            next.code,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (next.code.length <= 2) 20.sp else 14.sp,
                            maxLines = 1
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            "Sljedeći unos",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(next.label, fontWeight = FontWeight.Bold)
                        Text(croatianDate(next.date), color = colors.onSurfaceVariant, fontSize = 12.sp)
                        if (next.hasWorkTime) {
                            Text(
                                "${ScheduleLogic.formatClock(next.startMinute)} – ${ScheduleLogic.formatClock(next.endMinute)} · ${ScheduleLogic.formatDuration(next.workMinutes ?: 0)}",
                                color = colors.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        if (next.note.isNotBlank()) {
                            Text(next.note, color = colors.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = colors.primary)
                }
            }
        }
        else {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp), corner = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = colors.onSurfaceVariant)
                        Text(
                            " Nema budućih unosa",
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        "Raspored nakon današnjeg dana još je prazan. Možeš odmah dodati prvi budući unos za ${croatianDate(today.plusDays(1))}.",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { onOpenCalendar(today.plusDays(1)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Planiraj sljedeći dan", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }


        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "OVAJ TJEDAN",
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compactWeek = maxWidth < 390.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (compactWeek) 4.dp else 6.dp)
                    ) {
                        repeat(7) { index ->
                            val date = weekStart.plusDays(index.toLong())
                            val entry = store.entryFor(date)
                            val cellColor = entry?.color ?: colors.surfaceVariant
                            val dayDescription = if (entry == null) {
                                "${croatianDate(date)}, nema unosa"
                            } else {
                                "${croatianDate(date)}, ${entry.code}, ${entry.label}"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (compactWeek) 78.dp else 86.dp)
                                    .semantics { contentDescription = dayDescription }
                                    .background(cellColor, RoundedCornerShape(13.dp))
                                    .clickable { onOpenCalendar(date) }
                                    .padding(if (compactWeek) 4.dp else 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned")[index],
                                        fontSize = if (compactWeek) 9.sp else 10.sp,
                                        color = if (entry != null) Color.White else colors.onSurfaceVariant
                                    )
                                    Text(
                                        date.dayOfMonth.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry != null) Color.White else colors.onSurface
                                    )
                                    Text(
                                        entry?.code.orEmpty(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = when {
                                            (entry?.code?.length ?: 0) <= 2 && compactWeek -> 17.sp
                                            (entry?.code?.length ?: 0) <= 2 -> 20.sp
                                            compactWeek -> 10.sp
                                            else -> 12.sp
                                        },
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onOpenCalendar(today) },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.size(8.dp))
            Text("Otvori kalendar", fontWeight = FontWeight.Bold)
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        monthTitle(currentMonth).uppercase(),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text("Prazno = nema unosa", color = colors.onSurfaceVariant, fontSize = 11.sp)
                }
                if (monthEntries.isEmpty()) {
                    Text(
                        "Ovaj mjesec još nema unosa. Dodirni datum u kalendaru za prvi unos.",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
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
private fun TodayQuickButton(
    type: ShiftType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .semantics {
                contentDescription = HomeTodayUiLogic.quickActionDescription(type.code, type.name)
            }
            .heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = type.color),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = type.code,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = shiftCodeCompactFontSize(type.code),
            maxLines = 1
        )
        Spacer(Modifier.size(7.dp))
        Text(
            text = type.name,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MiniMetric(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .background(colors.surfaceVariant.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
            Text(label, color = colors.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
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
    val colors = MaterialTheme.colorScheme
    GlassCard(modifier = modifier, padding = PaddingValues(12.dp), corner = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            icon()
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
