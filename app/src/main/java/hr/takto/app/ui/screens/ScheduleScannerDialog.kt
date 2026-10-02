package hr.takto.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.RosterScanParser
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.ScheduleScanParseResult
import hr.takto.app.model.ScannedScheduleItem
import hr.takto.app.ui.components.croatianDate
import hr.takto.app.ui.components.monthTitle
import hr.takto.app.ui.theme.TaktoBlue
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ScheduleScannerDialog(
    store: ScheduleStore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val scope = rememberCoroutineScope()
    val recognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val documentScanner = remember {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
    }

    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ScheduleScanParseResult?>(null) }
    var overwrite by remember { mutableStateOf(true) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var personHint by remember { mutableStateOf(store.userProfile.value.fullName) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var scanReferenceMonth by remember { mutableStateOf(YearMonth.now()) }
    var referenceMonthConfirmed by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScannedScheduleItem?>(null) }

    fun applyRecognizedText(text: String, referenceMonth: YearMonth = scanReferenceMonth) {
        val referenceDate = referenceMonth.atDay(1)
        val parsed = if (personHint.isNotBlank()) {
            RosterScanParser.parseForPerson(text, personHint, referenceDate)
        } else {
            RosterScanParser.parse(text, referenceDate)
        }
        result = parsed
        if (!parsed.usedReferenceMonth) referenceMonthConfirmed = true
        error = when {
            parsed.ambiguousDateCount > 0 ->
                "Pronađeno je više različitih rasporeda za iste datume. Ponovno označi područje tako da obuhvati zaglavlje s datumima i samo svoj red."
            parsed.items.isEmpty() && personHint.isNotBlank() ->
                "Nisam pronašao dovoljno siguran red za osobu „${personHint.trim()}”. Provjeri ime ili ponovno označi samo njezin red."
            parsed.items.isEmpty() ->
                "Nisu pronađeni sigurni datum i oznaka. Ponovno označi zaglavlje s datumima i svoj red ili pokušaj s jasnijom slikom."
            else -> null
        }
    }

    fun processBitmap(bitmap: Bitmap) {
        scanning = true
        error = null
        result = null
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { recognized ->
                scanning = false
                recognizedText = recognized.text
                applyRecognizedText(recognized.text)
            }
            .addOnFailureListener {
                scanning = false
                error = "Prepoznavanje nije uspjelo. Pokušaj ponovno s jasnijom fotografijom."
            }
    }

    fun replaceScannedItem(original: ScannedScheduleItem, replacement: ScannedScheduleItem?) {
        val current = result ?: return
        val updated = current.items
            .mapNotNull { item ->
                if (item.date == original.date) replacement else item
            }
            .distinctBy { it.date }
            .sortedBy { it.date }
        result = current.copy(items = updated)
        editingItem = null
    }

    fun prepareImage(uri: Uri) {
        scanning = true
        error = null
        result = null
        recognizedText = null
        scanReferenceMonth = YearMonth.now()
        referenceMonthConfirmed = false
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeRosterBitmap(context, uri) }
            scanning = false
            if (bitmap == null) {
                error = "Slika se ne može otvoriti."
            } else {
                sourceBitmap = bitmap
                editingBitmap = bitmap
            }
        }
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(::prepareImage)
    }

    val cameraScanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        val imageUri = scanResult?.pages?.firstOrNull()?.imageUri
        if (imageUri != null) {
            prepareImage(imageUri)
        } else {
            error = "Skeniranje je završeno bez čitljive slike."
        }
    }

    editingBitmap?.let { bitmap ->
        RosterImageEditorDialog(
            sourceBitmap = bitmap,
            onDismiss = { editingBitmap = null },
            onConfirm = { cropped ->
                editingBitmap = null
                processBitmap(cropped)
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uvezi raspored") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Prvo odaberi ili fotografiraj raspored. Zatim označi zaglavlje s datumima i samo svoj red, a druge osobe ostavi izvan okvira.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = personHint,
                    onValueChange = { personHint = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Osoba u rasporedu") },
                    supportingText = {
                        Text(
                            if (personHint.isBlank()) {
                                "Ostavi prazno samo ako si izrezao točno jedan red."
                            } else {
                                "Takto će pokušati izdvojiti samo red ove osobe ako su na slici i drugi zaposlenici."
                            }
                        )
                    },
                    singleLine = true
                )

                recognizedText?.let { text ->
                    TextButton(
                        onClick = { applyRecognizedText(text) },
                        enabled = !scanning,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Ponovno provjeri osobu", color = TaktoBlue)
                    }
                }

                Button(
                    onClick = {
                        val host = activity
                        if (host == null) {
                            error = "Kamera za skeniranje trenutačno nije dostupna."
                        } else {
                            documentScanner.getStartScanIntent(host)
                                .addOnSuccessListener { intentSender ->
                                    cameraScanner.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener {
                                    error = "Skeniranje kamerom trenutačno nije dostupno. Možeš uvesti fotografiju iz galerije."
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Skeniraj kamerom", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TaktoBlue)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (result == null) "Uvezi iz galerije" else "Odaberi drugu sliku",
                        color = TaktoBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (scanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text("Prepoznajem datume i oznake…")
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    if (sourceBitmap != null) {
                        Button(
                            onClick = {
                                result = null
                                error = null
                                editingBitmap = sourceBitmap
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !scanning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Ponovno označi područje", color = TaktoBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                result?.takeIf { it.items.isNotEmpty() }?.let { parsed ->
                    Text(
                        "Pronađeno ${parsed.items.size} unosa",
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (!parsed.usedReferenceMonth && parsed.detectedMonth != null) {
                        Text(
                            "Mjesec: ${monthTitle(parsed.detectedMonth)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            "Mjesec nije prepoznat. Odaberi točan mjesec prije uvoza.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val next = scanReferenceMonth.minusMonths(1)
                                    scanReferenceMonth = next
                                    referenceMonthConfirmed = false
                                    recognizedText?.let { applyRecognizedText(it, next) }
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodni mjesec")
                            }
                            Text(
                                monthTitle(scanReferenceMonth),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val next = scanReferenceMonth.plusMonths(1)
                                    scanReferenceMonth = next
                                    referenceMonthConfirmed = false
                                    recognizedText?.let { applyRecognizedText(it, next) }
                                }
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeći mjesec")
                            }
                        }
                        Button(
                            onClick = { referenceMonthConfirmed = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (referenceMonthConfirmed) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    TaktoBlue
                                }
                            )
                        ) {
                            Text(
                                if (referenceMonthConfirmed) {
                                    "Mjesec potvrđen"
                                } else {
                                    "Potvrdi ${monthTitle(scanReferenceMonth)}"
                                },
                                color = if (referenceMonthConfirmed) TaktoBlue else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    parsed.items.take(18).forEach { item ->
                        ScanPreviewRow(
                            store = store,
                            item = item,
                            onEdit = { editingItem = item }
                        )
                    }
                    if (parsed.items.size > 18) {
                        Text(
                            "Još ${parsed.items.size - 18} unosa bit će uključeno nakon potvrde.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Prepiši postojeće dane", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (overwrite) "Prepoznati raspored ima prednost na pronađenim datumima."
                                else "Već popunjeni datumi ostaju nepromijenjeni.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = overwrite, onCheckedChange = { overwrite = it })
                    }

                    Text(
                        "Ako na slici nema vremena rada, koristi se spremljeno zadano vrijeme oznake. Ako ni ono nije postavljeno, unos se sprema bez radnih sati.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            val items = result?.items.orEmpty()
            Button(
                onClick = {
                    val imported = store.importScannedSchedule(items, overwrite)
                    Toast.makeText(
                        context,
                        buildString {
                            append("Uvezeno ").append(imported.imported)
                            if (imported.freeDays > 0) {
                                append(" · slobodno ").append(imported.freeDays)
                            }
                            append(" · preskočeno ").append(imported.skipped)
                        },
                        Toast.LENGTH_LONG
                    ).show()
                    onDismiss()
                },
                enabled = items.isNotEmpty() &&
                    (result?.ambiguousDateCount ?: 0) == 0 &&
                    (result?.usedReferenceMonth != true || referenceMonthConfirmed) &&
                    !scanning,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) {
                Text("Uvezi u kalendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    editingItem?.let { item ->
        ScanItemEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { replacement -> replaceScannedItem(item, replacement) },
            onRemove = { replaceScannedItem(item, null) }
        )
    }
}

@Composable
private fun ScanPreviewRow(
    store: ScheduleStore,
    item: ScannedScheduleItem,
    onEdit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val freeDay = item.code == RosterScanParser.FREE_DAY_CODE
    val type = if (freeDay) null else store.shiftType(item.code)
    val preset = if (freeDay) null else store.workTimePreset(item.code)
    val start = item.startMinute ?: preset?.startMinute
    val end = item.endMinute ?: preset?.endMinute
    val pause = if (item.startMinute != null && item.endMinute != null) item.breakMinutes
        else preset?.breakMinutes ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (freeDay) colors.surfaceContainerHighest else type?.color ?: TaktoBlue,
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (freeDay) "—" else item.code,
                color = if (freeDay) colors.onSurfaceVariant else Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(croatianDate(item.date), fontWeight = FontWeight.SemiBold)
            if (freeDay) {
                Text(
                    "Slobodno",
                    color = colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            } else {
                Text(
                if (ScheduleLogic.isValidWorkTime(start, end, pause)) {
                    ScheduleLogic.formatClock(start) + " – " +
                        ScheduleLogic.formatClock(end) + " · " +
                        ScheduleLogic.formatDuration(
                            ScheduleLogic.workDurationMinutes(start, end, pause) ?: 0
                        )
                } else {
                    "Bez radnog vremena"
                },
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
                )
            }
        }
        TextButton(onClick = onEdit) {
            Text("Uredi", color = TaktoBlue, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScanItemEditDialog(
    item: ScannedScheduleItem,
    onDismiss: () -> Unit,
    onSave: (ScannedScheduleItem) -> Unit,
    onRemove: () -> Unit
) {
    var freeDay by remember(item) { mutableStateOf(item.code == RosterScanParser.FREE_DAY_CODE) }
    var codeText by remember(item) {
        mutableStateOf(if (item.code == RosterScanParser.FREE_DAY_CODE) "" else item.code)
    }
    var startText by remember(item) {
        mutableStateOf(item.startMinute?.let(ScheduleLogic::formatClock).orEmpty())
    }
    var endText by remember(item) {
        mutableStateOf(item.endMinute?.let(ScheduleLogic::formatClock).orEmpty())
    }
    var breakText by remember(item) {
        mutableStateOf(item.breakMinutes.takeIf { it > 0 }?.toString().orEmpty())
    }

    val cleanCode = ScheduleLogic.normalizeReusableCode(codeText)
    val start = ScheduleLogic.parseClock(startText)
    val end = ScheduleLogic.parseClock(endText)
    val pause = breakText.ifBlank { "0" }.toIntOrNull()
    val hasAnyTime = startText.isNotBlank() || endText.isNotBlank()
    val validTime = !hasAnyTime || (
        start != null &&
            end != null &&
            pause != null &&
            ScheduleLogic.isValidWorkTime(start, end, pause)
        )
    val valid = freeDay || (cleanCode.isNotBlank() && validTime)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Provjeri skenirani unos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    croatianDate(item.date),
                    fontWeight = FontWeight.ExtraBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Slobodan dan", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Uključi ako za ovaj datum nema radne smjene.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = freeDay,
                        onCheckedChange = { freeDay = it }
                    )
                }

                if (!freeDay) {
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it.take(ScheduleLogic.MAX_REUSABLE_CODE_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Oznaka smjene") },
                        supportingText = { Text("Primjer: J, N, D, GO, SD") },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        onValueChange = { breakText = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pauza (min)") },
                        placeholder = { Text("0") },
                        singleLine = true
                    )

                    if (hasAnyTime && !validTime) {
                        Text(
                            "Početak, kraj ili pauza nisu valjani. Ostavi oba vremena prazna ili unesi cijelo radno vrijeme.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    if (freeDay) {
                        onSave(
                            ScannedScheduleItem(
                                date = item.date,
                                code = RosterScanParser.FREE_DAY_CODE
                            )
                        )
                    } else {
                        onSave(
                            ScannedScheduleItem(
                                date = item.date,
                                code = cleanCode,
                                startMinute = if (hasAnyTime) start else null,
                                endMinute = if (hasAnyTime) end else null,
                                breakMinutes = if (hasAnyTime) pause ?: 0 else 0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) {
                Text("Spremi")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) {
                    Text("Ukloni", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Odustani")
                }
            }
        }
    )
}

private fun decodeRosterBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(resolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val largest = max(info.size.width, info.size.height).coerceAtLeast(1)
            val sample = ((largest + MAX_SCAN_IMAGE_DIMENSION - 1) / MAX_SCAN_IMAGE_DIMENSION).coerceAtLeast(1)
            decoder.setTargetSampleSize(sample)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_SCAN_IMAGE_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}.getOrNull()

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

private const val MAX_SCAN_IMAGE_DIMENSION = 3200
