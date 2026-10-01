package hr.takto.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.DefaultShiftTypes
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.monthTitle
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoMuted
import java.time.YearMonth

@Composable
fun StatsScreen(store: ScheduleStore, contentPadding: PaddingValues) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var monthlyTargetDialog by remember { mutableStateOf(false) }
    val monthEntries = store.entriesForMonth(month)
    val counts = linkedMapOf(
        "D" to monthEntries.count { it.code.equals("D", true) },
        "N" to monthEntries.count { it.code.equals("N", true) },
        "GO" to monthEntries.count { it.code.equals("GO", true) },
        "BO" to monthEntries.count { it.code.equals("BO", true) },
        "PD" to monthEntries.count { it.code.equals("PD", true) }
    )
    val customEntries = monthEntries.filter { item -> counts.keys.none { item.code.equals(it, true) } }
    val custom = customEntries.size
    val customGroups = customEntries
        .groupBy { it.code.uppercase() }
        .map { (code, items) -> Triple(code, items.size, items.first()) }
        .sortedWith(compareByDescending<Triple<String, Int, hr.takto.app.model.ShiftEntry>> { it.second }.thenBy { it.first })
    val dayColor = store.shiftType("D")?.color ?: DefaultShiftTypes.day.color
    val nightColor = store.shiftType("N")?.color ?: DefaultShiftTypes.night.color
    val annualColor = store.shiftType("GO")?.color ?: DefaultShiftTypes.annual.color
    val sickColor = store.shiftType("BO")?.color ?: DefaultShiftTypes.sick.color
    val paidColor = store.shiftType("PD")?.color ?: DefaultShiftTypes.paid.color
    val total = monthEntries.size
    val workShifts = counts.getValue("D") + counts.getValue("N")
    val freeDays = (month.lengthOfMonth() - monthEntries.map { it.date.dayOfMonth }.distinct().size).coerceAtLeast(0)
    val timedEntries = monthEntries.filter { it.workMinutes != null }
    val totalWorkMinutes = store.totalWorkMinutes(timedEntries)
    val overtimeMinutes = store.totalOvertimeMinutes(timedEntries)
    val averageShiftMinutes = if (timedEntries.isNotEmpty()) totalWorkMinutes / timedEntries.size else 0
    val standardDaily = store.standardDailyMinutes.value
    val monthlyTarget = store.monthlyTargetMinutes(month)
    val monthlyBalance = totalWorkMinutes - monthlyTarget
    val regularMonthlyMinutes = minOf(totalWorkMinutes, monthlyTarget).coerceAtLeast(0)
    val fundOvertimeMinutes = (totalWorkMinutes - monthlyTarget).coerceAtLeast(0)
    val nightWorkMinutes = store.totalNightWorkMinutes(timedEntries)
    val weekendWorkMinutes = store.totalWeekendWorkMinutes(timedEntries)
    val sundayWorkMinutes = store.totalSundayWorkMinutes(timedEntries)
    val targetIsManual = store.hasMonthlyTargetOverride(month)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaktoLogo(iconSize = 34.dp)
            Spacer(Modifier.weight(1f))
            Text("Statistike", color = TaktoMuted)
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(7.dp), corner = 17.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Prethodni mjesec") }
                Text(monthTitle(month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Sljedeći mjesec") }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(workShifts.toString(), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Radnih smjena (D + N)", color = TaktoMuted)
                    Text("Ukupno oznaka: $total", color = TaktoMuted, fontSize = 12.sp)
                    if (timedEntries.isNotEmpty()) {
                        Text("Evidentirano: ${ScheduleLogic.formatDuration(totalWorkMinutes)}", color = TaktoMuted, fontSize = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(freeDays.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TaktoBlue)
                    Text("Slobodnih dana", color = TaktoMuted)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Radni sati i mjesečni fond", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { monthlyTargetDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TaktoBlue)
                        Text(" Fond", color = TaktoBlue)
                    }
                }
                Text(
                    "Obračun koristi samo dane kojima si upisao početak i kraj smjene. Standardni radni dan: ${ScheduleLogic.formatDuration(standardDaily)}.",
                    color = TaktoMuted,
                    fontSize = 12.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeMetric(Modifier.weight(1f), "Evidentirano", totalWorkMinutes, TaktoBlue)
                    TimeMetric(Modifier.weight(1f), if (targetIsManual) "Fond · ručni" else "Fond · automatski", monthlyTarget, Color(0xFF22B8CF))
                    SignedTimeMetric(Modifier.weight(1f), "Razlika", monthlyBalance)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeMetric(Modifier.weight(1f), "Redovni sati", regularMonthlyMinutes, Color(0xFF13D7A0))
                    TimeMetric(Modifier.weight(1f), "Prekovremeni · fond", fundOvertimeMinutes, Color(0xFFFFB21D))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeMetric(Modifier.weight(1f), "Prekovremeno po danu", overtimeMinutes, Color(0xFFFFB21D))
                    TimeMetric(Modifier.weight(1f), "Prosjek smjene", averageShiftMinutes, Color(0xFF22B8CF))
                }
                Text("Smjena s vremenom: ${timedEntries.size}", color = TaktoMuted, fontSize = 12.sp)
                if (!targetIsManual) {
                    Text(
                        "Automatski fond = svi dani ponedjeljak–petak × standardni radni dan. Blagdani se ne oduzimaju automatski; za to postavi ručni fond za ovaj mjesec.",
                        color = TaktoMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Posebni radni sati", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Noćni rad koristi prozor 22:00–06:00. Vikend i nedjelja računaju se prema stvarnom datumu, uključujući smjene koje prelaze ponoć. Pauza se proporcionalno raspoređuje.",
                    color = TaktoMuted,
                    fontSize = 11.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeMetric(Modifier.weight(1f), "Noćni rad", nightWorkMinutes, Color(0xFF8B46F6))
                    TimeMetric(Modifier.weight(1f), "Vikend", weekendWorkMinutes, Color(0xFF13D7A0))
                    TimeMetric(Modifier.weight(1f), "Nedjelja", sundayWorkMinutes, Color(0xFFFFB21D))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Modifier.weight(1f), "D", "Dnevne", counts.getValue("D"), dayColor)
            StatTile(Modifier.weight(1f), "N", "Noćne", counts.getValue("N"), nightColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Modifier.weight(1f), "GO", "Godišnji", counts.getValue("GO"), annualColor)
            StatTile(Modifier.weight(1f), "BO", "Bolovanje", counts.getValue("BO"), sickColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Modifier.weight(1f), "PD", "Plaćeni dopust", counts.getValue("PD"), paidColor)
            StatTile(Modifier.weight(1f), "✎", "Vlastiti", custom, Color(0xFF22B8CF))
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vizualni uvid", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        counts = listOf(
                            counts.getValue("D") to dayColor,
                            counts.getValue("N") to nightColor,
                            counts.getValue("GO") to annualColor,
                            counts.getValue("BO") to sickColor,
                            counts.getValue("PD") to paidColor,
                            custom to Color(0xFF22B8CF)
                        ),
                        modifier = Modifier.size(170.dp)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Legend("Dnevne smjene", counts.getValue("D"), dayColor)
                        Legend("Noćne smjene", counts.getValue("N"), nightColor)
                        Legend("Godišnji odmor", counts.getValue("GO"), annualColor)
                        Legend("Bolovanje", counts.getValue("BO"), sickColor)
                        Legend("Plaćeni dopust", counts.getValue("PD"), paidColor)
                        if (custom > 0) Legend("Vlastiti unosi", custom, Color(0xFF22B8CF))
                    }
                }
            }
        }

        if (customGroups.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vlastite oznake", style = MaterialTheme.typography.titleLarge)
                    Text("Pregled vlastitih i spremljenih brzih oznaka za odabrani mjesec.", color = TaktoMuted)
                    customGroups.take(10).forEach { (code, count, sample) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(36.dp).background(sample.color, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(code, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (code.length <= 3) 11.sp else 8.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(sample.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(code, color = TaktoMuted, fontSize = 11.sp)
                            }
                            Text(count.toString(), fontWeight = FontWeight.ExtraBold, color = sample.color)
                        }
                    }
                    if (customGroups.size > 10) Text("+ još ${customGroups.size - 10} različitih oznaka", color = TaktoMuted)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Trendovi smjena ${month.year}.", style = MaterialTheme.typography.titleLarge)
                MonthlyBars(store, month.year)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Trendovi radnih sati ${month.year}.", style = MaterialTheme.typography.titleLarge)
                Text("Prikaz uključuje samo unose s evidentiranim radnim vremenom.", color = TaktoMuted, fontSize = 12.sp)
                MonthlyHoursBars(store, month.year)
            }
        }
        Spacer(Modifier.height(6.dp))
    }

    if (monthlyTargetDialog) {
        MonthlyTargetDialog(
            month = month,
            currentMinutes = monthlyTarget,
            automaticMinutes = store.automaticMonthlyTargetMinutes(month),
            isManual = targetIsManual,
            onDismiss = { monthlyTargetDialog = false },
            onAutomatic = {
                store.setMonthlyTargetOverride(month, null)
                monthlyTargetDialog = false
            },
            onSave = { minutes ->
                store.setMonthlyTargetOverride(month, minutes)
                monthlyTargetDialog = false
            }
        )
    }
}

