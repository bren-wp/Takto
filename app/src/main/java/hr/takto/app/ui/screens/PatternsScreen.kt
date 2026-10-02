package hr.takto.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.croatianDate
import hr.takto.app.ui.theme.TaktoBlue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private data class PatternDef(
    val name: String,
    val description: String,
    val codes: List<String?>,
    val id: String? = null
)

private fun patternSummary(codes: List<String?>): String {
    if (codes.isEmpty()) return "Prazan uzorak"
    val segments = mutableListOf<Pair<String?, Int>>()
    codes.forEach { code ->
        val last = segments.lastOrNull()
        if (last != null && last.first == code) {
            segments[segments.lastIndex] = code to (last.second + 1)
        } else {
            segments += code to 1
        }
    }
    return segments.joinToString(" · ") { (code, count) ->
        val label = code ?: "bez unosa"
        if (count > 1) "$label × $count" else label
    }
}

@Composable
fun PatternsScreen(store: ScheduleStore, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val suggestedTypes = store.suggestedShiftTypes()
    val primaryType = suggestedTypes.firstOrNull()
    val secondaryType = suggestedTypes.getOrNull(1)
    val builtInPatterns = buildList {
        primaryType?.let { primary ->
            add(
                PatternDef(
                    "5 × ${primary.code} / 2 bez unosa",
                    "Pet dana s oznakom ${primary.code}, zatim dva dana bez unosa.",
                    listOf(primary.code, primary.code, primary.code, primary.code, primary.code, null, null)
                )
            )
        }
        if (primaryType != null && secondaryType != null) {
            add(
                PatternDef(
                    "2 ${primaryType.code} / 2 ${secondaryType.code} / 4 bez unosa",
                    "Prilagodljiva osmodnevna rotacija iz tvojih najčešćih oznaka.",
                    listOf(
                        primaryType.code,
                        primaryType.code,
                        secondaryType.code,
                        secondaryType.code,
                        null,
                        null,
                        null,
                        null
                    )
                )
            )
            add(
                PatternDef(
                    "${primaryType.code} / ${secondaryType.code} / bez unosa",
                    "Jednostavna trodnevna rotacija iz tvojih najčešćih oznaka.",
                    listOf(primaryType.code, secondaryType.code, null)
                )
            )
        }
    }
    val patterns = builtInPatterns + store.savedPatterns.map { saved ->
        PatternDef(saved.name, "Tvoj spremljeni uzorak.", saved.codes, saved.id)
    }
    var selectedPattern by remember { mutableStateOf<PatternDef?>(null) }
    var customPatternDialog by remember { mutableStateOf(false) }

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
            Text("Uzorci", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Uzorci rasporeda", style = MaterialTheme.typography.headlineLarge)
            Text("Popuni više tjedana odjednom. Prazna mjesta u uzorku ostaju bez unosa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { customPatternDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = TaktoBlue)
                Text(" Novi vlastiti uzorak", color = TaktoBlue, fontWeight = FontWeight.Bold)
            }
        }

        patterns.forEach { pattern ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Replay, null, tint = TaktoBlue)
                        Text(pattern.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp).weight(1f))
                        pattern.id?.let { id ->
                            IconButton(onClick = { store.removeSavedPattern(id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Obriši uzorak", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Text(pattern.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            patternSummary(pattern.codes),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = { selectedPattern = pattern },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Text(" Primijeni uzorak", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (customPatternDialog) {
        CustomPatternDialog(
            store = store,
            onDismiss = { customPatternDialog = false },
            onSave = { name, codes ->
                val saved = store.addSavedPattern(name, codes)
                Toast.makeText(
                    context,
                    if (saved) "Vlastiti uzorak je spremljen." else "Uzorak nije spremljen. Provjeri naziv i korake ili ukloni neki stari uzorak.",
                    Toast.LENGTH_LONG
                ).show()
                if (saved) customPatternDialog = false
            }
        )
    }

    selectedPattern?.let { pattern ->
        PatternApplyDialog(
            pattern = pattern,
            onDismiss = { selectedPattern = null },
            onApply = { startDate, days, overwrite ->
                val result = store.applyPattern(
                    startDate = startDate,
                    codes = pattern.codes,
                    numberOfDays = days,
                    overwriteExisting = overwrite
                )
                Toast.makeText(
                    context,
                    "Promijenjeno: ${result.changed} · preskočeno: ${result.skipped} · bez unosa: ${result.freeDays}",
                    Toast.LENGTH_LONG
                ).show()
                selectedPattern = null
            }
        )
    }
}

@Composable
private fun CustomPatternDialog(
    store: ScheduleStore,
    onDismiss: () -> Unit,
    onSave: (String, List<String?>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sequence by remember { mutableStateOf("") }
    val quickTypes = store.suggestedShiftTypes().take(6)
    val codes = ScheduleLogic.parsePatternSequence(sequence)
    val valid = name.isNotBlank() && codes.isNotEmpty() && codes.any { it != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi vlastiti uzorak") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(ScheduleLogic.MAX_PATTERN_NAME_LENGTH) },
                    label = { Text("Naziv uzorka") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sequence,
                    onValueChange = { sequence = it.take(240) },
                    label = { Text("Koraci odvojeni zarezom") },
                    supportingText = { Text("Primjer: R, R, EDU, -, -  ·  '-' znači bez unosa") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Brzo dodaj korak",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                quickTypes.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { type ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val next = if (sequence.isBlank()) type.code else "$sequence, ${type.code}"
                                    sequence = next.take(240)
                                },
                                label = { Text(type.code, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val next = if (sequence.isBlank()) "-" else "$sequence, -"
                            sequence = next.take(240)
                        },
                        label = { Text("Bez unosa") },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { sequence = "" },
                        enabled = sequence.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Očisti")
                    }
                }
                if (codes.isNotEmpty()) {
                    Text("Pregled", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Text(
                            patternSummary(codes),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    "Možeš koristiti bilo koju spremljenu oznaku ili novu kratku oznaku. Znak '-' ostavlja dan bez unosa.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, codes) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi uzorak") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
private fun PatternApplyDialog(
    pattern: PatternDef,
    onDismiss: () -> Unit,
    onApply: (startDate: LocalDate, days: Int, overwrite: Boolean) -> Unit
) {
    val today = LocalDate.now()
    var days by remember(pattern) { mutableIntStateOf(28) }
    var overwrite by remember(pattern) { mutableStateOf(false) }
    var startText by remember(pattern) { mutableStateOf(formatPatternDate(today)) }
    val startDate = parsePatternDate(startText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Primijeni: ${pattern.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Odaberi datum od kojeg uzorak počinje.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                    label = { Text("Početni datum") },
                    supportingText = {
                        Text(
                            startDate?.let { croatianDate(it) } ?: "Format: 1.10.2026.",
                            color = if (startDate != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    },
                    isError = startText.isNotBlank() && startDate == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                    listOf(
                        "Danas" to today,
                        "Sutra" to today.plusDays(1),
                        "Pon" to nextMonday
                    ).forEach { (label, date) ->
                        FilterChip(
                            selected = startDate == date,
                            onClick = { startText = formatPatternDate(date) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("Koliko dana popuniti?", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 28, 56).forEach { option ->
                        FilterChip(
                            selected = days == option,
                            onClick = { days = option },
                            label = { Text(option.toString()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Prepiši postojeće unose", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (overwrite) "Postojeći raspored u rasponu može biti zamijenjen." else "Postojeći dani ostaju netaknuti.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = overwrite, onCheckedChange = { overwrite = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { startDate?.let { onApply(it, days, overwrite) } },
                enabled = startDate != null,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) {
                Text("Primijeni")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

private val patternDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.uuuu.")

private fun formatPatternDate(date: LocalDate): String = date.format(patternDateFormatter)

private fun parsePatternDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), patternDateFormatter) }.getOrNull()
