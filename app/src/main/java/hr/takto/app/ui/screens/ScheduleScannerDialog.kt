package hr.takto.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.RosterScanParser
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.ScheduleScanParseResult
import hr.takto.app.model.ScannedScheduleItem
import hr.takto.app.ui.components.croatianDate
import hr.takto.app.ui.theme.TaktoBlue
import java.time.LocalDate

@Composable
fun ScheduleScannerDialog(
    store: ScheduleStore,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ScheduleScanParseResult?>(null) }
    var overwrite by remember { mutableStateOf(true) }

    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanning = true
        error = null
        result = null

        val input = runCatching { InputImage.fromFilePath(context, uri) }.getOrElse {
            scanning = false
            error = "Slika se ne može otvoriti."
            return@rememberLauncherForActivityResult
        }

        recognizer.process(input)
            .addOnSuccessListener { recognized ->
                scanning = false
                val parsed = RosterScanParser.parse(recognized.text, LocalDate.now())
                result = parsed
                if (parsed.items.isEmpty()) {
                    error = "Tekst je prepoznat, ali nisu pronađeni sigurni parovi datum + oznaka."
                }
            }
            .addOnFailureListener {
                scanning = false
                error = "Prepoznavanje teksta nije uspjelo. Pokušaj s oštrijom i ravnije fotografiranom slikom."
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uvezi raspored sa slike") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "OCR se obrađuje na uređaju. Slika se ne šalje u Takto cloud niti aplikacija traži INTERNET dopuštenje.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

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
                        if (result == null) "Odaberi sliku iz galerije" else "Odaberi drugu sliku",
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
                        Text("Prepoznajem datume, oznake i radno vrijeme…")
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                result?.takeIf { it.items.isNotEmpty() }?.let { parsed ->
                    Text(
                        "Pronađeno: " + parsed.items.size + " unosa",
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        parsed.detectedMonth?.let { "Mjesec: " + it.toString() }
                            ?: "Mjesec nije jasno pronađen; koristi se trenutačni mjesec.",
                        color = if (parsed.usedReferenceMonth) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    parsed.items.take(16).forEach { item ->
                        ScanPreviewRow(store = store, item = item)
                    }
                    if (parsed.items.size > 16) {
                        Text(
                            "Još " + (parsed.items.size - 16) + " unosa bit će uvezeno nakon potvrde.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Prepiši postojeće unose", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (overwrite) "Skenirani raspored ima prednost za pronađene datume."
                                else "Već popunjeni datumi ostaju netaknuti.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = overwrite, onCheckedChange = { overwrite = it })
                    }

                    Text(
                        "Ako OCR nije pronašao početak i kraj rada, Takto koristi spremljeno zadano radno vrijeme te oznake. Bez zadanog vremena unos se sprema bez sati.",
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
                        "Uvezeno: " + imported.imported + " · preskočeno: " + imported.skipped,
                        Toast.LENGTH_LONG
                    ).show()
                    onDismiss()
                },
                enabled = items.isNotEmpty() && !scanning,
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
    )
}

@Composable
private fun ScanPreviewRow(
    store: ScheduleStore,
    item: ScannedScheduleItem
) {
    val colors = MaterialTheme.colorScheme
    val type = store.shiftType(item.code)
    val preset = store.workTimePreset(item.code)
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
                .background(type?.color ?: TaktoBlue, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.code,
                color = Color.White,
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
            Text(
                if (ScheduleLogic.isValidWorkTime(start, end, pause)) {
                    ScheduleLogic.formatClock(start) + " – " +
                        ScheduleLogic.formatClock(end) + " · " +
                        ScheduleLogic.formatDuration(
                            ScheduleLogic.workDurationMinutes(start, end, pause) ?: 0
                        )
                } else {
                    "Bez prepoznatog ili zadanog radnog vremena"
                },
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
