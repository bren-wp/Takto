package hr.takto.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.DefaultShiftTypes
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.ShiftEntry
import hr.takto.app.model.ShiftType
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.MonthCalendar
import hr.takto.app.ui.components.ShiftChoice
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.croatianDate
import hr.takto.app.ui.components.monthTitle
import hr.takto.app.ui.theme.TaktoBlue
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    store: ScheduleStore,
    contentPadding: PaddingValues,
    initialDate: LocalDate? = null
) {
    val context = LocalContext.current
    var month by remember(initialDate) { mutableStateOf(initialDate?.let(YearMonth::from) ?: YearMonth.now()) }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var customDialog by remember { mutableStateOf(false) }
    var searchDialog by remember { mutableStateOf(false) }
    var jumpDialog by remember { mutableStateOf(false) }
    var customDate by remember { mutableStateOf<LocalDate?>(null) }
    var customPendingNote by remember { mutableStateOf("") }

    var multiSelect by remember { mutableStateOf(false) }
    var selectedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var bulkSheet by remember { mutableStateOf(false) }
    var bulkCustomDialog by remember { mutableStateOf(false) }
    var bulkOverwrite by remember { mutableStateOf(true) }

    var copiedWeek by remember { mutableStateOf<List<ShiftEntry?>>(emptyList()) }
    var pasteWeekDate by remember { mutableStateOf<LocalDate?>(null) }
    var workTimeDate by remember { mutableStateOf<LocalDate?>(null) }
    var bulkWorkTimeDialog by remember { mutableStateOf(false) }

    fun resetMultiSelection() {
        selectedDates = emptySet()
        multiSelect = false
        bulkSheet = false
        bulkCustomDialog = false
        bulkWorkTimeDialog = false
    }

    fun showBulkResult(result: ScheduleStore.BulkEditResult) {
        Toast.makeText(
            context,
            "Promijenjeno: ${result.changed} · preskočeno: ${result.skipped}",
            Toast.LENGTH_LONG
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaktoLogo(iconSize = 34.dp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { searchDialog = true }) {
                Icon(Icons.Default.Search, contentDescription = "Pretraži raspored", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = {
                    val restored = store.undoLastChange()
                    if (restored > 0) Toast.makeText(context, "Vraćena je zadnja promjena.", Toast.LENGTH_SHORT).show()
                },
                enabled = store.canUndo.value
            ) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = if (store.canUndo.value) "Vrati: ${store.undoLabel.value}" else "Nema promjene za vratiti",
                    tint = if (store.canUndo.value) TaktoBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
            IconButton(
                onClick = {
                    multiSelect = !multiSelect
                    selectedDate = null
                    if (!multiSelect) selectedDates = emptySet()
                }
            ) {
                Icon(
                    Icons.Default.Checklist,
                    contentDescription = "Odaberi više dana",
                    tint = if (multiSelect) TaktoBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = {
                val today = LocalDate.now()
                month = YearMonth.from(today)
                if (multiSelect) {
                    selectedDates = setOf(today)
                } else {
                    selectedDate = today
                }
            }) {
                Icon(Icons.Default.Today, contentDescription = null, tint = TaktoBlue)
                Spacer(Modifier.size(4.dp))
                Text("Danas", color = TaktoBlue)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    month = month.minusMonths(1)
                    selectedDate = null
                    selectedDates = emptySet()
                }) {
                    Icon(Icons.Default.ChevronLeft, "Prethodni mjesec")
                }
                Text(
                    monthTitle(month),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { jumpDialog = true },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = {
                    month = month.plusMonths(1)
                    selectedDate = null
                    selectedDates = emptySet()
                }) {
                    Icon(Icons.Default.ChevronRight, "Sljedeći mjesec")
                }
            }
        }

        MonthCalendar(
            month = month,
            entries = store.entries,
            selectedDate = if (multiSelect) null else selectedDate,
            selectedDates = if (multiSelect) selectedDates else emptySet(),
            modifier = Modifier.fillMaxWidth(),
            onDayClick = { date ->
                if (multiSelect) {
                    selectedDates = if (date in selectedDates) selectedDates - date else selectedDates + date
                } else {
                    selectedDate = date
                }
            }
        )

        if (multiSelect) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(12.dp), corner = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Višestruki odabir", fontWeight = FontWeight.Bold)
                            Text("Odabrano: ${selectedDates.size} dana", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { selectedDates = emptySet() }) { Text("Očisti") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { selectedDates = ScheduleLogic.monthDates(month).toSet() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cijeli mjesec") }
                        TextButton(
                            onClick = { selectedDates = ScheduleLogic.monthWeekdays(month).toSet() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Pon–pet") }
                    }
                    Button(
                        onClick = { bulkSheet = true },
                        enabled = selectedDates.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Text(" Uredi odabrane dane", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            if (multiSelect) {
                "Dodirni više datuma pa im odjednom dodijeli bilo koju spremljenu ili vlastitu oznaku."
            } else {
                "Prazna kućica znači da nema spremljenog unosa. Dodirni datum i odaberi oznaku ili vlastiti unos."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    selectedDate?.let { date ->
        var note by remember(date) { mutableStateOf(store.entryFor(date)?.note.orEmpty()) }
        var showAllTypes by remember(date) { mutableStateOf(false) }
        val current = store.entryFor(date)
        val rankedTypes = store.suggestedShiftTypes(date)
        val visibleTypes = if (showAllTypes) rankedTypes else rankedTypes.take(6)

        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(croatianDate(date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (current == null) "Odaberi oznaku" else "Promijeni oznaku", style = MaterialTheme.typography.headlineMedium)
                        if (current != null) {
                            Text("Trenutno: ${current.code} · ${current.label}", color = current.color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (current != null) {
                        IconButton(onClick = {
                            store.removeEntry(date)
                            selectedDate = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Ukloni unos", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(ScheduleStore.MAX_NOTE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Napomena (neobavezno)") },
                    supportingText = { Text("${note.length}/${ScheduleStore.MAX_NOTE_LENGTH}") },
                    maxLines = 2
                )
                if (current != null && note.trim() != current.note) {
                    TextButton(
                        onClick = { store.updateNote(date, note) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Spremi napomenu", color = TaktoBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (current != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(12.dp), corner = 15.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = TaktoBlue)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text("Radno vrijeme", fontWeight = FontWeight.Bold)
                                when {
                                    ScheduleLogic.isLeaveCode(current.code) ->
                                        Text("GO, BO i PD ne ulaze u obračun radnih sati.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    current.hasWorkTime -> {
                                        Text(
                                            "${ScheduleLogic.formatClock(current.startMinute)} – ${ScheduleLogic.formatClock(current.endMinute)} · ${ScheduleLogic.formatDuration(current.workMinutes ?: 0)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                        if (current.breakMinutes > 0) Text("Pauza: ${current.breakMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                    else -> Text("Vrijeme još nije upisano.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            if (!ScheduleLogic.isLeaveCode(current.code)) {
                                TextButton(onClick = { workTimeDate = date; selectedDate = null }) {
                                    Text(if (current.hasWorkTime) "Uredi" else "Dodaj")
                                }
                            }
                        }
                    }
                }

                Text(
                    if (showAllTypes) "Sve spremljene oznake" else "Brzi odabir",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                ShiftTypeGrid(types = visibleTypes) { type ->
                    store.setEntry(date, type, note)
                    selectedDate = null
                }
                if (rankedTypes.size > 6) {
                    TextButton(
                        onClick = { showAllTypes = !showAllTypes },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (showAllTypes) "Prikaži manje" else "Prikaži sve oznake (${rankedTypes.size})")
                    }
                }
                Button(
                    onClick = {
                        customPendingNote = note
                        customDate = date
                        customDialog = true
                        selectedDate = null
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Vlastiti unos", fontWeight = FontWeight.Bold)
                }

                Text("Tjedan", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            copiedWeek = store.copyWeek(date)
                            Toast.makeText(
                                context,
                                "Tjedan je kopiran (${copiedWeek.count { it != null }} unosa).",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedDate = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Text(" Kopiraj")
                    }
                    Button(
                        onClick = {
                            pasteWeekDate = date
                            selectedDate = null
                        },
                        enabled = copiedWeek.size == 7,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
                    ) {
                        Icon(Icons.Default.ContentPaste, null)
                        Text(" Zalijepi")
                    }
                }
                if (copiedWeek.size == 7) {
                    Text(
                        "U memoriji: ${copiedWeek.count { it != null }} označenih dana i ${copiedWeek.count { it == null }} praznih.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (current != null) {
                    TextButton(
                        onClick = { store.removeEntry(date); selectedDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Text(" Ukloni unos za ovaj dan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (bulkSheet && selectedDates.isNotEmpty()) {
        var note by remember(selectedDates) { mutableStateOf("") }
        var showAllBulkTypes by remember(selectedDates) { mutableStateOf(false) }
        val bulkReferenceDate = selectedDates.maxOrNull() ?: LocalDate.now()
        val rankedTypes = store.suggestedShiftTypes(bulkReferenceDate)
        val visibleTypes = if (showAllBulkTypes) rankedTypes else rankedTypes.take(6)

        ModalBottomSheet(
            onDismissRequest = { bulkSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Uredi ${selectedDates.size} dana", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Od ${croatianDate(selectedDates.minOrNull()!!)} do ${croatianDate(selectedDates.maxOrNull()!!)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Prepiši postojeće unose", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (bulkOverwrite) "Odabrani postojeći dani bit će zamijenjeni." else "Već popunjeni dani bit će preskočeni.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = bulkOverwrite, onCheckedChange = { bulkOverwrite = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(ScheduleStore.MAX_NOTE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Zajednička napomena (neobavezno)") },
                    maxLines = 2
                )

                fun assign(type: hr.takto.app.model.ShiftType) {
                    val result = store.setEntries(selectedDates, type, note, bulkOverwrite)
                    showBulkResult(result)
                    resetMultiSelection()
                }

                Text(
                    if (showAllBulkTypes) "Sve spremljene oznake" else "Brzi odabir",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                ShiftTypeGrid(types = visibleTypes) { type -> assign(type) }
                if (rankedTypes.size > 6) {
                    TextButton(
                        onClick = { showAllBulkTypes = !showAllBulkTypes },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (showAllBulkTypes) "Prikaži manje" else "Prikaži sve oznake (${rankedTypes.size})")
                    }
                }
                Button(
                    onClick = {
                        customPendingNote = note
                        bulkSheet = false
                        bulkCustomDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Text(" Vlastiti unos")
                }

                Button(
                    onClick = {
                        bulkSheet = false
                        bulkWorkTimeDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null)
                    Text(" Postavi radno vrijeme popunjenim danima")
                }

                TextButton(
                    onClick = {
                        val result = store.removeEntries(selectedDates)
                        showBulkResult(result)
                        resetMultiSelection()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Text(" Ukloni unose s odabranih dana", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (bulkCustomDialog && selectedDates.isNotEmpty()) {
        CustomEntryDialog(
            initialText = "",
            initialNote = customPendingNote,
            initialColorArgb = ScheduleStore.DEFAULT_CUSTOM_COLOR,
            onDismiss = {
                bulkCustomDialog = false
                customPendingNote = ""
            },
            onSave = { text, note, colorArgb ->
                val result = store.setCustomEntries(selectedDates, text, note, colorArgb, bulkOverwrite)
                showBulkResult(result)
                customPendingNote = ""
                resetMultiSelection()
            }
        )
    }

    pasteWeekDate?.let { targetDate ->
        var overwrite by remember(targetDate) { mutableStateOf(false) }
        val weekStart = ScheduleLogic.startOfWeek(targetDate)
        AlertDialog(
            onDismissRequest = { pasteWeekDate = null },
            title = { Text("Zalijepi kopirani tjedan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Ciljni tjedan: ${croatianDate(weekStart)} – ${croatianDate(weekStart.plusDays(6))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Kopirano: ${copiedWeek.count { it != null }} unosa · ${copiedWeek.count { it == null }} slobodnih dana.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Prepiši postojeće", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (overwrite) "I prazni dani iz kopiranog tjedna mogu obrisati postojeći unos." else "Postojeći popunjeni dani bit će preskočeni.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = overwrite, onCheckedChange = { overwrite = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = store.pasteWeek(targetDate, copiedWeek, overwrite)
                        showBulkResult(result)
                        pasteWeekDate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
                ) { Text("Zalijepi") }
            },
            dismissButton = { TextButton(onClick = { pasteWeekDate = null }) { Text("Odustani") } }
        )
    }

    workTimeDate?.let { date ->
        val entry = store.entryFor(date)
        if (entry == null || ScheduleLogic.isLeaveCode(entry.code)) {
            workTimeDate = null
        } else {
            WorkTimeDialog(
                title = "Radno vrijeme · ${croatianDate(date)}",
                initialStartMinute = entry.startMinute,
                initialEndMinute = entry.endMinute,
                initialBreakMinutes = entry.breakMinutes,
                preset = store.workTimePreset(entry.code),
                showClear = entry.hasWorkTime,
                onDismiss = { workTimeDate = null },
                onClear = {
                    store.clearWorkTime(date)
                    workTimeDate = null
                    Toast.makeText(context, "Radno vrijeme je uklonjeno.", Toast.LENGTH_SHORT).show()
                },
                onSave = { start, end, pause ->
                    if (store.updateWorkTime(date, start, end, pause)) {
                        workTimeDate = null
                        Toast.makeText(context, "Radno vrijeme je spremljeno.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    if (bulkWorkTimeDialog && selectedDates.isNotEmpty()) {
        WorkTimeDialog(
            title = "Radno vrijeme za ${selectedDates.size} dana",
            initialStartMinute = null,
            initialEndMinute = null,
            initialBreakMinutes = 0,
            showClear = false,
            onDismiss = { bulkWorkTimeDialog = false },
            onClear = {},
            onSave = { start, end, pause ->
                val result = store.updateWorkTime(selectedDates, start, end, pause)
                bulkWorkTimeDialog = false
                showBulkResult(result)
            }
        )
    }

    if (searchDialog) {
        ScheduleSearchDialog(
            store = store,
            onDismiss = { searchDialog = false },
            onSelect = { date ->
                month = YearMonth.from(date)
                selectedDates = emptySet()
                multiSelect = false
                selectedDate = date
                searchDialog = false
            }
        )
    }

    if (jumpDialog) {
        JumpToMonthDialog(
            current = month,
            onDismiss = { jumpDialog = false },
            onSelect = { target ->
                month = target
                selectedDate = target.atDay(1)
                selectedDates = emptySet()
                multiSelect = false
                jumpDialog = false
            }
        )
    }

    if (customDialog) {
        val date = customDate
        CustomEntryDialog(
            initialText = date?.let(store::entryFor)?.takeIf { entry -> DefaultShiftTypes.presets.none { it.code.equals(entry.code, true) } }?.code.orEmpty(),
            initialNote = customPendingNote.ifBlank { date?.let(store::entryFor)?.note.orEmpty() },
            initialColorArgb = date?.let(store::entryFor)
                ?.takeIf { entry -> DefaultShiftTypes.presets.none { it.code.equals(entry.code, true) } }
                ?.colorArgb ?: ScheduleStore.DEFAULT_CUSTOM_COLOR,
            onDismiss = {
                customDialog = false
                customDate = null
                customPendingNote = ""
            },
            onSave = { text, note, colorArgb ->
                date?.let { store.setCustomEntry(it, text, note, colorArgb) }
                customDialog = false
                customDate = null
                customPendingNote = ""
                selectedDate = null
            }
        )
    }
}

@Composable
private fun ShiftTypeGrid(
    types: List<ShiftType>,
    onSelect: (ShiftType) -> Unit
) {
    types.chunked(2).forEach { pair ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            pair.forEach { type ->
                ShiftChoice(type, Modifier.weight(1f)) { onSelect(type) }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScheduleSearchDialog(
    store: ScheduleStore,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalized = query.trim()
    val results = if (normalized.isBlank()) {
        emptyList()
    } else {
        store.entries.values
            .asSequence()
            .filter { entry ->
                entry.code.contains(normalized, ignoreCase = true) ||
                    entry.label.contains(normalized, ignoreCase = true) ||
                    entry.note.contains(normalized, ignoreCase = true) ||
                    entry.date.toString().contains(normalized, ignoreCase = true) ||
                    croatianDate(entry.date).contains(normalized, ignoreCase = true)
            }
            .sortedBy { it.date }
            .take(20)
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pretraži raspored") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Oznaka, napomena ili datum") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                when {
                    normalized.isBlank() -> Text("Upiši oznaku, naziv, napomenu ili datum.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    results.isEmpty() -> Text("Nema pronađenih unosa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> results.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(13.dp))
                                .clickable { onSelect(entry.date) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(42.dp).background(entry.color, RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.code, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (entry.code.length <= 2) 16.sp else 11.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(entry.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(croatianDate(entry.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                if (entry.hasWorkTime) {
                                    Text(
                                        "${ScheduleLogic.formatClock(entry.startMinute)} – ${ScheduleLogic.formatClock(entry.endMinute)} · ${ScheduleLogic.formatDuration(entry.workMinutes ?: 0)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                if (entry.note.isNotBlank()) Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}

@Composable
private fun WorkTimeDialog(
    title: String,
    initialStartMinute: Int?,
    initialEndMinute: Int?,
    initialBreakMinutes: Int,
    preset: hr.takto.app.model.WorkTimePreset? = null,
    showClear: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var startText by remember(initialStartMinute) {
        mutableStateOf(initialStartMinute?.let(ScheduleLogic::formatClock).orEmpty())
    }
    var endText by remember(initialEndMinute) {
        mutableStateOf(initialEndMinute?.let(ScheduleLogic::formatClock).orEmpty())
    }
    var breakText by remember(initialBreakMinutes) {
        mutableStateOf(initialBreakMinutes.takeIf { it > 0 }?.toString().orEmpty())
    }
    val start = ScheduleLogic.parseClock(startText)
    val end = ScheduleLogic.parseClock(endText)
    val pause = breakText.trim().toIntOrNull()?.coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES) ?: 0
    val duration = ScheduleLogic.workDurationMinutes(start, end, pause)
    val valid = start != null && end != null && duration != null && duration > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ako je završetak ranije od početka, Takto automatski računa da rad završava sljedeći dan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (preset != null) {
                    TextButton(
                        onClick = {
                            startText = ScheduleLogic.formatClock(preset.startMinute)
                            endText = ScheduleLogic.formatClock(preset.endMinute)
                            breakText = preset.breakMinutes.toString()
                        }
                    ) {
                        Text(
                            "Primijeni zadano ${ScheduleLogic.formatClock(preset.startMinute)}–${ScheduleLogic.formatClock(preset.endMinute)} · pauza ${preset.breakMinutes} min",
                            color = TaktoBlue
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it.take(5) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Početak") },
                        placeholder = { Text("07:00") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it.take(5) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Kraj") },
                        placeholder = { Text("15:00") },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { value -> breakText = value.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pauza u minutama") },
                    placeholder = { Text("30") },
                    singleLine = true
                )
                if (valid) {
                    Text(
                        "Ukupno: ${ScheduleLogic.formatDuration(duration ?: 0)}",
                        color = TaktoBlue,
                        fontWeight = FontWeight.Bold
                    )
                } else if (startText.isNotBlank() || endText.isNotBlank()) {
                    Text("Upiši valjano vrijeme u obliku HH:mm.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (showClear) {
                    TextButton(onClick = onClear) {
                        Text("Ukloni radno vrijeme", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onSave(start!!, end!!, pause) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
private fun CustomEntryDialog(
    initialText: String,
    initialNote: String,
    initialColorArgb: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    var colorArgb by remember(initialColorArgb) { mutableStateOf(initialColorArgb) }
    val palette = listOf(0xFF22B8CFL, 0xFF2488FFL, 0xFF8B46F6L, 0xFF13D7A0L, 0xFFFFB21DL, 0xFFFF4B55L)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vlastiti unos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Upiši bilo što. Oznaka se prikazuje preko kućice kalendara.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(ScheduleStore.MAX_CUSTOM_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Oznaka ili tekst") },
                    supportingText = { Text("${text.length}/${ScheduleStore.MAX_CUSTOM_LENGTH}") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(ScheduleStore.MAX_NOTE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Napomena (neobavezno)") },
                    maxLines = 3
                )
                Text("Boja", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    palette.forEach { option ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(Color(option), RoundedCornerShape(10.dp))
                                .border(
                                    if (colorArgb == option) 2.dp else 0.dp,
                                    if (colorArgb == option) Color.White else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { colorArgb = option },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorArgb == option) Text("✓", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onSave(text, note, colorArgb) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}


@Composable
private fun JumpToMonthDialog(
    current: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit
) {
    val thisYear = LocalDate.now().year
    val minYear = thisYear - 100
    val maxYear = thisYear + 100
    var monthText by remember(current) { mutableStateOf(current.monthValue.toString()) }
    var yearText by remember(current) { mutableStateOf(current.year.toString()) }

    val monthValue = monthText.toIntOrNull()
    val yearValue = yearText.toIntOrNull()
    val valid = monthValue in 1..12 && yearValue != null && yearValue in minYear..maxYear

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Idi na mjesec") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Brzi skok do bilo kojeg mjeseca u rasponu od 100 godina unatrag do 100 godina unaprijed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = monthText,
                        onValueChange = { monthText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Mjesec") },
                        placeholder = { Text("1–12") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(5) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Godina") },
                        placeholder = { Text("$minYear–$maxYear") },
                        singleLine = true
                    )
                }
                if (!valid && (monthText.isNotBlank() || yearText.isNotBlank())) {
                    Text(
                        "Dopušten je mjesec 1–12 i godina $minYear–$maxYear.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(YearMonth.of(yearValue!!, monthValue!!)) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Otvori") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