@Composable
private fun TimeMetric(modifier: Modifier, label: String, minutes: Int, color: Color) {
    Box(
        modifier = modifier
            .background(Color(0xFF16243A), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ScheduleLogic.formatDuration(minutes), color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TaktoMuted, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun SignedTimeMetric(modifier: Modifier, label: String, minutes: Int) {
    val positive = minutes >= 0
    val absolute = kotlin.math.abs(minutes)
    val value = when {
        minutes > 0 -> "+${ScheduleLogic.formatDuration(absolute)}"
        minutes < 0 -> "−${ScheduleLogic.formatDuration(absolute)}"
        else -> "0 min"
    }
    Box(
        modifier = modifier
            .background(Color(0xFF16243A), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = if (positive) Color(0xFF13D7A0) else Color(0xFFFF6570), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TaktoMuted, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun MonthlyTargetDialog(
    month: YearMonth,
    currentMinutes: Int,
    automaticMinutes: Int,
    isManual: Boolean,
    onDismiss: () -> Unit,
    onAutomatic: () -> Unit,
    onSave: (Int) -> Unit
) {
    var hours by remember(month, currentMinutes) { mutableStateOf((currentMinutes / 60).toString()) }
    var minutes by remember(month, currentMinutes) { mutableStateOf((currentMinutes % 60).toString()) }
    val parsedHours = hours.trim().toIntOrNull()
    val parsedMinutes = minutes.trim().toIntOrNull()
    val total = if (parsedHours != null && parsedMinutes != null && parsedHours >= 0 && parsedMinutes in 0..59) {
        parsedHours * 60 + parsedMinutes
    } else null
    val valid = total != null && total in 0..ScheduleLogic.MAX_MONTHLY_TARGET_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mjesečni fond · ${monthTitle(month)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (isManual) "Trenutno koristiš ručni fond." else "Trenutno koristiš automatski fond prema radnim danima pon–pet.",
                    color = TaktoMuted
                )
                Text("Automatski: ${ScheduleLogic.formatDuration(automaticMinutes)}", color = Color(0xFF22B8CF), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it.filter(Char::isDigit).take(3) },
                        label = { Text("Sati") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Text("Ručni fond vrijedi samo za odabrani mjesec i ulazi u sigurnosnu kopiju.", color = TaktoMuted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(total!!) }, enabled = valid) { Text("Spremi") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onAutomatic) { Text("Automatski") }
                TextButton(onClick = onDismiss) { Text("Odustani") }
            }
        }
    )
}

@Composable
private fun StatTile(modifier: Modifier, code: String, label: String, count: Int, color: Color) {
    GlassCard(modifier = modifier, padding = PaddingValues(12.dp), corner = 17.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(code, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(count.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TaktoMuted, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
    }
}

@Composable
private fun Legend(name: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(4.dp)))
        Text(name, modifier = Modifier.padding(start = 8.dp).weight(1f), fontSize = 12.sp, color = TaktoMuted)
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun DonutChart(counts: List<Pair<Int, Color>>, modifier: Modifier = Modifier) {
    val rawTotal = counts.sumOf { it.first }
    val total = rawTotal.coerceAtLeast(1)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            var start = -90f
            counts.forEach { (count, color) ->
                if (count > 0) {
                    val sweep = 360f * count / total.toFloat()
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt),
                        size = Size(size.width - 32.dp.toPx(), size.height - 32.dp.toPx()),
                        topLeft = Offset(16.dp.toPx(), 16.dp.toPx())
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rawTotal.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("UKUPNO", fontSize = 10.sp, color = TaktoMuted, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun MonthlyBars(store: ScheduleStore, year: Int) {
    val values = (1..12).map { month ->
        store.entries.values.count { it.date.year == year && it.date.monthValue == month && (it.code.equals("D", true) || it.code.equals("N", true)) }
    }
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val names = listOf("Sij", "Velj", "Ožu", "Tra", "Svi", "Lip", "Srp", "Kol", "Ruj", "Lis", "Stu", "Pro")
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (value > 0) Text(value.toString(), fontSize = 9.sp, color = TaktoMuted)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((120f * value / max.toFloat()).coerceAtLeast(4f).dp)
                        .background(TaktoBlue, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Spacer(Modifier.height(5.dp))
                Text(names[index], fontSize = 9.sp, color = TaktoMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun MonthlyHoursBars(store: ScheduleStore, year: Int) {
    val values = (1..12).map { month ->
        store.entries.values
            .filter { it.date.year == year && it.date.monthValue == month }
            .sumOf { it.workMinutes ?: 0 }
    }
    val max = (values.maxOrNull() ?: 60).coerceAtLeast(60)
    val names = listOf("Sij", "Velj", "Ožu", "Tra", "Svi", "Lip", "Srp", "Kol", "Ruj", "Lis", "Stu", "Pro")
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (value > 0) Text("${value / 60}h", fontSize = 9.sp, color = TaktoMuted)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((120f * value / max.toFloat()).coerceAtLeast(4f).dp)
                        .background(Color(0xFF22B8CF), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Spacer(Modifier.height(5.dp))
                Text(names[index], fontSize = 9.sp, color = TaktoMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

