package hr.takto.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.PayrollCalculator
import hr.takto.app.model.PayrollInputs
import hr.takto.app.model.StatsChartLogic
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.monthTitle
import hr.takto.app.ui.components.readableContentColor
import hr.takto.app.ui.components.shiftCodeCompactFontSize
import hr.takto.app.ui.theme.TaktoBlue
import java.time.YearMonth
import java.util.Locale

private data class ScheduleCodeStat(
    val code: String,
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun StatsScreen(store: ScheduleStore, contentPadding: PaddingValues) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var monthlyTargetDialog by remember { mutableStateOf(false) }
    val monthEntries = store.entriesForMonth(month)
    val distribution = monthEntries
        .groupBy { it.code.uppercase(Locale.ROOT) }
        .map { (code, items) ->
            val sample = items.first()
            ScheduleCodeStat(
                code = code,
                label = sample.label.ifBlank { code },
                count = items.size,
                color = sample.color
            )
        }
        .sortedWith(compareByDescending<ScheduleCodeStat> { it.count }.thenBy { it.code })
    val total = monthEntries.size
    val freeDays = (month.lengthOfMonth() - monthEntries.map { it.date.dayOfMonth }.distinct().size).coerceAtLeast(0)
    val timedEntries = monthEntries.filter { it.workMinutes != null }
    val untimedWorkEntryCount = monthEntries.count {
        !ScheduleLogic.isLeaveCode(it.code) && !it.hasWorkTime
    }
    val sickLeaveDayCount = monthEntries.count { it.code.equals("BO", ignoreCase = true) }
    val totalWorkMinutes = store.totalWorkMinutes(timedEntries)
    val overtimeMinutes = store.totalOvertimeMinutes(timedEntries)
    val confirmedOvertimeMinutes = store.totalConfirmedOvertimeMinutes(timedEntries)
    val averageShiftMinutes = if (timedEntries.isNotEmpty()) totalWorkMinutes / timedEntries.size else 0
    val standardDaily = store.standardDailyMinutes.value
    val monthlyTarget = store.monthlyTargetMinutes(month)
    val monthlyBalance = totalWorkMinutes - monthlyTarget
    val regularMonthlyMinutes = ScheduleLogic.regularWorkMinutes(totalWorkMinutes, confirmedOvertimeMinutes)
    val fundOvertimeMinutes = (totalWorkMinutes - monthlyTarget).coerceAtLeast(0)
    val nightWorkMinutes = store.totalNightWorkMinutes(timedEntries)
    val saturdayWorkMinutes = store.totalSaturdayWorkMinutes(timedEntries)
    val sundayWorkMinutes = store.totalSundayWorkMinutes(timedEntries)
    val holidayWorkMinutes = store.totalHolidayWorkMinutes(timedEntries)
    val targetIsManual = store.hasMonthlyTargetOverride(month)
    val payroll = PayrollCalculator.calculate(
        profile = store.payrollProfile.value,
        input = PayrollInputs(
            month = month,
            monthlyFundMinutes = monthlyTarget,
            workedMinutes = totalWorkMinutes,
            overtimeMinutes = confirmedOvertimeMinutes,
            nightMinutes = nightWorkMinutes,
            saturdayMinutes = saturdayWorkMinutes,
            sundayMinutes = sundayWorkMinutes,
            holidayMinutes = holidayWorkMinutes,
            untimedWorkEntryCount = untimedWorkEntryCount,
            sickLeaveDayCount = sickLeaveDayCount
        )
    )

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
            Text("Statistike", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(total.toString(), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Unosa u rasporedu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Različitih oznaka: ${distribution.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    if (timedEntries.isNotEmpty()) {
                        Text("Evidentirano: ${ScheduleLogic.formatDuration(totalWorkMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(freeDays.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TaktoBlue)
                    Text("Dana bez unosa", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    "Obračun koristi dane kojima si upisao početak i kraj radnog vremena. Standardni radni dan: ${ScheduleLogic.formatDuration(standardDaily)}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 390.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeMetric(Modifier.fillMaxWidth(), "Evidentirano", totalWorkMinutes, TaktoBlue)
                            TimeMetric(
                                Modifier.fillMaxWidth(),
                                if (targetIsManual) "Fond · ručni" else "Fond · automatski",
                                monthlyTarget,
                                Color(0xFF22B8CF)
                            )
                            SignedTimeMetric(Modifier.fillMaxWidth(), "Razlika", monthlyBalance)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TimeMetric(Modifier.weight(1f), "Evidentirano", totalWorkMinutes, TaktoBlue)
                            TimeMetric(
                                Modifier.weight(1f),
                                if (targetIsManual) "Fond · ručni" else "Fond · automatski",
                                monthlyTarget,
                                Color(0xFF22B8CF)
                            )
                            SignedTimeMetric(Modifier.weight(1f), "Razlika", monthlyBalance)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeMetric(Modifier.weight(1f), "Redovni sati", regularMonthlyMinutes, Color(0xFF13D7A0))
                    TimeMetric(Modifier.weight(1f), "Prekovremeni · fond", fundOvertimeMinutes, Color(0xFFFFB21D))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeMetric(Modifier.weight(1f), "Prekovremeno po danu", overtimeMinutes, Color(0xFFFFB21D))
                    TimeMetric(Modifier.weight(1f), "Prosjek radnog unosa", averageShiftMinutes, Color(0xFF22B8CF))
                }
                Text("Unosi s vremenom: ${timedEntries.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (!targetIsManual) {
                    Text(
                        "Automatski fond = radni dani ponedjeljak–petak bez hrvatskih blagdana × standardni radni dan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Posebni radni sati", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Noćni rad, subota, nedjelja i rad na blagdan računaju se prema stvarnom datumu, uključujući smjene koje prelaze ponoć. Pauza se proporcionalno raspoređuje.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 390.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeMetric(Modifier.fillMaxWidth(), "Noćni rad", nightWorkMinutes, Color(0xFF8B46F6))
                            TimeMetric(Modifier.fillMaxWidth(), "Subota", saturdayWorkMinutes, Color(0xFF13D7A0))
                            TimeMetric(Modifier.fillMaxWidth(), "Nedjelja", sundayWorkMinutes, Color(0xFFFFB21D))
                            TimeMetric(Modifier.fillMaxWidth(), "Blagdan", holidayWorkMinutes, Color(0xFFEF476F))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TimeMetric(Modifier.weight(1f), "Noćni rad", nightWorkMinutes, Color(0xFF8B46F6))
                                TimeMetric(Modifier.weight(1f), "Subota", saturdayWorkMinutes, Color(0xFF13D7A0))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TimeMetric(Modifier.weight(1f), "Nedjelja", sundayWorkMinutes, Color(0xFFFFB21D))
                                TimeMetric(Modifier.weight(1f), "Blagdan", holidayWorkMinutes, Color(0xFFEF476F))
                            }
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Priznati prekovremeni", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Ulaze u obračun plaće",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        ScheduleLogic.formatDuration(confirmedOvertimeMinutes),
                        color = Color(0xFFFFB21D),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    "Kontrola prema mjesečnom fondu: ${ScheduleLogic.formatDuration(fundOvertimeMinutes)} · iznad standardnog dana: ${ScheduleLogic.formatDuration(overtimeMinutes)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    "Za plaću se koriste samo prekovremeni koje si izričito upisao uz radni dan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Plaća", style = MaterialTheme.typography.titleLarge)
                when {
                    !store.payrollProfile.value.enabled -> {
                        Text(
                            "Obračun plaće nije postavljen. U Postavkama upiši sustav obračuna, koeficijent, staž i porezne podatke.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    !payroll.complete -> {
                        Text(
                            "Nedostaju podaci: ${payroll.missing.joinToString(", ")}.",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Dovrši podatke u Postavkama kako Takto ne bi prikazao netočan iznos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    else -> {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (maxWidth < 420.dp) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MoneyMetric(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "Isplata",
                                        value = payroll.payoutEur,
                                        color = Color(0xFF13D7A0)
                                    )
                                    MoneyMetric(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "Neto",
                                        value = payroll.netSalaryEur,
                                        color = TaktoBlue
                                    )
                                    MoneyMetric(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "Bruto",
                                        value = payroll.grossEur,
                                        color = Color(0xFF8B46F6)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MoneyMetric(
                                        modifier = Modifier.weight(1f),
                                        label = "Isplata",
                                        value = payroll.payoutEur,
                                        color = Color(0xFF13D7A0)
                                    )
                                    MoneyMetric(
                                        modifier = Modifier.weight(1f),
                                        label = "Neto",
                                        value = payroll.netSalaryEur,
                                        color = TaktoBlue
                                    )
                                    MoneyMetric(
                                        modifier = Modifier.weight(1f),
                                        label = "Bruto",
                                        value = payroll.grossEur,
                                        color = Color(0xFF8B46F6)
                                    )
                                }
                            }
                        }
                        Text(
                            "Sat: ${statsEuro(payroll.hourlyRateEur)} · osnovna plaća: ${statsEuro(payroll.baseSalaryEur)} · staž: ${statsEuro(payroll.seniorityEur)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            "Prekovremeni: ${statsEuro(payroll.overtimePayEur)} · noć: ${statsEuro(payroll.nightSupplementEur)} · subota: ${statsEuro(payroll.saturdaySupplementEur)} · nedjelja: ${statsEuro(payroll.sundaySupplementEur)} · blagdan: ${statsEuro(payroll.holidaySupplementEur)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        if (payroll.additionalGrossEur > 0.0 || payroll.nonTaxableEur > 0.0) {
                            Text(
                                "Ostali bruto dodaci: ${statsEuro(payroll.additionalGrossEur)} · neoporezivo: ${statsEuro(payroll.nonTaxableEur)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            "Izračun se temelji na spremljenim parametrima i evidentiranim satima za odabrani mjesec.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Raspodjela oznaka", style = MaterialTheme.typography.titleLarge)
                if (distribution.isEmpty()) {
                    Text(
                        "Za ovaj mjesec još nema spremljenih unosa.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val singleColumn = maxWidth < 360.dp
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            distribution.take(6)
                                .chunked(if (singleColumn) 1 else 2)
                                .forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        pair.forEach { stat ->
                                            StatTile(
                                                modifier = Modifier.weight(1f),
                                                code = stat.code,
                                                label = stat.label,
                                                count = stat.count,
                                                color = stat.color
                                            )
                                        }
                                        if (!singleColumn && pair.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                        }
                    }
                    if (distribution.size > 6) {
                        Text(
                            "+ još ${distribution.size - 6} različitih oznaka",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (distribution.isNotEmpty()) {
            val chartPrimary = distribution.take(6)
            val otherCount = distribution.drop(6).sumOf { it.count }
            val otherColor = MaterialTheme.colorScheme.outline

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Vizualni uvid", style = MaterialTheme.typography.titleLarge)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val chart: @Composable () -> Unit = {
                            DonutChart(
                                counts = buildList {
                                    chartPrimary.forEach { add(it.count to it.color) }
                                    if (otherCount > 0) add(otherCount to otherColor)
                                },
                                modifier = Modifier.size(170.dp)
                            )
                        }
                        val legend: @Composable () -> Unit = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chartPrimary.forEach { stat ->
                                    Legend(
                                        name = "${stat.code} · ${stat.label}",
                                        count = stat.count,
                                        color = stat.color
                                    )
                                }
                                if (otherCount > 0) Legend("Ostale oznake", otherCount, otherColor)
                            }
                        }

                        if (maxWidth < 390.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chart()
                                legend()
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chart()
                                Box(Modifier.weight(1f)) { legend() }
                            }
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Trendovi oznaka ${month.year}.", style = MaterialTheme.typography.titleLarge)
                MonthlyBars(store, month.year)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Trendovi radnih sati ${month.year}.", style = MaterialTheme.typography.titleLarge)
                Text("Prikaz uključuje samo unose s evidentiranim radnim vremenom.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
private fun MoneyMetric(
    modifier: Modifier,
    label: String,
    value: Double,
    color: Color
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(statsEuro(value), color = color, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

private fun statsEuro(value: Double): String =
    String.format(Locale("hr", "HR"), "%,.2f €", value)

@Composable
private fun TimeMetric(modifier: Modifier, label: String, minutes: Int, color: Color) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ScheduleLogic.formatDuration(minutes), color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2)
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
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = if (positive) Color(0xFF13D7A0) else MaterialTheme.colorScheme.error, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Automatski: ${ScheduleLogic.formatDuration(automaticMinutes)}", color = Color(0xFF22B8CF), fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter(Char::isDigit).take(3) },
                    label = { Text("Sati") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                    label = { Text("Minute") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Ručni fond vrijedi samo za odabrani mjesec i ulazi u sigurnosnu kopiju.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .background(color, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    code,
                    color = readableContentColor(color),
                    fontSize = shiftCodeCompactFontSize(code),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            Text(count.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
    }
}

@Composable
private fun Legend(name: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(4.dp)))
        Text(name, modifier = Modifier.padding(start = 8.dp).weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("UKUPNO", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun MonthlyBars(store: ScheduleStore, year: Int) {
    val values = (1..12).map { month ->
        store.entries.values.count { it.date.year == year && it.date.monthValue == month }
    }
    if (!StatsChartLogic.hasPositiveData(values)) {
        Text(
            "U $year. godini još nema spremljenih unosa.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        return
    }

    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val names = listOf("Sij", "Velj", "Ožu", "Tra", "Svi", "Lip", "Srp", "Kol", "Ruj", "Lis", "Stu", "Pro")
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "${names[index]}: $value unosa" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (value > 0) {
                    Text(value.toString(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(StatsChartLogic.barHeight(value, max).dp)
                        .background(TaktoBlue, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    names[index],
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
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
    if (!StatsChartLogic.hasPositiveData(values)) {
        Text(
            "U $year. godini još nema evidentiranih radnih sati.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        return
    }

    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val names = listOf("Sij", "Velj", "Ožu", "Tra", "Svi", "Lip", "Srp", "Kol", "Ruj", "Lis", "Stu", "Pro")
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "${names[index]}: ${ScheduleLogic.formatDuration(value)} evidentiranog rada"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (value > 0) {
                    Text(
                        StatsChartLogic.compactDurationLabel(value),
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(StatsChartLogic.barHeight(value, max).dp)
                        .background(Color(0xFF22B8CF), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    names[index],
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
