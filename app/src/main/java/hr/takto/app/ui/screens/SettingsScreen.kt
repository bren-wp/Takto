package hr.takto.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import hr.takto.app.BuildConfig
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.AppThemeMode
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.EmploymentCatalog
import hr.takto.app.model.UserProfile
import hr.takto.app.reminders.ReminderScheduler
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.theme.TaktoBlue
import java.nio.charset.StandardCharsets

@Composable
fun SettingsScreen(store: ScheduleStore, contentPadding: PaddingValues) {
    val context = LocalContext.current
    var profileDialog by remember { mutableStateOf(false) }
    var appearanceDialog by remember { mutableStateOf(false) }
    var reminderDialog by remember { mutableStateOf(false) }
    var shiftReminderLeadDialog by remember { mutableStateOf(false) }
    var notificationPermissionTarget by remember { mutableStateOf<String?>(null) }
    var standardDayDialog by remember { mutableStateOf(false) }
    var workTimePresetCode by remember { mutableStateOf<String?>(null) }
    var workTimeListDialog by remember { mutableStateOf(false) }
    var showAllCustomPresets by remember { mutableStateOf(false) }
    var colorDialogCode by remember { mutableStateOf<String?>(null) }
    var customPresetDialogCode by remember { mutableStateOf<String?>(null) }
    var showNewCustomPresetDialog by remember { mutableStateOf(false) }
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingBackupContent by remember { mutableStateOf<String?>(null) }
    val notificationPermissionGranted = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (notificationPermissionTarget) {
                "shift" -> store.setShiftReminders(true)
                else -> store.setReminders(true)
            }
        }
        notificationPermissionTarget = null
        ReminderScheduler.sync(context, store)
        if (!granted) Toast.makeText(context, "Dopusti obavijesti da bi podsjetnici radili.", Toast.LENGTH_LONG).show()
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Nije moguće otvoriti odredišnu CSV datoteku.")
                stream.use { it.write(store.exportCsv().toByteArray(StandardCharsets.UTF_8)) }
            }.onSuccess {
                Toast.makeText(context, "Raspored je izvezen.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Izvoz nije uspio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Nije moguće otvoriti odredišnu iCalendar datoteku.")
                stream.use { it.write(store.exportICalendar().toByteArray(StandardCharsets.UTF_8)) }
            }.onSuccess {
                Toast.makeText(context, "iCalendar datoteka je izvezena.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "iCalendar izvoz nije uspio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                readUtf8TextLimited(context, uri, ScheduleStore.MAX_IMPORT_CHARS)
            }.onSuccess { text ->
                if (text.isBlank()) {
                    Toast.makeText(context, "Odabrana CSV datoteka je prazna.", Toast.LENGTH_SHORT).show()
                } else {
                    pendingCsvContent = text
                }
            }.onFailure { error ->
                val message = if (error is ImportFileTooLargeException) {
                    "CSV datoteka je prevelika za siguran uvoz."
                } else {
                    "CSV datoteku nije moguće pročitati."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Nije moguće otvoriti odredišnu backup datoteku.")
                stream.use { it.write(store.exportBackupJson().toByteArray(StandardCharsets.UTF_8)) }
            }.onSuccess {
                Toast.makeText(context, "Sigurnosna kopija je spremljena.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Sigurnosna kopija nije spremljena.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportArchiveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-ndjson")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Nije moguće otvoriti odredišnu arhivsku datoteku.")
                stream.use { store.writeArchiveTo(it) }
            }.onSuccess {
                Toast.makeText(context, "Trajna arhiva rasporeda je izvezena.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Arhivu nije moguće izvesti.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                readUtf8TextLimited(context, uri, ScheduleStore.MAX_IMPORT_CHARS)
            }.onSuccess { text ->
                if (text.isBlank()) {
                    Toast.makeText(context, "Sigurnosna kopija je prazna.", Toast.LENGTH_SHORT).show()
                } else {
                    pendingBackupContent = text
                }
            }.onFailure { error ->
                val message = if (error is ImportFileTooLargeException) {
                    "Sigurnosna kopija je prevelika za siguran uvoz."
                } else {
                    "Sigurnosnu kopiju nije moguće pročitati."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun enableNotificationFeature(target: String) {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermissionTarget = target
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            if (target == "shift") store.setShiftReminders(true) else store.setReminders(true)
            ReminderScheduler.sync(context, store)
        }
    }

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
            Text("Postavke", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Postavke i personalizacija", style = MaterialTheme.typography.headlineLarge)
            Text("Sve važne postavke organizirane na jednom mjestu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val profile = store.userProfile.value
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { profileDialog = true }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(TaktoBlue.copy(alpha = 0.14f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TaktoBlue)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        profile.fullName.ifBlank { "Moj radni profil" },
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        listOf(profile.sector, profile.industry)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "Ime, sektor, djelatnost, ustanova i radno mjesto" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                    if (profile.organizationName.isNotBlank() || profile.position.isNotBlank()) {
                        Text(
                            listOf(profile.organizationName, profile.position)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
                Icon(Icons.Default.Edit, contentDescription = "Uredi radni profil", tint = TaktoBlue)
            }
        }

        SettingsSectionTitle("Profil i raspored")

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = TaktoBlue)
                    Text("Oznake rasporeda", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp))
                }
                Text("Gotove oznake možeš prilagoditi, a vlastite oznake mogu predstavljati bilo koji tip rada, obveze ili odsutnosti.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    store.shiftTypes().forEach { type ->
                        Column(modifier = Modifier.weight(1f).clickable { colorDialogCode = type.code }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(type.color, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type.code, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = if (type.code.length == 1) 20.sp else 16.sp)
                            }
                            Text(type.name, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
                Text("Vlastite brze oznake", fontWeight = FontWeight.SemiBold)
                if (store.customShiftTypes().isEmpty()) {
                    Text("Još nema spremljenih vlastitih oznaka. Možeš ih dodati i kasnije birati jednim dodirom u kalendaru.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    store.customShiftTypes()
                        .take(if (showAllCustomPresets) Int.MAX_VALUE else 4)
                        .forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                .clickable { customPresetDialogCode = type.code }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(44.dp).background(type.color, RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type.code, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (type.code.length <= 3) 15.sp else 11.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(type.name, fontWeight = FontWeight.SemiBold)
                                Text("Dodirni za uređivanje", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            IconButton(onClick = { store.removeCustomShiftPreset(type.code) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Ukloni vlastitu oznaku", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (store.customShiftTypes().size > 4) {
                    TextButton(onClick = { showAllCustomPresets = !showAllCustomPresets }) {
                        Text(
                            if (showAllCustomPresets) "Prikaži manje" else "Prikaži sve (${store.customShiftTypes().size})",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Button(
                    onClick = { showNewCustomPresetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(" Dodaj vlastitu brzu oznaku", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text("Vlastiti jednokratni unos", fontWeight = FontWeight.Bold)
                            Text("I dalje možeš upisati bilo što bez spremanja među brze oznake.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                Text("Radno vrijeme po oznakama", fontWeight = FontWeight.SemiBold)
                Text(
                    "Početak, kraj i pauzu uređuješ u zasebnom pregledu kako postavke ne bi bile prenatrpane.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Button(
                    onClick = { workTimeListDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        " Uredi radno vrijeme po oznakama",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SettingsSectionTitle("Podsjetnici i radno vrijeme")

        SettingsRow(
            icon = Icons.Default.Notifications,
            title = "Dnevni podsjetnik",
            subtitle = when {
                store.remindersEnabled.value && !notificationPermissionGranted -> "Potrebno je dopustiti obavijesti"
                store.remindersEnabled.value -> "Uključen oko ${timeText(store.reminderHour.value, store.reminderMinute.value)}"
                else -> "Isključen"
            },
            trailing = {
                Switch(
                    checked = store.remindersEnabled.value && notificationPermissionGranted,
                    onCheckedChange = { checked ->
                        if (checked) enableNotificationFeature("daily")
                        else {
                            store.setReminders(false)
                            ReminderScheduler.cancelDaily(context)
                        }
                    }
                )
            }
        )

        SettingsRow(
            icon = Icons.Default.AccessTime,
            title = "Vrijeme podsjetnika",
            subtitle = "Svaki dan oko ${timeText(store.reminderHour.value, store.reminderMinute.value)} ako postoji unos",
            onClick = { reminderDialog = true }
        )

        SettingsRow(
            icon = Icons.Default.Notifications,
            title = "Podsjetnik prije rada",
            subtitle = when {
                store.shiftRemindersEnabled.value && !notificationPermissionGranted -> "Potrebno je dopustiti obavijesti"
                store.shiftRemindersEnabled.value -> "Uključen · ${reminderLeadText(store.shiftReminderLeadMinutes.value)}"
                else -> "Isključen · radi za dane s upisanim početkom rada"
            },
            trailing = {
                Switch(
                    checked = store.shiftRemindersEnabled.value && notificationPermissionGranted,
                    onCheckedChange = { checked ->
                        if (checked) enableNotificationFeature("shift")
                        else {
                            store.setShiftReminders(false)
                            ReminderScheduler.cancelShift(context)
                        }
                    }
                )
            }
        )

        SettingsRow(
            icon = Icons.Default.AccessTime,
            title = "Koliko ranije podsjetiti",
            subtitle = reminderLeadText(store.shiftReminderLeadMinutes.value),
            onClick = { shiftReminderLeadDialog = true }
        )

        SettingsRow(
            icon = Icons.Default.AccessTime,
            title = "Standardni radni dan",
            subtitle = "${ScheduleLogic.formatDuration(store.standardDailyMinutes.value)} · koristi se za izračun prekovremenih sati",
            onClick = { standardDayDialog = true }
        )

        SettingsSectionTitle("Podaci, uvoz i sigurnosne kopije")

        SettingsRow(
            icon = Icons.Default.Download,
            title = "Uvezi raspored",
            subtitle = "CSV sa zarezom ili točka-zarezom; prije uvoza biraš čuvanje ili prepisivanje postojećih dana",
            onClick = { importCsvLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) }
        )

        SettingsRow(
            icon = Icons.Default.Upload,
            title = "Izvezi CSV",
            subtitle = "Spremi cijeli raspored kao tabličnu datoteku",
            onClick = { exportCsvLauncher.launch("Takto-raspored.csv") }
        )

        SettingsRow(
            icon = Icons.Default.Upload,
            title = "Izvezi iCalendar (.ics)",
            subtitle = "Otvori Takto raspored u Google Kalendaru, Outlooku, Apple Kalendaru ili drugoj kalendarskoj aplikaciji",
            onClick = { exportIcsLauncher.launch("Takto-raspored.ics") }
        )

        SettingsRow(
            icon = Icons.Default.Share,
            title = "Podijeli raspored",
            subtitle = "Pošalji CSV tekst kroz aplikaciju po izboru, bez mrežnih dozvola u Taktu",
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Takto raspored")
                    putExtra(Intent.EXTRA_TEXT, store.exportCsv())
                }
                context.startActivity(Intent.createChooser(shareIntent, "Podijeli Takto raspored"))
            }
        )

        SettingsRow(
            icon = Icons.Default.Backup,
            title = "Sigurnosna kopija",
            subtitle = "Izvezi puni Takto backup u JSON",
            onClick = { exportBackupLauncher.launch("Takto-backup.json") }
        )

        SettingsRow(
            icon = Icons.Default.Backup,
            title = "Vrati sigurnosnu kopiju",
            subtitle = "Spoji backup s postojećim podacima; Takto ne briše datume koji nisu u backupu",
            onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) }
        )

        SettingsRow(
            icon = Icons.Default.History,
            title = "Trajna arhiva rasporeda",
            subtitle = "${store.archiveRevisionCount.value} spremljenih promjena · nema automatskog brisanja starih ni budućih rasporeda",
            onClick = { exportArchiveLauncher.launch("Takto-trajna-arhiva.jsonl") }
        )

        SettingsSectionTitle("Izgled i aplikacija")

        SettingsRow(
            icon = when (store.themeMode.value) {
                AppThemeMode.LIGHT -> Icons.Default.LightMode
                AppThemeMode.DARK -> Icons.Default.DarkMode
                AppThemeMode.SYSTEM -> Icons.Default.ColorLens
            },
            title = "Izgled",
            subtitle = when (store.themeMode.value) {
                AppThemeMode.LIGHT -> "Svijetli način"
                AppThemeMode.DARK -> "Tamni način · poboljšan kontrast"
                AppThemeMode.SYSTEM -> "Prema postavci uređaja"
            },
            onClick = { appearanceDialog = true }
        )

        SettingsRow(
            icon = Icons.Default.ColorLens,
            title = "Boje oznaka",
            subtitle = "Dodirni obojenu oznaku u odjeljku iznad i prilagodi boju"
        )

        SettingsRow(
            icon = Icons.Default.Language,
            title = "Jezik",
            subtitle = "Hrvatski (HR)"
        )

        SettingsRow(
            icon = Icons.Default.Slideshow,
            title = "Ponovno prikaži uvod",
            subtitle = "Vrati onboarding bez brisanja rasporeda",
            onClick = store::resetOnboarding
        )

        SettingsRow(
            icon = Icons.Default.Info,
            title = "O aplikaciji Takto",
            subtitle = "Verzija ${BuildConfig.VERSION_NAME} · Dodirni. Označi. Radi."
        )

        SettingsRow(
            icon = Icons.Default.History,
            title = "Čuvanje rasporeda",
            subtitle = "Povijesni i budući rasporedi ostaju spremljeni dok ih korisnik pojedinačno ne promijeni."
        )

        SettingsSectionTitle("Brendigo i podrška")

        SettingsRow(
            icon = Icons.Default.Public,
            title = "Brendigo",
            subtitle = "Takto je izradio Brendigo · brendigo.com",
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://brendigo.com")))
                }
            }
        )

        SettingsRow(
            icon = Icons.Default.Email,
            title = "Podrška",
            subtitle = "info@brendigo.com",
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@brendigo.com?subject=Takto%20podrska")))
                }
            }
        )

        Text(
            "© 2026 Brendigo · Takto ${BuildConfig.VERSION_NAME}",
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }


    if (appearanceDialog) {
        AppearanceDialog(
            current = store.themeMode.value,
            onDismiss = { appearanceDialog = false },
            onSelect = { mode ->
                store.setThemeMode(mode)
                appearanceDialog = false
            }
        )
    }

    if (profileDialog) {
        UserProfileDialog(
            initial = store.userProfile.value,
            onDismiss = { profileDialog = false },
            onSave = { profile ->
                store.saveUserProfile(profile)
                profileDialog = false
                Toast.makeText(context, "Radni profil je spremljen.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    pendingCsvContent?.let { content ->
        ImportModeDialog(
            title = "Kako uvesti CSV?",
            description = "Odaberi što napraviti s datumima koji već imaju spremljen unos.",
            safeLabel = "Sačuvaj postojeće",
            replaceLabel = "Prepiši postojeće",
            onDismiss = { pendingCsvContent = null },
            onSafe = {
                val result = store.importCsv(content, overwriteExisting = false)
                pendingCsvContent = null
                if (result.valid) {
                    Toast.makeText(
                        context,
                        "Uvezeno: ${result.imported} · preskočeno: ${result.skipped} · slobodno: ${result.freeDays}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "CSV nije valjan, ima nezatvorene navodnike ili je prevelik.", Toast.LENGTH_LONG).show()
                }
            },
            onReplace = {
                val result = store.importCsv(content, overwriteExisting = true)
                pendingCsvContent = null
                if (result.valid) {
                    Toast.makeText(
                        context,
                        "Uvezeno: ${result.imported} · preskočeno: ${result.skipped} · slobodno: ${result.freeDays}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "CSV nije valjan, ima nezatvorene navodnike ili je prevelik.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    pendingBackupContent?.let { content ->
        ImportModeDialog(
            title = "Kako vratiti sigurnosnu kopiju?",
            description = "Spajanje čuva postojeće datume. Ažuriranje prepisuje samo datume koji postoje u backupu; ostali stari i budući rasporedi ostaju netaknuti.",
            safeLabel = "Spoji bez prepisivanja",
            replaceLabel = "Spoji i ažuriraj datume",
            onDismiss = { pendingBackupContent = null },
            onSafe = {
                val result = store.importBackupJson(content, replaceExisting = false)
                pendingBackupContent = null
                if (result.valid) {
                    ReminderScheduler.sync(context, store)
                    Toast.makeText(context, "Vraćeno: ${result.imported} · preskočeno: ${result.skipped}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Takto sigurnosna kopija nije valjana ili je iz novije nepodržane verzije.", Toast.LENGTH_LONG).show()
                }
            },
            onReplace = {
                val result = store.importBackupJson(content, replaceExisting = true)
                pendingBackupContent = null
                if (result.valid) {
                    ReminderScheduler.sync(context, store)
                    Toast.makeText(context, "Vraćeno: ${result.imported} · preskočeno: ${result.skipped}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Takto sigurnosna kopija nije valjana ili je iz novije nepodržane verzije.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }


    colorDialogCode?.let { code ->
        val type = store.shiftType(code)
        if (type != null) {
            ShiftColorDialog(
                code = type.code,
                name = type.name,
                currentColorArgb = store.shiftColors[type.code] ?: ScheduleStore.DEFAULT_CUSTOM_COLOR,
                onDismiss = { colorDialogCode = null },
                onSelect = { argb ->
                    store.setShiftColor(type.code, argb)
                    colorDialogCode = null
                },
                onResetAll = {
                    store.resetShiftColors()
                    colorDialogCode = null
                }
            )
        }
    }

    if (showNewCustomPresetDialog) {
        CustomShiftPresetDialog(
            initialCode = "",
            initialName = "",
            initialColorArgb = ScheduleStore.DEFAULT_CUSTOM_COLOR,
            onDismiss = { showNewCustomPresetDialog = false },
            onSave = { code, name, color ->
                val saved = store.saveCustomShiftPreset(code, name, color)
                Toast.makeText(
                    context,
                    if (saved) "Vlastita oznaka je spremljena." else "Oznaka nije spremljena. Kod mora biti različit od D, N, GO, BO i PD.",
                    Toast.LENGTH_LONG
                ).show()
                if (saved) showNewCustomPresetDialog = false
            }
        )
    }

    customPresetDialogCode?.let { code ->
        val type = store.shiftType(code)
        if (type != null && !type.isPreset) {
            CustomShiftPresetDialog(
                initialCode = type.code,
                initialName = type.name,
                initialColorArgb = store.customShiftPresets[type.code]?.colorArgb ?: ScheduleStore.DEFAULT_CUSTOM_COLOR,
                lockCode = true,
                onDismiss = { customPresetDialogCode = null },
                onSave = { editedCode, name, color ->
                    val saved = store.saveCustomShiftPreset(editedCode, name, color)
                    if (saved) customPresetDialogCode = null
                }
            )
        }
    }

    if (reminderDialog) {
        ReminderTimeDialog(
            initialHour = store.reminderHour.value,
            initialMinute = store.reminderMinute.value,
            onDismiss = { reminderDialog = false },
            onSave = { hour, minute ->
                store.setReminderTime(hour, minute)
                ReminderScheduler.sync(context, store)
                reminderDialog = false
            }
        )
    }

    if (shiftReminderLeadDialog) {
        ShiftReminderLeadDialog(
            currentMinutes = store.shiftReminderLeadMinutes.value,
            onDismiss = { shiftReminderLeadDialog = false },
            onSelect = { minutes ->
                store.setShiftReminderLeadMinutes(minutes)
                ReminderScheduler.sync(context, store)
                shiftReminderLeadDialog = false
            }
        )
    }

    if (standardDayDialog) {
        StandardDayDialog(
            initialMinutes = store.standardDailyMinutes.value,
            onDismiss = { standardDayDialog = false },
            onSave = { minutes ->
                store.setStandardDailyMinutes(minutes)
                standardDayDialog = false
            }
        )
    }

    if (workTimeListDialog) {
        WorkTimeListDialog(
            store = store,
            onDismiss = { workTimeListDialog = false },
            onSelect = { code ->
                workTimeListDialog = false
                workTimePresetCode = code
            }
        )
    }

    workTimePresetCode?.let { code ->
        val type = store.shiftType(code)
        if (type != null && !ScheduleLogic.isLeaveCode(type.code)) {
            WorkTimePresetDialog(
                code = type.code,
                name = type.name,
                preset = store.workTimePreset(type.code),
                onDismiss = { workTimePresetCode = null },
                onSave = { start, end, pause ->
                    val saved = store.saveWorkTimePreset(type.code, start, end, pause)
                    if (saved) {
                        Toast.makeText(context, "Zadano vrijeme za ${type.code} je spremljeno.", Toast.LENGTH_SHORT).show()
                        workTimePresetCode = null
                    }
                },
                onClear = {
                    store.clearWorkTimePreset(type.code)
                    workTimePresetCode = null
                }
            )
        }
    }

}

@Composable
private fun WorkTimeListDialog(
    store: ScheduleStore,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val types = store.allShiftTypes().filterNot { ScheduleLogic.isLeaveCode(it.code) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Radno vrijeme po oznakama") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Odaberi oznaku kojoj želiš postaviti zadano radno vrijeme. Postavka se primjenjuje na nove unose te oznake.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                types.forEach { type ->
                    val preset = store.workTimePreset(type.code)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .clickable { onSelect(type.code) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(type.color, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                type.code,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (type.code.length <= 3) 15.sp else 10.sp,
                                maxLines = 1
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(type.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (preset == null) "Nije postavljeno"
                                else "${ScheduleLogic.formatClock(preset.startMinute)} – ${ScheduleLogic.formatClock(preset.endMinute)} · pauza ${preset.breakMinutes} min",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            "Uredi",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}

@Composable
private fun ImportModeDialog(
    title: String,
    description: String,
    safeLabel: String,
    replaceLabel: String,
    onDismiss: () -> Unit,
    onSafe: () -> Unit,
    onReplace: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(onClick = onSafe, colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)) {
                Text(safeLabel)
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onReplace) { Text(replaceLabel, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Odustani") }
            }
        }
    )
}

@Composable
private fun CustomShiftPresetDialog(
    initialCode: String,
    initialName: String,
    initialColorArgb: Long,
    lockCode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    var code by remember(initialCode) { mutableStateOf(initialCode) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var colorArgb by remember(initialColorArgb) { mutableStateOf(initialColorArgb) }
    val normalizedCode = ScheduleLogic.normalizeReusableCode(code)
    val palette = listOf(
        0xFF22B8CFL, 0xFF2488FFL, 0xFF8B46F6L, 0xFF13D7A0L,
        0xFFFFB21DL, 0xFFFF4B55L, 0xFFEC4899L, 0xFF64748BL
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lockCode) "Uredi vlastitu oznaku" else "Nova vlastita oznaka") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Spremljena oznaka pojavljuje se kao brzi izbor u kalendaru. Jednokratni vlastiti unos i dalje ostaje dostupan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (!lockCode) code = it.take(ScheduleLogic.MAX_REUSABLE_CODE_LENGTH) },
                    label = { Text("Kratka oznaka") },
                    supportingText = { Text("Primjeri: 12-20, TEREN, EDU, L") },
                    readOnly = lockCode,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(ScheduleLogic.MAX_REUSABLE_NAME_LENGTH) },
                    label = { Text("Naziv") },
                    supportingText = { Text("Npr. Popodnevna smjena, Teren, Edukacija") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Boja", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    palette.take(4).forEach { option ->
                        ColorChoice(option, colorArgb == option, Modifier.weight(1f)) { colorArgb = option }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    palette.drop(4).forEach { option ->
                        ColorChoice(option, colorArgb == option, Modifier.weight(1f)) { colorArgb = option }
                    }
                }
                if (normalizedCode.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(48.dp).background(Color(colorArgb), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(normalizedCode, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                        Text(name.ifBlank { normalizedCode }, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(normalizedCode, name.ifBlank { normalizedCode }, colorArgb) },
                enabled = normalizedCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
private fun ShiftColorDialog(
    code: String,
    name: String,
    currentColorArgb: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onResetAll: () -> Unit
) {
    val palette = listOf(
        0xFF2488FFL,
        0xFF8B46F6L,
        0xFF13D7A0L,
        0xFFFFB21DL,
        0xFFFF4B55L,
        0xFF22B8CFL,
        0xFFEC4899L,
        0xFF64748BL
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Boja za $code · $name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Promjena boje ažurira i postojeće unose s ovom oznakom.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.take(4).forEach { argb ->
                        ColorChoice(argb, currentColorArgb == argb, Modifier.weight(1f)) { onSelect(argb) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.drop(4).forEach { argb ->
                        ColorChoice(argb, currentColorArgb == argb, Modifier.weight(1f)) { onSelect(argb) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } },
        dismissButton = { TextButton(onClick = onResetAll) { Text("Vrati zadane boje") } }
    )
}

@Composable
private fun ColorChoice(argb: Long, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(52.dp)
            .background(Color(argb), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
    }
}

@Composable
private fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var hourText by remember { mutableStateOf(initialHour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(initialMinute.toString().padStart(2, '0')) }
    val hour = hourText.toIntOrNull()
    val minute = minuteText.toIntOrNull()
    val valid = hour != null && minute != null && hour in 0..23 && minute in 0..59

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vrijeme podsjetnika") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Podsjetnik se prikazuje samo ako taj dan ima spremljen unos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Sat") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minute") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(hour ?: 7, minute ?: 0) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

private fun reminderLeadText(minutes: Int): String = when {
    minutes <= 0 -> "U trenutku početka"
    minutes < 60 -> "$minutes min prije rada"
    minutes % (24 * 60) == 0 -> {
        val days = minutes / (24 * 60)
        if (days == 1) "1 dan prije rada" else "$days dana prije rada"
    }
    minutes % 60 == 0 -> {
        val hours = minutes / 60
        if (hours == 1) "1 h prije rada" else "$hours h prije rada"
    }
    else -> "${minutes / 60} h ${minutes % 60} min prije rada"
}

@Composable
private fun ShiftReminderLeadDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val options = listOf(0, 15, 30, 60, 120, 240, 720, 1440)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Podsjetnik prije rada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Takto prati sljedeći radni unos s upisanim početkom i obavještava te prije njega. Nakon obavijesti automatski zakazuje sljedeći.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { minutes ->
                    TextButton(
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            (if (minutes == currentMinutes) "✓  " else "   ") + reminderLeadText(minutes),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (minutes == currentMinutes) TaktoBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}

@Composable
private fun WorkTimePresetDialog(
    code: String,
    name: String,
    preset: hr.takto.app.model.WorkTimePreset?,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit,
    onClear: () -> Unit
) {
    var startText by remember(code, preset) { mutableStateOf(preset?.startMinute?.let(ScheduleLogic::formatClock) ?: "07:00") }
    var endText by remember(code, preset) { mutableStateOf(preset?.endMinute?.let(ScheduleLogic::formatClock) ?: "15:00") }
    var breakText by remember(code, preset) { mutableStateOf((preset?.breakMinutes ?: 0).toString()) }
    val start = ScheduleLogic.parseClock(startText)
    val end = ScheduleLogic.parseClock(endText)
    val pause = breakText.trim().toIntOrNull()
    val duration = if (start != null && end != null && pause != null) ScheduleLogic.workDurationMinutes(start, end, pause) else null
    val valid = start != null && end != null && pause != null && pause in 0..ScheduleLogic.MAX_BREAK_MINUTES && (duration ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zadano vrijeme · $code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$name · novo dodijeljeni dani mogu automatski dobiti ovo radno vrijeme.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it.take(5) },
                        label = { Text("Početak") },
                        placeholder = { Text("07:00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it.take(5) },
                        label = { Text("Kraj") },
                        placeholder = { Text("15:00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { breakText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Pauza (min)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (valid) {
                    Text("Neto trajanje: ${ScheduleLogic.formatDuration(duration ?: 0)}", color = TaktoBlue, fontWeight = FontWeight.Bold)
                } else {
                    Text("Provjeri početak, kraj i pauzu. Noćna smjena može završiti sljedeći dan.", color = Color(0xFFFFB21D), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onSave(start!!, end!!, pause!!) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = {
            Row {
                if (preset != null) TextButton(onClick = onClear) { Text("Ukloni", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Odustani") }
            }
        }
    )
}

@Composable
private fun StandardDayDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var hourText by remember(initialMinutes) { mutableStateOf((initialMinutes / 60).toString()) }
    var minuteText by remember(initialMinutes) { mutableStateOf((initialMinutes % 60).toString().padStart(2, '0')) }
    val hours = hourText.toIntOrNull()
    val minutes = minuteText.toIntOrNull()
    val total = if (hours != null && minutes != null && hours in 0..24 && minutes in 0..59) hours * 60 + minutes else null
    val valid = total != null && total in ScheduleStore.MIN_STANDARD_DAILY_MINUTES..ScheduleStore.MAX_STANDARD_DAILY_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Standardni radni dan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Takto prekovremeno računa po danu kao vrijeme iznad ove vrijednosti. Ne mijenja spremljene smjene.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Sati") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minute") },
                        singleLine = true
                    )
                }
                if (valid) Text("Standard: ${ScheduleLogic.formatDuration(total ?: 0)}", color = TaktoBlue, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (valid) onSave(total!!) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

private class ImportFileTooLargeException : IllegalArgumentException()

private fun readUtf8TextLimited(
    context: android.content.Context,
    uri: Uri,
    maxChars: Int
): String {
    val stream = context.contentResolver.openInputStream(uri)
        ?: error("Datoteku nije moguće otvoriti.")
    return stream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
        val output = StringBuilder(minOf(maxChars, 64 * 1024))
        val buffer = CharArray(8 * 1024)
        var total = 0
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break
            if (total + read > maxChars) throw ImportFileTooLargeException()
            output.append(buffer, 0, read)
            total += read
        }
        output.toString()
    }
}

private fun timeText(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
    )
}

@Composable
private fun AppearanceDialog(
    current: AppThemeMode,
    onDismiss: () -> Unit,
    onSelect: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izgled aplikacije") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Odaberi prikaz koji ti najbolje odgovara. Promjena se primjenjuje odmah i ostaje spremljena.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    Triple(AppThemeMode.DARK, "Tamni način", "Primarni Takto izgled s podignutom tamnom paletom i jasnim kontrastom"),
                    Triple(AppThemeMode.LIGHT, "Svijetli način", "Svijetle površine i tamni tekst za dnevni rad"),
                    Triple(AppThemeMode.SYSTEM, "Prema uređaju", "Takto automatski prati svijetli ili tamni način uređaja")
                ).forEach { (mode, title, subtitle) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (mode == current) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (mode) {
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Default.ColorLens
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        if (mode == current) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
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
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    destructive: Boolean = false
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        padding = PaddingValues(14.dp),
        corner = 17.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(
                    if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else TaktoBlue.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (destructive) MaterialTheme.colorScheme.error else TaktoBlue)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            trailing?.invoke()
        }
    }
}


@Composable
private fun UserProfileDialog(
    initial: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var fullName by remember(initial) { mutableStateOf(initial.fullName) }
    var sector by remember(initial) { mutableStateOf(initial.sector) }
    var industry by remember(initial) { mutableStateOf(initial.industry) }
    var institutionType by remember(initial) { mutableStateOf(initial.institutionType) }
    var organizationName by remember(initial) { mutableStateOf(initial.organizationName) }
    var position by remember(initial) { mutableStateOf(initial.position) }
    var picker by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Work, contentDescription = null, tint = TaktoBlue)
                Text(" Radni profil")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Sva polja ostaju spremljena na uređaju. Popisi su prijedlozi — možeš upisati bilo koju javnu ili državnu ustanovu i bilo koje radno mjesto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ime i prezime") },
                    singleLine = true
                )
                ProfileFieldWithSuggestions(
                    label = "Sektor",
                    value = sector,
                    onValueChange = { sector = it.take(120) },
                    onSuggestions = { picker = "sector" }
                )
                ProfileFieldWithSuggestions(
                    label = "Djelatnost / industrija",
                    value = industry,
                    onValueChange = { industry = it.take(120) },
                    onSuggestions = { picker = "industry" }
                )
                ProfileFieldWithSuggestions(
                    label = "Vrsta ustanove",
                    value = institutionType,
                    onValueChange = { institutionType = it.take(120) },
                    onSuggestions = { picker = "institution" }
                )
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Naziv ustanove / poslodavca") },
                    singleLine = true
                )
                ProfileFieldWithSuggestions(
                    label = "Radno mjesto / pozicija",
                    value = position,
                    onValueChange = { position = it.take(120) },
                    onSuggestions = { picker = "position" }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        UserProfile(
                            fullName = fullName,
                            sector = sector,
                            industry = industry,
                            institutionType = institutionType,
                            organizationName = organizationName,
                            position = position
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )

    picker?.let { kind ->
        val title: String
        val options: List<String>
        val current: String
        when (kind) {
            "sector" -> {
                title = "Odaberi sektor"
                options = EmploymentCatalog.sectors
                current = sector
            }
            "industry" -> {
                title = "Odaberi djelatnost"
                options = EmploymentCatalog.industries
                current = industry
            }
            "institution" -> {
                title = "Odaberi vrstu ustanove"
                options = EmploymentCatalog.institutionTypes
                current = institutionType
            }
            else -> {
                title = "Odaberi radno mjesto"
                options = EmploymentCatalog.commonPositions
                current = position
            }
        }

        OptionPickerDialog(
            title = title,
            options = options,
            current = current,
            onDismiss = { picker = null },
            onSelect = { selected ->
                when (kind) {
                    "sector" -> sector = selected
                    "industry" -> industry = selected
                    "institution" -> institutionType = selected
                    else -> position = selected
                }
                picker = null
            }
        )
    }
}

@Composable
private fun ProfileFieldWithSuggestions(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSuggestions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true
        )
        TextButton(
            onClick = onSuggestions,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Prijedlozi", color = TaktoBlue, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OptionPickerDialog(
    title: String,
    options: List<String>,
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (option == current) TaktoBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(option, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (option == current) Text("✓", color = TaktoBlue, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}
