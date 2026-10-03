package hr.takto.app.data

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import hr.takto.app.model.CustomShiftPreset
import hr.takto.app.model.ICalendarExporter
import hr.takto.app.model.DefaultShiftTypes
import hr.takto.app.model.DefaultWorkTimePresets
import hr.takto.app.model.SavedPattern
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.SchedulePersistencePolicy
import hr.takto.app.model.ScheduleRecovery
import hr.takto.app.model.ScannedScheduleItem
import hr.takto.app.model.RosterScanParser
import hr.takto.app.model.ScheduleSuggestions
import hr.takto.app.model.ShiftEntry
import hr.takto.app.model.ShiftType
import hr.takto.app.model.WorkTimePreset
import hr.takto.app.model.UserProfile
import hr.takto.app.model.PayrollProfile
import hr.takto.app.model.PayrollSystem
import hr.takto.app.model.PensionMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

/**
 * Jedini izvor istine za Takto raspored i korisničke postavke.
 *
 * Dnevni raspored sprema se u atomsku datotečnu snimku, uz prethodnu recovery
 * kopiju i append-only revizijsku arhivu. Time veliki višegodišnji rasporedi
 * više ne ovise o jednom velikom SharedPreferences stringu, a prekid procesa
 * tijekom spremanja ne može ostaviti napola zapisanu glavnu snimku.
 *
 * SharedPreferences ostaje za male postavke i kao jednokratni legacy izvor
 * pri migraciji starijih instalacija.
 */
class ScheduleStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val entries = mutableStateMapOf<LocalDate, ShiftEntry>()
    val shiftColors = mutableStateMapOf<String, Long>()
    val customShiftPresets = mutableStateMapOf<String, CustomShiftPreset>()
    val savedPatterns = mutableStateListOf<SavedPattern>()
    val workTimePresets = mutableStateMapOf<String, WorkTimePreset>()
    val monthlyTargetOverrides = mutableStateMapOf<String, Int>()
    val userProfile = mutableStateOf(loadUserProfile())
    val payrollProfile = mutableStateOf(loadPayrollProfile())
    val archiveRevisionCount = mutableStateOf(0)
    private var persistedSnapshot: Map<LocalDate, ShiftEntry> = emptyMap()
    private var lastCheckpointRevisionCount = 0
    private data class StoredScheduleSnapshot(
        val entries: Map<LocalDate, ShiftEntry>,
        val archiveRevisionCount: Int,
        val archiveByteOffset: Long? = null
    )

    val onboardingDone = mutableStateOf(prefs.getBoolean(KEY_ONBOARDING, false))
    val remindersEnabled = mutableStateOf(prefs.getBoolean(KEY_REMINDERS, false))
    val reminderHour = mutableStateOf(prefs.getInt(KEY_REMINDER_HOUR, 7).coerceIn(0, 23))
    val reminderMinute = mutableStateOf(prefs.getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59))
    val shiftRemindersEnabled = mutableStateOf(prefs.getBoolean(KEY_SHIFT_REMINDERS, false))
    val shiftReminderLeadMinutes = mutableStateOf(
        prefs.getInt(KEY_SHIFT_REMINDER_LEAD_MINUTES, DEFAULT_SHIFT_REMINDER_LEAD_MINUTES)
            .coerceIn(0, MAX_SHIFT_REMINDER_LEAD_MINUTES)
    )
    val standardDailyMinutes = mutableStateOf(
        prefs.getInt(KEY_STANDARD_DAILY_MINUTES, DEFAULT_STANDARD_DAILY_MINUTES)
            .coerceIn(MIN_STANDARD_DAILY_MINUTES, MAX_STANDARD_DAILY_MINUTES)
    )
    val canUndo = mutableStateOf(false)
    val undoLabel = mutableStateOf("")
    private var undoState: UndoState? = null

    init {
        loadShiftColors()
        loadCustomShiftPresets()
        loadWorkTimePresets()
        seedDefaultWorkTimePresets()
        loadMonthlyTargetOverrides()
        loadSavedPatterns()
        archiveRevisionCount.value = loadArchiveRevisionCount()
        loadEntries()
        persistedSnapshot = entries.mapValues { (_, entry) -> entry.copy() }
    }

    fun shiftType(code: String): ShiftType? {
        val base = DefaultShiftTypes.presets.firstOrNull { it.code.equals(code, ignoreCase = true) }
        if (base != null) {
            val argb = shiftColors[base.code] ?: (base.color.toArgb().toLong() and 0xFFFFFFFFL)
            return base.copy(color = Color(argb))
        }
        val custom = customShiftPresets.values.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: return null
        return ShiftType(custom.code, custom.name, Color(custom.colorArgb), isPreset = false)
    }

    fun shiftTypes(): List<ShiftType> = DefaultShiftTypes.presets.mapNotNull { shiftType(it.code) }

    fun customShiftTypes(): List<ShiftType> = customShiftPresets.values
        .sortedBy { it.code }
        .map { ShiftType(it.code, it.name, Color(it.colorArgb), isPreset = false) }

    fun allShiftTypes(): List<ShiftType> = shiftTypes() + customShiftTypes()

    fun suggestedShiftTypes(referenceDate: LocalDate = LocalDate.now()): List<ShiftType> =
        ScheduleSuggestions.rank(
            types = allShiftTypes(),
            entries = entries.values,
            referenceDate = referenceDate,
            lookbackDays = SUGGESTION_LOOKBACK_DAYS
        )

    fun saveCustomShiftPreset(code: String, name: String, colorArgb: Long): Boolean {
        val normalizedCode = ScheduleLogic.normalizeReusableCode(code)
        val normalizedName = ScheduleLogic.normalizeDisplayName(name).ifBlank { normalizedCode }
        if (normalizedCode.isBlank() || normalizedCode == "-" || normalizedCode == "SLOBODNO") return false
        if (DefaultShiftTypes.presets.any { it.code.equals(normalizedCode, ignoreCase = true) }) return false
        if (!customShiftPresets.containsKey(normalizedCode) && customShiftPresets.size >= MAX_CUSTOM_PRESETS) return false

        val normalizedColor = colorArgb and 0xFFFFFFFFL
        customShiftPresets[normalizedCode] = CustomShiftPreset(normalizedCode, normalizedName, normalizedColor)
        entries.entries.toList().forEach { (date, entry) ->
            if (entry.code.equals(normalizedCode, ignoreCase = true)) {
                entries[date] = entry.copy(code = normalizedCode, label = normalizedName, colorArgb = normalizedColor)
            }
        }
        persistCustomShiftPresets()
        persistEntries()
        return true
    }

    fun removeCustomShiftPreset(code: String) {
        val key = customShiftPresets.keys.firstOrNull { it.equals(code, ignoreCase = true) } ?: return
        customShiftPresets.remove(key)
        workTimePresets.remove(key)
        persistCustomShiftPresets()
        persistWorkTimePresets()
    }

    fun workTimePreset(code: String): WorkTimePreset? =
        workTimePresets.entries.firstOrNull { it.key.equals(code, ignoreCase = true) }?.value

    fun saveWorkTimePreset(code: String, startMinute: Int, endMinute: Int, breakMinutes: Int): Boolean {
        val type = shiftType(code) ?: return false
        if (ScheduleLogic.isLeaveCode(type.code)) return false
        if (!ScheduleLogic.isValidWorkTime(startMinute, endMinute, breakMinutes)) return false
        val safe = WorkTimePreset(
            code = type.code,
            startMinute = startMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
            endMinute = endMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
            breakMinutes = breakMinutes.coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES)
        )
        workTimePresets[type.code] = safe
        persistWorkTimePresets()
        return true
    }

    fun clearWorkTimePreset(code: String) {
        val key = workTimePresets.keys.firstOrNull { it.equals(code, ignoreCase = true) } ?: return
        workTimePresets.remove(key)
        persistWorkTimePresets()
    }

    fun addSavedPattern(name: String, codes: List<String?>): Boolean {
        val cleanName = ScheduleLogic.normalizePatternName(name)
        val cleanCodes = codes.take(ScheduleLogic.MAX_PATTERN_STEPS).map { code ->
            code?.let(ScheduleLogic::normalizeReusableCode)?.takeIf { it.isNotBlank() }
        }
        if (cleanName.isBlank() || cleanCodes.isEmpty() || cleanCodes.all { it == null }) return false
        if (savedPatterns.size >= MAX_SAVED_PATTERNS) return false
        savedPatterns += SavedPattern(UUID.randomUUID().toString(), cleanName, cleanCodes)
        persistSavedPatterns()
        return true
    }

    fun removeSavedPattern(id: String) {
        if (savedPatterns.removeAll { it.id == id }) persistSavedPatterns()
    }

    fun setShiftColor(code: String, colorArgb: Long) {
        val preset = DefaultShiftTypes.presets.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: return
        val normalized = colorArgb and 0xFFFFFFFFL
        shiftColors[preset.code] = normalized
        entries.entries.filter { it.value.code.equals(preset.code, ignoreCase = true) }.forEach { (date, entry) ->
            entries[date] = entry.copy(colorArgb = normalized)
        }
        persistShiftColors()
        persistEntries()
    }

    fun resetShiftColors() {
        shiftColors.clear()
        DefaultShiftTypes.presets.forEach { type ->
            shiftColors[type.code] = type.color.toArgb().toLong() and 0xFFFFFFFFL
        }
        entries.entries.toList().forEach { (date, entry) ->
            shiftType(entry.code)?.let { type ->
                entries[date] = entry.copy(colorArgb = type.color.toArgb().toLong() and 0xFFFFFFFFL)
            }
        }
        persistShiftColors()
        persistEntries()
    }

    fun entryFor(date: LocalDate): ShiftEntry? = entries[date]

    fun entriesForMonth(month: YearMonth): List<ShiftEntry> =
        entries.values.filter { YearMonth.from(it.date) == month }.sortedBy { it.date }

    fun entriesBetween(start: LocalDate, endInclusive: LocalDate): List<ShiftEntry> =
        entries.values
            .filter { !it.date.isBefore(start) && !it.date.isAfter(endInclusive) }
            .sortedBy { it.date }

    fun nextEntry(from: LocalDate = LocalDate.now()): ShiftEntry? =
        entries.values.filter { !it.date.isBefore(from) }.minByOrNull { it.date }

    fun setEntry(date: LocalDate, type: ShiftType, note: String = "") {
        val before = captureUndo(listOf(date), "Promjena ${date}")
        val next = entryFromType(date, type, note, preserveExistingTime = true)
        if (entries[date] == next) return
        entries[date] = next
        persistEntries()
        commitUndo(before)
    }

    fun setCustomEntry(
        date: LocalDate,
        text: String,
        note: String = "",
        colorArgb: Long = DEFAULT_CUSTOM_COLOR
    ) {
        val clean = sanitizeCustomText(text)
        if (clean.isBlank()) return
        shiftType(clean)?.let { preset ->
            setEntry(date, preset, note)
            return
        }
        val before = captureUndo(listOf(date), "Vlastiti unos ${date}")
        val current = entries[date]
        val next = ShiftEntry(
            date = date,
            code = clean,
            label = clean,
            colorArgb = colorArgb and 0xFFFFFFFFL,
            note = note.trim().take(MAX_NOTE_LENGTH),
            startMinute = current?.startMinute,
            endMinute = current?.endMinute,
            breakMinutes = current?.breakMinutes ?: 0,
            overtimeMinutes = current?.overtimeMinutes ?: 0
        )
        if (current == next) return
        entries[date] = next
        persistEntries()
        commitUndo(before)
    }

    fun updateNote(date: LocalDate, note: String) {
        val current = entries[date] ?: return
        val next = current.copy(note = note.trim().take(MAX_NOTE_LENGTH))
        if (next == current) return
        val before = captureUndo(listOf(date), "Napomena ${date}")
        entries[date] = next
        persistEntries()
        commitUndo(before)
    }

    fun updateWorkTime(
        date: LocalDate,
        startMinute: Int,
        endMinute: Int,
        breakMinutes: Int,
        overtimeMinutes: Int = 0
    ): Boolean {
        val current = entries[date] ?: return false
        if (ScheduleLogic.isLeaveCode(current.code)) return false
        if (!ScheduleLogic.isValidWorkTime(startMinute, endMinute, breakMinutes)) return false
        val duration = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes) ?: return false
        if (overtimeMinutes !in 0..duration) return false
        val safeBreak = breakMinutes
        val next = current.copy(
            startMinute = startMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
            endMinute = endMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
            breakMinutes = safeBreak,
            overtimeMinutes = overtimeMinutes
        )
        if (next == current) return true
        val before = captureUndo(listOf(date), "Radno vrijeme ${date}")
        entries[date] = next
        persistEntries()
        commitUndo(before)
        return true
    }

    fun clearWorkTime(date: LocalDate): Boolean {
        val current = entries[date] ?: return false
        if (!current.hasWorkTime && current.breakMinutes == 0 && current.overtimeMinutes == 0) return false
        val before = captureUndo(listOf(date), "Uklanjanje radnog vremena ${date}")
        entries[date] = current.copy(
            startMinute = null,
            endMinute = null,
            breakMinutes = 0,
            overtimeMinutes = 0
        )
        persistEntries()
        commitUndo(before)
        return true
    }

    fun updateWorkTime(
        dates: Collection<LocalDate>,
        startMinute: Int,
        endMinute: Int,
        breakMinutes: Int,
        overtimeMinutes: Int = 0
    ): BulkEditResult {
        if (!ScheduleLogic.isValidWorkTime(startMinute, endMinute, breakMinutes)) {
            return BulkEditResult(0, 0, 0)
        }
        val duration = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)
            ?: return BulkEditResult(0, 0, 0)
        if (overtimeMinutes !in 0..duration) return BulkEditResult(0, 0, 0)
        val unique = dates.distinct().sorted().take(MAX_BULK_DAYS)
        val before = captureUndo(unique, "Radno vrijeme za ${unique.size} dana")
        val safeBreak = breakMinutes
        var changed = 0
        var skipped = 0
        unique.forEach { date ->
            val current = entries[date]
            if (current == null || ScheduleLogic.isLeaveCode(current.code)) {
                skipped++
                return@forEach
            }
            val next = current.copy(
                startMinute = startMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
                endMinute = endMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
                breakMinutes = safeBreak,
                overtimeMinutes = overtimeMinutes
            )
            if (next != current) {
                entries[date] = next
                changed++
            }
        }
        if (changed > 0) {
            persistEntries()
            commitUndo(before)
        }
        return BulkEditResult(changed, skipped, 0)
    }

    fun removeEntry(date: LocalDate) {
        if (!entries.containsKey(date)) return
        val before = captureUndo(listOf(date), "Uklonjen unos ${date}")
        entries.remove(date)
        persistEntries()
        commitUndo(before)
    }

    /**
     * Dodjeljuje istu oznaku većem broju datuma i stanje sprema samo jednom.
     * Kad je overwriteExisting=false, već popunjeni dani ostaju netaknuti.
     */
    fun setEntries(
        dates: Collection<LocalDate>,
        type: ShiftType,
        note: String = "",
        overwriteExisting: Boolean = true
    ): BulkEditResult {
        var changed = 0
        var skipped = 0
        val uniqueDates = dates.distinct().sorted().take(MAX_BULK_DAYS)
        val before = captureUndo(uniqueDates, "Uređivanje ${uniqueDates.size} dana")
        uniqueDates.forEach { date ->
            if (!overwriteExisting && entries.containsKey(date)) {
                skipped++
                return@forEach
            }
            val next = entryFromType(date, type, note, preserveExistingTime = true)
            if (entries[date] != next) {
                entries[date] = next
                changed++
            }
        }
        if (changed > 0) {
            persistEntries()
            commitUndo(before)
        }
        return BulkEditResult(changed = changed, skipped = skipped, freeDays = 0)
    }

    /** Bulk varijanta vlastitog unosa za višestruki odabir datuma. */
    fun setCustomEntries(
        dates: Collection<LocalDate>,
        text: String,
        note: String = "",
        colorArgb: Long = DEFAULT_CUSTOM_COLOR,
        overwriteExisting: Boolean = true
    ): BulkEditResult {
        val clean = sanitizeCustomText(text)
        if (clean.isBlank()) return BulkEditResult(0, 0, 0)
        shiftType(clean)?.let { preset ->
            return setEntries(dates, preset, note, overwriteExisting)
        }

        var changed = 0
        var skipped = 0
        val normalizedColor = colorArgb and 0xFFFFFFFFL
        val normalizedNote = note.trim().take(MAX_NOTE_LENGTH)
        val uniqueDates = dates.distinct().sorted().take(MAX_BULK_DAYS)
        val before = captureUndo(uniqueDates, "Vlastiti unos za ${uniqueDates.size} dana")
        uniqueDates.forEach { date ->
            if (!overwriteExisting && entries.containsKey(date)) {
                skipped++
                return@forEach
            }
            val current = entries[date]
            val next = ShiftEntry(
                date = date,
                code = clean,
                label = clean,
                colorArgb = normalizedColor,
                note = normalizedNote,
                startMinute = current?.startMinute,
                endMinute = current?.endMinute,
                breakMinutes = current?.breakMinutes ?: 0,
                overtimeMinutes = current?.overtimeMinutes ?: 0
            )
            if (entries[date] != next) {
                entries[date] = next
                changed++
            }
        }
        if (changed > 0) {
            persistEntries()
            commitUndo(before)
        }
        return BulkEditResult(changed = changed, skipped = skipped, freeDays = 0)
    }

    /**
     * Skupno primjenjuje stavke dobivene lokalnim OCR-om. Eksplicitno prepoznato
     * radno vrijeme ima prednost; inače se koristi spremljeno zadano vrijeme oznake.
     */
    fun importScannedSchedule(
        items: Collection<ScannedScheduleItem>,
        overwriteExisting: Boolean = true
    ): ImportResult {
        val normalized = items
            .filter { it.code.isNotBlank() }
            .distinctBy { it.date }
            .sortedBy { it.date }
            .take(MAX_BULK_DAYS)
        if (normalized.isEmpty()) return ImportResult(0, 0, 0)

        val before = captureUndo(normalized.map { it.date }, "Uvoz skeniranog rasporeda")
        var imported = 0
        var skipped = 0
        var freeDays = 0

        normalized.forEach { scanned ->
            val current = entries[scanned.date]
            if (!overwriteExisting && current != null) {
                skipped++
                return@forEach
            }

            if (scanned.code == RosterScanParser.FREE_DAY_CODE) {
                freeDays++
                if (overwriteExisting && current != null) {
                    entries.remove(scanned.date)
                    imported++
                }
                return@forEach
            }

            val type = shiftType(scanned.code)
            var next = if (type != null) {
                entryFromType(
                    date = scanned.date,
                    type = type,
                    note = "",
                    preserveExistingTime = false
                )
            } else {
                val code = ScheduleLogic.normalizeReusableCode(scanned.code)
                if (code.isBlank()) {
                    skipped++
                    return@forEach
                }
                ShiftEntry(
                    date = scanned.date,
                    code = code,
                    label = code,
                    colorArgb = importedCodeColor(code)
                )
            }

            if (
                !ScheduleLogic.isLeaveCode(next.code) &&
                ScheduleLogic.isValidWorkTime(
                    scanned.startMinute,
                    scanned.endMinute,
                    scanned.breakMinutes
                )
            ) {
                next = next.copy(
                    startMinute = scanned.startMinute,
                    endMinute = scanned.endMinute,
                    breakMinutes = scanned.breakMinutes
                )
            }

            if (entries[scanned.date] != next) {
                entries[scanned.date] = next
                imported++
            }
        }

        if (imported > 0) {
            persistEntries()
            commitUndo(before)
        }
        return ImportResult(imported, skipped, freeDays)
    }

    /** Postavlja odabrane datume kao slobodne dane. */
    fun removeEntries(dates: Collection<LocalDate>): BulkEditResult {
        var changed = 0
        val unique = dates.distinct().sorted().take(MAX_BULK_DAYS)
        val before = captureUndo(unique, "Slobodni dani (${unique.size})")
        unique.forEach { date ->
            if (entries.remove(date) != null) changed++
        }
        if (changed > 0) {
            persistEntries()
            commitUndo(before)
        }
        return BulkEditResult(changed = changed, skipped = 0, freeDays = unique.size)
    }

    /**
     * Snima ponedjeljak-nedjelja tjedna kao predložak. null znači dan bez unosa.
     * Snapshot je memorijski i ne mijenja spremljeni raspored.
     */
    fun copyWeek(dateInWeek: LocalDate): List<ShiftEntry?> =
        ScheduleLogic.weekDates(dateInWeek).map { entries[it]?.copy() }

    /**
     * Lijepi sedmodnevni snapshot na tjedan ciljnog datuma.
     * Ako overwriteExisting=true, slobodni dan iz izvornog tjedna briše ciljni unos.
     */
    fun pasteWeek(
        targetDateInWeek: LocalDate,
        snapshot: List<ShiftEntry?>,
        overwriteExisting: Boolean
    ): BulkEditResult {
        if (snapshot.size != 7) return BulkEditResult(0, 0, 0)
        val targetDates = ScheduleLogic.weekDates(targetDateInWeek)
        val before = captureUndo(targetDates, "Lijepljenje tjedna")
        var changed = 0
        var skipped = 0
        var freeDays = 0

        targetDates.forEachIndexed { index, targetDate ->
            val source = snapshot[index]
            val existing = entries[targetDate]
            if (!overwriteExisting && existing != null) {
                skipped++
                return@forEachIndexed
            }

            if (source == null) {
                freeDays++
                if (overwriteExisting && entries.remove(targetDate) != null) changed++
            } else {
                val next = source.copy(date = targetDate)
                if (existing != next) {
                    entries[targetDate] = next
                    changed++
                }
            }
        }
        if (changed > 0) {
            persistEntries()
            commitUndo(before)
        }
        return BulkEditResult(changed, skipped, freeDays)
    }

    fun saveUserProfile(profile: UserProfile) {
        val sanitized = profile.copy(
            fullName = profile.fullName.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_TEXT),
            sector = profile.sector.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_TEXT),
            industry = profile.industry.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_TEXT),
            institutionType = profile.institutionType.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_TEXT),
            organizationName = profile.organizationName.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_TEXT),
            position = profile.position.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_TEXT)
        )
        userProfile.value = sanitized
        prefs.edit().putString(KEY_USER_PROFILE, JSONObject().apply {
            put("fullName", sanitized.fullName)
            put("sector", sanitized.sector)
            put("industry", sanitized.industry)
            put("institutionType", sanitized.institutionType)
            put("organizationName", sanitized.organizationName)
            put("position", sanitized.position)
        }.toString()).apply()
    }

    fun savePayrollProfile(profile: PayrollProfile) {
        val sanitized = profile.copy(
            rolePresetId = profile.rolePresetId.trim().take(80),
            coefficient = profile.coefficient.coerceIn(0.0, 20.0),
            yearsOfService = profile.yearsOfService.coerceIn(0, 70),
            manualBaseEur = profile.manualBaseEur.coerceIn(0.0, 20_000.0),
            taxLocalityPresetId = profile.taxLocalityPresetId.trim().take(80),
            lowerTaxRatePercent = profile.lowerTaxRatePercent.coerceIn(0.0, 60.0),
            higherTaxRatePercent = profile.higherTaxRatePercent.coerceIn(0.0, 60.0),
            personalAllowanceEur = profile.personalAllowanceEur.coerceIn(0.0, 50_000.0),
            additionalGrossEur = profile.additionalGrossEur.coerceIn(0.0, 100_000.0),
            nonTaxableEur = profile.nonTaxableEur.coerceIn(0.0, 100_000.0),
            overtimePercent = profile.overtimePercent.coerceIn(0.0, 300.0),
            nightPercent = profile.nightPercent.coerceIn(0.0, 300.0),
            saturdayPercent = profile.saturdayPercent.coerceIn(0.0, 300.0),
            sundayPercent = profile.sundayPercent.coerceIn(0.0, 300.0),
            holidayPercent = profile.holidayPercent.coerceIn(0.0, 300.0),
            otherEmployersGrossEur = profile.otherEmployersGrossEur.coerceIn(0.0, 100_000.0),
            allAdjustmentsConfirmed = profile.allAdjustmentsConfirmed
        )
        payrollProfile.value = sanitized
        prefs.edit().putString(KEY_PAYROLL_PROFILE, payrollProfileToJson(sanitized).toString()).apply()
    }

    fun writeArchiveTo(output: OutputStream) {
        val file = context.getFileStreamPath(HISTORY_FILE)
        if (!file.exists()) return
        file.inputStream().buffered().use { input ->
            input.copyTo(output)
        }
    }

    fun finishOnboarding() {
        onboardingDone.value = true
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
    }

    fun resetOnboarding() {
        onboardingDone.value = false
        prefs.edit().putBoolean(KEY_ONBOARDING, false).apply()
    }

    fun setReminders(enabled: Boolean) {
        remindersEnabled.value = enabled
        prefs.edit().putBoolean(KEY_REMINDERS, enabled).apply()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        reminderHour.value = safeHour
        reminderMinute.value = safeMinute
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, safeHour)
            .putInt(KEY_REMINDER_MINUTE, safeMinute)
            .apply()
    }

    fun setShiftReminders(enabled: Boolean) {
        shiftRemindersEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SHIFT_REMINDERS, enabled).apply()
    }

    fun setShiftReminderLeadMinutes(minutes: Int) {
        val safe = minutes.coerceIn(0, MAX_SHIFT_REMINDER_LEAD_MINUTES)
        shiftReminderLeadMinutes.value = safe
        prefs.edit().putInt(KEY_SHIFT_REMINDER_LEAD_MINUTES, safe).apply()
    }

    fun setStandardDailyMinutes(minutes: Int) {
        val safe = minutes.coerceIn(MIN_STANDARD_DAILY_MINUTES, MAX_STANDARD_DAILY_MINUTES)
        standardDailyMinutes.value = safe
        prefs.edit().putInt(KEY_STANDARD_DAILY_MINUTES, safe).apply()
    }

    fun automaticMonthlyTargetMinutes(month: YearMonth): Int =
        ScheduleLogic.automaticMonthlyTargetMinutes(month, standardDailyMinutes.value)

    fun monthlyTargetMinutes(month: YearMonth): Int =
        monthlyTargetOverrides[month.toString()] ?: automaticMonthlyTargetMinutes(month)

    fun hasMonthlyTargetOverride(month: YearMonth): Boolean = monthlyTargetOverrides.containsKey(month.toString())

    fun setMonthlyTargetOverride(month: YearMonth, minutes: Int?) {
        val key = month.toString()
        if (minutes == null) monthlyTargetOverrides.remove(key)
        else monthlyTargetOverrides[key] = minutes.coerceIn(0, ScheduleLogic.MAX_MONTHLY_TARGET_MINUTES)
        persistMonthlyTargetOverrides()
    }

    fun totalWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { it.workMinutes ?: 0 }

    fun totalNightWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        ScheduleLogic.nightWorkMinutes(entry.startMinute, entry.endMinute, entry.breakMinutes)
    }

    fun totalWeekendWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        ScheduleLogic.weekendWorkMinutes(entry.date, entry.startMinute, entry.endMinute, entry.breakMinutes)
    }

    fun totalSaturdayWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        ScheduleLogic.saturdayWorkMinutes(entry.date, entry.startMinute, entry.endMinute, entry.breakMinutes)
    }

    fun totalSundayWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        ScheduleLogic.sundayWorkMinutes(entry.date, entry.startMinute, entry.endMinute, entry.breakMinutes)
    }

    fun totalHolidayWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        ScheduleLogic.holidayWorkMinutes(entry.date, entry.startMinute, entry.endMinute, entry.breakMinutes)
    }

    fun totalConfirmedOvertimeMinutes(items: Collection<ShiftEntry>): Int =
        items.sumOf { it.overtimeMinutes.coerceAtLeast(0) }

    fun totalOvertimeMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        val minutes = entry.workMinutes ?: return@sumOf 0
        ScheduleLogic.overtimeMinutes(minutes, standardDailyMinutes.value)
    }

    fun undoLastChange(): Int {
        val state = undoState ?: return 0
        state.entries.forEach { (date, previous) ->
            if (previous == null) entries.remove(date) else entries[date] = previous
        }
        persistEntries()
        val restored = state.entries.size
        undoState = null
        canUndo.value = false
        undoLabel.value = ""
        return restored
    }

    fun clearAll() {
        if (entries.isEmpty()) return
        val before = captureUndo(entries.keys.take(MAX_UNDO_DAYS), "Brisanje svih unosa")
        val fullyCaptured = entries.size <= MAX_UNDO_DAYS
        entries.clear()
        persistEntries()
        if (fullyCaptured) commitUndo(before) else clearUndoState()
    }

    fun applyPattern(
        startDate: LocalDate,
        codes: List<String?>,
        numberOfDays: Int,
        overwriteExisting: Boolean
    ): PatternApplyResult {
        if (codes.isEmpty() || numberOfDays <= 0) return PatternApplyResult(0, 0, 0)

        var changed = 0
        var freeDays = 0
        var skipped = 0
        val safeDays = numberOfDays.coerceAtMost(MAX_PATTERN_DAYS)
        val affectedDates = List(safeDays) { index -> startDate.plusDays(index.toLong()) }
        val before = captureUndo(affectedDates, "Primjena uzorka")

        repeat(safeDays) { index ->
            val date = startDate.plusDays(index.toLong())
            val code = codes[index % codes.size]
            val alreadyExists = entries.containsKey(date)
            if (alreadyExists && !overwriteExisting) {
                skipped++
                return@repeat
            }

            if (code.isNullOrBlank()) {
                if (entries.remove(date) != null) changed++
                freeDays++
            } else {
                val preset = shiftType(code)
                val next = if (preset != null) {
                    entryFromType(date, preset, note = "", preserveExistingTime = false)
                } else {
                    val cleanCode = sanitizeCustomText(code)
                    ShiftEntry(
                        date = date,
                        code = cleanCode,
                        label = cleanCode,
                        colorArgb = DEFAULT_CUSTOM_COLOR,
                        note = ""
                    )
                }
                if (entries[date] != next) {
                    entries[date] = next
                    changed++
                }
            }
        }
        if (changed > 0) {
            persistEntries()
            commitUndo(before)
        }
        return PatternApplyResult(changed, skipped, freeDays)
    }

    fun exportICalendar(): String = ICalendarExporter.export(entries.values)

    fun exportCsv(): String {
        val sb = StringBuilder("datum,sifra,naziv,napomena,boja,pocetak,kraj,pauza_min,prekovremeno_min\n")
        entries.values.sortedBy { it.date }.forEach { item ->
            sb.append(csv(item.date.toString())).append(',')
                .append(csv(item.code)).append(',')
                .append(csv(item.label)).append(',')
                .append(csv(item.note)).append(',')
                .append(csv(formatColorArgb(item.colorArgb))).append(',')
                .append(csv(item.startMinute?.let(ScheduleLogic::formatClock).orEmpty())).append(',')
                .append(csv(item.endMinute?.let(ScheduleLogic::formatClock).orEmpty())).append(',')
                .append(csv(item.breakMinutes.takeIf { item.hasWorkTime }?.toString().orEmpty())).append(',')
                .append(csv(item.overtimeMinutes.takeIf { item.hasWorkTime && it > 0 }?.toString().orEmpty())).append('\n')
        }
        return sb.toString()
    }

    fun importCsv(content: String, overwriteExisting: Boolean = true): ImportResult {
        if (content.length > MAX_IMPORT_CHARS || content.count { it == '"' } % 2 != 0) {
            return ImportResult(0, 0, 0, valid = false)
        }
        var imported = 0
        var skipped = 0
        var freeDays = 0
        val normalized = content.removePrefix("\uFEFF")
        val firstLine = normalized.lineSequence().firstOrNull { it.isNotBlank() } ?: return ImportResult(0, 0, 0)
        val delimiter = detectDelimiter(firstLine)
        fun meaningfulRows(): Sequence<List<String>> =
            parseCsv(normalized, delimiter).filter { row -> row.any { it.isNotBlank() } }

        val rowCount = meaningfulRows().take(MAX_IMPORT_ROWS + 1).count()
        if (rowCount > MAX_IMPORT_ROWS) return ImportResult(0, 0, 0, valid = false)
        if (rowCount == 0) return ImportResult(0, 0, 0)

        meaningfulRows().forEachIndexed { index, parts ->
            if (index == 0 && isHeader(parts)) return@forEachIndexed
            if (parts.size < 2) {
                skipped++
                return@forEachIndexed
            }

            val date = parseDate(parts[0])
            if (date == null) {
                skipped++
                return@forEachIndexed
            }
            if (!overwriteExisting && entries.containsKey(date)) {
                skipped++
                return@forEachIndexed
            }

            val code = parts[1].trim()
            if (code.isBlank()) {
                entries.remove(date)
                imported++
                freeDays++
                return@forEachIndexed
            }

            val preset = shiftType(code)
            val note = parts.getOrNull(3).orEmpty().trim().take(MAX_NOTE_LENGTH)
            val startMinute = parts.getOrNull(5)?.takeIf { it.isNotBlank() }?.let(ScheduleLogic::parseClock)
            val endMinute = parts.getOrNull(6)?.takeIf { it.isNotBlank() }?.let(ScheduleLogic::parseClock)
            val breakMinutes = parts.getOrNull(7)?.trim()?.toIntOrNull()?.coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES) ?: 0
            val workDuration = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)
            val hasValidTime = startMinute != null && endMinute != null &&
                workDuration?.let { it > 0 } == true
            val overtimeMinutes = parts.getOrNull(8)?.trim()?.toIntOrNull()?.coerceAtLeast(0)
                ?.takeIf { hasValidTime && it <= workDuration }
                ?: 0
            if (preset != null) {
                entries[date] = ShiftEntry(
                    date = date,
                    code = preset.code,
                    label = preset.name,
                    colorArgb = preset.color.toArgb().toLong() and 0xFFFFFFFFL,
                    note = note,
                    startMinute = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) startMinute else null,
                    endMinute = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) endMinute else null,
                    breakMinutes = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) breakMinutes else 0,
                    overtimeMinutes = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) overtimeMinutes else 0
                )
            } else {
                val cleanCode = sanitizeCustomText(code)
                if (cleanCode.isBlank()) {
                    skipped++
                    return@forEachIndexed
                }
                val label = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }?.take(MAX_CUSTOM_LENGTH) ?: cleanCode
                val color = parseColorArgb(parts.getOrNull(4)) ?: importedCodeColor(cleanCode)
                if (
                    customShiftPresets.keys.none { it.equals(cleanCode, ignoreCase = true) } &&
                    DefaultShiftTypes.presets.none { it.code.equals(cleanCode, ignoreCase = true) } &&
                    customShiftPresets.size < MAX_CUSTOM_PRESETS
                ) {
                    customShiftPresets[cleanCode] = CustomShiftPreset(cleanCode, label, color)
                }
                entries[date] = ShiftEntry(
                    date, cleanCode, label, color, note,
                    startMinute = if (hasValidTime) startMinute else null,
                    endMinute = if (hasValidTime) endMinute else null,
                    breakMinutes = if (hasValidTime) breakMinutes else 0,
                    overtimeMinutes = if (hasValidTime) overtimeMinutes else 0
                )
            }
            imported++
        }
        persistCustomShiftPresets()
        persistEntries()
        clearUndoState()
        return ImportResult(imported, skipped, freeDays)
    }

    fun exportBackupJson(): String = JSONObject().apply {
        put("schema", DATA_SCHEMA_VERSION)
        put("exportedAt", java.time.Instant.now().toString())
        put("entries", entriesToJson())
        put("profile", JSONObject().apply {
            put("fullName", userProfile.value.fullName)
            put("sector", userProfile.value.sector)
            put("industry", userProfile.value.industry)
            put("institutionType", userProfile.value.institutionType)
            put("organizationName", userProfile.value.organizationName)
            put("position", userProfile.value.position)
        })
        put("payroll", payrollProfileToJson(payrollProfile.value))
        put("settings", JSONObject().apply {
            put("remindersEnabled", remindersEnabled.value)
            put("reminderHour", reminderHour.value)
            put("reminderMinute", reminderMinute.value)
            put("shiftRemindersEnabled", shiftRemindersEnabled.value)
            put("shiftReminderLeadMinutes", shiftReminderLeadMinutes.value)
            put("standardDailyMinutes", standardDailyMinutes.value)
            put("monthlyTargetOverrides", JSONObject().apply {
                monthlyTargetOverrides.forEach { (month, minutes) -> put(month, minutes) }
            })
            put("workTimePresets", workTimePresetsToJson())
            put("shiftColors", JSONObject().apply {
                shiftColors.forEach { (code, argb) -> put(code, argb) }
            })
            put("customShiftPresets", customShiftPresetsToJson())
            put("savedPatterns", savedPatternsToJson())
        })
    }.toString(2)

    fun importBackupJson(content: String, replaceExisting: Boolean = false): ImportResult {
        if (content.length > MAX_IMPORT_CHARS) return ImportResult(0, 0, 0, valid = false)
        val root = runCatching { JSONObject(content) }.getOrElse { return ImportResult(0, 0, 0, valid = false) }
        val schema = root.optInt("schema", 1)
        if (schema !in 1..DATA_SCHEMA_VERSION) return ImportResult(0, 0, 0, valid = false)
        val array = root.optJSONArray("entries") ?: return ImportResult(0, 0, 0, valid = false)
        if (array.length() > MAX_IMPORT_ROWS) return ImportResult(0, 0, 0, valid = false)
        val parsed = mutableListOf<ShiftEntry>()
        repeat(array.length()) { index ->
            parseEntry(array.optJSONObject(index))?.let(parsed::add)
        }
        var imported = 0
        var skipped = 0
        parsed.forEach { item ->
            if (!replaceExisting && entries.containsKey(item.date)) {
                skipped++
            } else {
                entries[item.date] = item
                imported++
            }
        }
        root.optJSONObject("profile")?.let { profile ->
            saveUserProfile(
                UserProfile(
                    fullName = profile.optString("fullName", userProfile.value.fullName),
                    sector = profile.optString("sector", userProfile.value.sector),
                    industry = profile.optString("industry", userProfile.value.industry),
                    institutionType = profile.optString("institutionType", userProfile.value.institutionType),
                    organizationName = profile.optString("organizationName", userProfile.value.organizationName),
                    position = profile.optString("position", userProfile.value.position)
                )
            )
        }
        root.optJSONObject("payroll")?.let { payroll ->
            savePayrollProfile(parsePayrollProfile(payroll))
        }

        root.optJSONObject("settings")?.let { settings ->
            remindersEnabled.value = settings.optBoolean("remindersEnabled", remindersEnabled.value)
            reminderHour.value = settings.optInt("reminderHour", reminderHour.value).coerceIn(0, 23)
            reminderMinute.value = settings.optInt("reminderMinute", reminderMinute.value).coerceIn(0, 59)
            shiftRemindersEnabled.value = settings.optBoolean("shiftRemindersEnabled", shiftRemindersEnabled.value)
            shiftReminderLeadMinutes.value = settings.optInt("shiftReminderLeadMinutes", shiftReminderLeadMinutes.value)
                .coerceIn(0, MAX_SHIFT_REMINDER_LEAD_MINUTES)
            standardDailyMinutes.value = settings.optInt("standardDailyMinutes", standardDailyMinutes.value)
                .coerceIn(MIN_STANDARD_DAILY_MINUTES, MAX_STANDARD_DAILY_MINUTES)
            settings.optJSONObject("monthlyTargetOverrides")?.let { targets ->
                targets.keys().forEach { key ->
                    val month = runCatching { YearMonth.parse(key) }.getOrNull()
                    if (month != null) {
                        monthlyTargetOverrides[month.toString()] = targets.optInt(key, 0)
                            .coerceIn(0, ScheduleLogic.MAX_MONTHLY_TARGET_MINUTES)
                    }
                }
            }
            settings.optJSONObject("shiftColors")?.let { colors ->
                DefaultShiftTypes.presets.forEach { type ->
                    if (colors.has(type.code)) shiftColors[type.code] = colors.optLong(type.code, shiftColors.getValue(type.code))
                }
            }
            settings.optJSONArray("customShiftPresets")?.let { array ->
                repeat(array.length().coerceAtMost(MAX_CUSTOM_PRESETS)) { index ->
                    parseCustomShiftPreset(array.optJSONObject(index))?.let { preset ->
                        if (
                            DefaultShiftTypes.presets.none { it.code.equals(preset.code, ignoreCase = true) } &&
                            (customShiftPresets.containsKey(preset.code) || customShiftPresets.size < MAX_CUSTOM_PRESETS)
                        ) {
                            customShiftPresets[preset.code] = preset
                        }
                    }
                }
            }
            settings.optJSONArray("workTimePresets")?.let { array ->
                repeat(array.length().coerceAtMost(MAX_WORK_TIME_PRESETS)) { index ->
                    parseWorkTimePreset(array.optJSONObject(index))?.let { preset ->
                        if (shiftType(preset.code) != null && !ScheduleLogic.isLeaveCode(preset.code)) {
                            workTimePresets[preset.code] = preset
                        }
                    }
                }
            }
            settings.optJSONArray("savedPatterns")?.let { array ->
                repeat(array.length().coerceAtMost(MAX_SAVED_PATTERNS)) { index ->
                    parseSavedPattern(array.optJSONObject(index))?.let { pattern ->
                        if (savedPatterns.none { it.id == pattern.id } && savedPatterns.size < MAX_SAVED_PATTERNS) savedPatterns += pattern
                    }
                }
            }
            prefs.edit()
                .putBoolean(KEY_REMINDERS, remindersEnabled.value)
                .putInt(KEY_REMINDER_HOUR, reminderHour.value)
                .putInt(KEY_REMINDER_MINUTE, reminderMinute.value)
                .putBoolean(KEY_SHIFT_REMINDERS, shiftRemindersEnabled.value)
                .putInt(KEY_SHIFT_REMINDER_LEAD_MINUTES, shiftReminderLeadMinutes.value)
                .putInt(KEY_STANDARD_DAILY_MINUTES, standardDailyMinutes.value)
                .apply()
            persistShiftColors()
            persistCustomShiftPresets()
            persistWorkTimePresets()
            persistMonthlyTargetOverrides()
            persistSavedPatterns()
        }
        // Spremljene preset oznake uvijek koriste aktualni naziv i boju.
        // Time stari ili ručno izmijenjeni backup ne može ostaviti D/N/GO/BO/PD
        // s pogrešnim nazivom ili zastarjelom bojom.
        entries.entries.toList().forEach { (date, entry) ->
            shiftType(entry.code)?.let { type ->
                entries[date] = entry.copy(
                    code = type.code,
                    label = type.name,
                    colorArgb = type.color.toArgb().toLong() and 0xFFFFFFFFL
                )
            }
        }
        persistEntries()
        clearUndoState()
        return ImportResult(imported, skipped, 0)
    }

    private fun entryFromType(
        date: LocalDate,
        type: ShiftType,
        note: String,
        preserveExistingTime: Boolean
    ): ShiftEntry {
        val current = entries[date]
        val isLeave = ScheduleLogic.isLeaveCode(type.code)
        val keepCurrentTime = preserveExistingTime && current?.hasWorkTime == true && !isLeave
        val defaultTime = if (!isLeave) workTimePreset(type.code) else null
        return ShiftEntry(
            date = date,
            code = type.code,
            label = type.name,
            colorArgb = type.color.toArgb().toLong() and 0xFFFFFFFFL,
            note = note.trim().take(MAX_NOTE_LENGTH),
            startMinute = when {
                isLeave -> null
                keepCurrentTime -> current.startMinute
                else -> defaultTime?.startMinute
            },
            endMinute = when {
                isLeave -> null
                keepCurrentTime -> current.endMinute
                else -> defaultTime?.endMinute
            },
            breakMinutes = when {
                isLeave -> 0
                keepCurrentTime -> current.breakMinutes
                else -> defaultTime?.breakMinutes ?: 0
            },
            overtimeMinutes = when {
                isLeave -> 0
                keepCurrentTime -> current.overtimeMinutes
                else -> 0
            }
        )
    }

    private fun loadShiftColors() {
        DefaultShiftTypes.presets.forEach { type ->
            shiftColors[type.code] = type.color.toArgb().toLong() and 0xFFFFFFFFL
        }
        val raw = prefs.getString(KEY_SHIFT_COLORS, null) ?: return
        runCatching {
            val obj = JSONObject(raw)
            DefaultShiftTypes.presets.forEach { type ->
                if (obj.has(type.code)) shiftColors[type.code] = obj.optLong(type.code, shiftColors.getValue(type.code))
            }
        }
    }

    private fun persistShiftColors() {
        val obj = JSONObject()
        shiftColors.forEach { (code, argb) -> obj.put(code, argb) }
        prefs.edit().putString(KEY_SHIFT_COLORS, obj.toString()).apply()
    }

    private fun importedCodeColor(code: String): Long {
        val palette = longArrayOf(
            0xFF22B8CFL, 0xFF2488FFL, 0xFF8B46F6L, 0xFF13D7A0L,
            0xFFFFB21DL, 0xFFFF4B55L, 0xFF64748BL, 0xFF0EA5E9L
        )
        val index = (code.uppercase(Locale.ROOT).hashCode() and Int.MAX_VALUE) % palette.size
        return palette[index]
    }

    private fun loadCustomShiftPresets() {
        val raw = prefs.getString(KEY_CUSTOM_SHIFT_PRESETS, null) ?: return
        var cleanedLegacyCollisions = false
        runCatching {
            val array = JSONArray(raw)
            repeat(array.length().coerceAtMost(MAX_CUSTOM_PRESETS)) { index ->
                parseCustomShiftPreset(array.optJSONObject(index))?.let { preset ->
                    if (DefaultShiftTypes.presets.none { it.code.equals(preset.code, ignoreCase = true) }) {
                        customShiftPresets[preset.code] = preset
                    } else {
                        cleanedLegacyCollisions = true
                    }
                }
            }
        }.onSuccess {
            if (cleanedLegacyCollisions) persistCustomShiftPresets()
        }.onFailure {
            customShiftPresets.clear()
        }
    }

    private fun persistCustomShiftPresets() {
        prefs.edit().putString(KEY_CUSTOM_SHIFT_PRESETS, customShiftPresetsToJson().toString()).apply()
    }

    private fun customShiftPresetsToJson(): JSONArray = JSONArray().apply {
        customShiftPresets.values.sortedBy { it.code }.forEach { preset ->
            put(JSONObject().apply {
                put("code", preset.code)
                put("name", preset.name)
                put("color", preset.colorArgb)
            })
        }
    }

    private fun parseCustomShiftPreset(obj: JSONObject?): CustomShiftPreset? {
        if (obj == null) return null
        val code = ScheduleLogic.normalizeReusableCode(obj.optString("code", ""))
        if (code.isBlank()) return null
        val name = ScheduleLogic.normalizeDisplayName(obj.optString("name", code)).ifBlank { code }
        val color = obj.optLong("color", DEFAULT_CUSTOM_COLOR) and 0xFFFFFFFFL
        return CustomShiftPreset(code, name, color)
    }

    private fun loadWorkTimePresets() {
        val raw = prefs.getString(KEY_WORK_TIME_PRESETS, null) ?: return
        var cleanedInvalidPresets = false
        runCatching {
            val array = JSONArray(raw)
            repeat(array.length().coerceAtMost(MAX_WORK_TIME_PRESETS)) { index ->
                val parsed = parseWorkTimePreset(array.optJSONObject(index))
                if (
                    parsed != null &&
                    shiftType(parsed.code) != null &&
                    !ScheduleLogic.isLeaveCode(parsed.code)
                ) {
                    workTimePresets[parsed.code] = parsed
                } else {
                    cleanedInvalidPresets = true
                }
            }
        }.onSuccess {
            if (cleanedInvalidPresets) persistWorkTimePresets()
        }.onFailure {
            workTimePresets.clear()
        }
    }

    private fun seedDefaultWorkTimePresets() {
        if (prefs.getBoolean(KEY_DEFAULT_WORK_TIMES_SEEDED, false)) return

        var changed = false
        DefaultWorkTimePresets.presets.forEach { preset ->
            if (workTimePresets.keys.none { it.equals(preset.code, ignoreCase = true) }) {
                workTimePresets[preset.code] = preset
                changed = true
            }
        }
        if (changed) persistWorkTimePresets()
        prefs.edit().putBoolean(KEY_DEFAULT_WORK_TIMES_SEEDED, true).apply()
    }

    private fun persistWorkTimePresets() {
        prefs.edit().putString(KEY_WORK_TIME_PRESETS, workTimePresetsToJson().toString()).apply()
    }

    private fun workTimePresetsToJson(): JSONArray = JSONArray().apply {
        workTimePresets.values.sortedBy { it.code }.forEach { preset ->
            put(JSONObject().apply {
                put("code", preset.code)
                put("startMinute", preset.startMinute)
                put("endMinute", preset.endMinute)
                put("breakMinutes", preset.breakMinutes)
            })
        }
    }

    private fun parseWorkTimePreset(obj: JSONObject?): WorkTimePreset? {
        if (obj == null) return null
        val code = ScheduleLogic.normalizeReusableCode(obj.optString("code", ""))
        if (code.isBlank()) return null
        val start = obj.optInt("startMinute", -1).takeIf { it in 0 until ScheduleLogic.MINUTES_PER_DAY } ?: return null
        val end = obj.optInt("endMinute", -1).takeIf { it in 0 until ScheduleLogic.MINUTES_PER_DAY } ?: return null
        val breakMinutes = obj.optInt("breakMinutes", 0).coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES)
        val duration = ScheduleLogic.workDurationMinutes(start, end, breakMinutes) ?: return null
        if (duration <= 0) return null
        return WorkTimePreset(code, start, end, breakMinutes)
    }

    private fun loadMonthlyTargetOverrides() {
        val raw = prefs.getString(KEY_MONTHLY_TARGET_OVERRIDES, null) ?: return
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { key ->
                val month = runCatching { YearMonth.parse(key) }.getOrNull() ?: return@forEach
                monthlyTargetOverrides[month.toString()] = obj.optInt(key, 0)
                    .coerceIn(0, ScheduleLogic.MAX_MONTHLY_TARGET_MINUTES)
            }
        }.onFailure { monthlyTargetOverrides.clear() }
    }

    private fun persistMonthlyTargetOverrides() {
        val obj = JSONObject()
        monthlyTargetOverrides.toSortedMap().forEach { (month, minutes) -> obj.put(month, minutes) }
        prefs.edit().putString(KEY_MONTHLY_TARGET_OVERRIDES, obj.toString()).apply()
    }

    private fun loadSavedPatterns() {
        val raw = prefs.getString(KEY_SAVED_PATTERNS, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            repeat(array.length().coerceAtMost(MAX_SAVED_PATTERNS)) { index ->
                parseSavedPattern(array.optJSONObject(index))?.let(savedPatterns::add)
            }
        }.onFailure { savedPatterns.clear() }
    }

    private fun persistSavedPatterns() {
        prefs.edit().putString(KEY_SAVED_PATTERNS, savedPatternsToJson().toString()).apply()
    }

    private fun savedPatternsToJson(): JSONArray = JSONArray().apply {
        savedPatterns.forEach { pattern ->
            put(JSONObject().apply {
                put("id", pattern.id)
                put("name", pattern.name)
                put("codes", JSONArray().apply { pattern.codes.forEach { code -> put(code ?: JSONObject.NULL) } })
            })
        }
    }

    private fun parseSavedPattern(obj: JSONObject?): SavedPattern? {
        if (obj == null) return null
        val id = obj.optString("id", "").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val name = ScheduleLogic.normalizePatternName(obj.optString("name", ""))
        val array = obj.optJSONArray("codes") ?: return null
        val codes = buildList<String?> {
            repeat(array.length().coerceAtMost(ScheduleLogic.MAX_PATTERN_STEPS)) { index ->
                if (array.isNull(index)) add(null)
                else add(ScheduleLogic.normalizeReusableCode(array.optString(index)).takeIf { it.isNotBlank() })
            }
        }
        if (name.isBlank() || codes.isEmpty() || codes.all { it == null }) return null
        return SavedPattern(id, name, codes)
    }

    private fun loadEntries() {
        val emergencySnapshot = if (prefs.getBoolean(KEY_ENTRIES_EMERGENCY, false)) {
            readLegacyPreferenceEntries()
        } else {
            null
        }
        val currentSnapshot = readSnapshotFile(CURRENT_SCHEDULE_FILE)
        val recoverySnapshot = readSnapshotFile(RECOVERY_SCHEDULE_FILE)
        val newestFileSnapshot = listOfNotNull(currentSnapshot, recoverySnapshot)
            .maxByOrNull { it.archiveRevisionCount }
        val legacySnapshot = if (emergencySnapshot == null && newestFileSnapshot == null) {
            readLegacyPreferenceEntries()
        } else {
            null
        }

        val base = when {
            emergencySnapshot != null -> StoredScheduleSnapshot(emergencySnapshot, archiveRevisionCount.value)
            newestFileSnapshot != null -> newestFileSnapshot
            legacySnapshot != null -> StoredScheduleSnapshot(legacySnapshot, 0)
            else -> StoredScheduleSnapshot(emptyMap(), 0)
        }
        val recovered = base.entries.toMutableMap()

        // Primjenjuju se samo revizije novije od snimke. Ako je proces bio
        // prekinut nakon zapisa revizije, ali prije završetka atomske snimke,
        // zadnja promjena se automatski vrati bez ponovnog čitanja cijele
        // višegodišnje arhive pri svakom pokretanju.
        applyArchiveRevisions(
            target = recovered,
            skipRevisions = base.archiveRevisionCount.coerceIn(0, archiveRevisionCount.value),
            resumeByteOffset = base.archiveByteOffset
        )

        entries.clear()
        entries.putAll(recovered.toSortedMap())

        // Svako uspješno učitavanje konsolidira stanje u dvije datotečne
        // snimke. Legacy SharedPreferences ključ briše se tek kada su obje
        // snimke sigurno zapisane.
        val currentWritten = writeSnapshotFile(CURRENT_SCHEDULE_FILE, entries.values)
        val recoveryWritten = writeSnapshotFile(RECOVERY_SCHEDULE_FILE, entries.values)
        if (currentWritten) {
            lastCheckpointRevisionCount = archiveRevisionCount.value
        } else {
            lastCheckpointRevisionCount = base.archiveRevisionCount
        }
        if (currentWritten && recoveryWritten) {
            prefs.edit()
                .remove(KEY_ENTRIES)
                .remove(KEY_ENTRIES_EMERGENCY)
                .apply()
        }
    }

    private fun readLegacyPreferenceEntries(): Map<LocalDate, ShiftEntry>? {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                repeat(array.length()) { index ->
                    parseEntry(array.optJSONObject(index))?.let { put(it.date, it) }
                }
            }
        }.getOrNull()
    }

    private fun readSnapshotFile(fileName: String): StoredScheduleSnapshot? {
        val file = context.getFileStreamPath(fileName)
        if (!file.exists() || file.length() > MAX_SNAPSHOT_BYTES) return null

        return runCatching {
            val atomic = AtomicFile(file)
            val raw = atomic.openRead()
                .bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }
            val root = JSONObject(raw)
            val schema = root.optInt("schema", -1)
            if (schema !in 1..CURRENT_SNAPSHOT_SCHEMA_VERSION) return@runCatching null

            val array = root.optJSONArray("entries") ?: return@runCatching null
            val declaredCount = root.optInt("entryCount", -1)
            if (declaredCount !in 0..MAX_IMPORT_ROWS || declaredCount != array.length()) {
                return@runCatching null
            }

            val loadedEntries = buildMap {
                repeat(array.length()) { index ->
                    parseEntry(array.optJSONObject(index))?.let { put(it.date, it) }
                }
            }
            if (loadedEntries.size != declaredCount) return@runCatching null

            StoredScheduleSnapshot(
                entries = loadedEntries,
                archiveRevisionCount = root.optInt("archiveRevisionCount", 0).coerceAtLeast(0),
                archiveByteOffset = if (schema >= 2 && root.has("archiveByteOffset")) {
                    root.optLong("archiveByteOffset", -1L).takeIf { it >= 0L }
                } else {
                    null
                }
            )
        }.getOrNull()
    }

    private fun writeSnapshotFile(
        fileName: String,
        source: Collection<ShiftEntry>,
        checkpointRevisionCount: Int = archiveRevisionCount.value
    ): Boolean {
        val file = context.getFileStreamPath(fileName)
        val atomic = AtomicFile(file)
        val payload = JSONObject().apply {
            put("schema", CURRENT_SNAPSHOT_SCHEMA_VERSION)
            put("savedAt", java.time.Instant.now().toString())
            put("entryCount", source.size)
            put("archiveRevisionCount", checkpointRevisionCount.coerceAtLeast(0))
            put("archiveByteOffset", context.getFileStreamPath(HISTORY_FILE).let { if (it.exists()) it.length() else 0L })
            put("entries", JSONArray().apply {
                source.sortedBy { it.date }.forEach { put(entryToJsonObject(it)) }
            })
        }.toString().toByteArray(StandardCharsets.UTF_8)

        if (payload.size > MAX_SNAPSHOT_BYTES) return false

        val output = runCatching { atomic.startWrite() }.getOrNull() ?: return false
        return runCatching {
            output.write(payload)
            output.flush()
            atomic.finishWrite(output)
            true
        }.getOrElse {
            runCatching { atomic.failWrite(output) }
            false
        }
    }

    private fun copyCurrentSnapshotToRecovery() {
        val currentFile = context.getFileStreamPath(CURRENT_SCHEDULE_FILE)
        if (!currentFile.exists() || currentFile.length() > MAX_SNAPSHOT_BYTES) return

        val currentAtomic = AtomicFile(currentFile)
        val recoveryAtomic = AtomicFile(context.getFileStreamPath(RECOVERY_SCHEDULE_FILE))
        val output = runCatching { recoveryAtomic.startWrite() }.getOrNull() ?: return

        runCatching {
            currentAtomic.openRead().buffered().use { input ->
                input.copyTo(output, bufferSize = SNAPSHOT_COPY_BUFFER_BYTES)
            }
            output.flush()
            recoveryAtomic.finishWrite(output)
        }.onFailure {
            runCatching { recoveryAtomic.failWrite(output) }
        }
    }

    private fun applyArchiveRevisions(
        target: MutableMap<LocalDate, ShiftEntry>,
        skipRevisions: Int,
        resumeByteOffset: Long?
    ) {
        val history = context.getFileStreamPath(HISTORY_FILE)
        if (!history.exists()) return

        fun applyLine(line: String) {
            if (line.isBlank()) return
            val revision = runCatching { JSONObject(line) }.getOrNull() ?: return
            val date = runCatching {
                LocalDate.parse(revision.optString("date"), DateTimeFormatter.ISO_LOCAL_DATE)
            }.getOrNull() ?: return

            val before = if (revision.isNull("before")) null else parseEntry(revision.optJSONObject("before"))
            val after = if (revision.isNull("after")) null else parseEntry(revision.optJSONObject("after"))
            val resolution = ScheduleRecovery.resolve(
                current = target[date],
                before = before,
                after = after
            )
            if (resolution.shouldApply) {
                if (resolution.next == null) target.remove(date)
                else target[date] = resolution.next
            }
        }

        val byteOffset = resumeByteOffset?.takeIf { offset ->
            val boundaryValid = offset == 0L || runCatching {
                RandomAccessFile(history, "r").use { file ->
                    file.seek(offset - 1L)
                    file.read() == '\n'.code
                }
            }.getOrDefault(false)
            SchedulePersistencePolicy.canResumeArchiveFromByteOffset(
                fileLength = history.length(),
                currentRevisionCount = archiveRevisionCount.value,
                checkpointRevisionCount = skipRevisions,
                checkpointByteOffset = offset,
                boundaryIsValid = boundaryValid
            )
        }

        runCatching {
            if (byteOffset != null) {
                FileInputStream(history).use { input ->
                    input.channel.position(byteOffset)
                    input.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                        lines.forEach(::applyLine)
                    }
                }
            } else {
                history.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                    lines.drop(skipRevisions).forEach(::applyLine)
                }
            }
        }
    }

    private fun parseEntry(obj: JSONObject?): ShiftEntry? {
        if (obj == null) return null
        val date = runCatching {
            LocalDate.parse(obj.getString("date"), DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull() ?: return null
        val code = sanitizeCustomText(obj.optString("code", ""))
        if (code.isBlank()) return null
        val start = obj.optInt("startMinute", -1).takeIf { it in 0 until ScheduleLogic.MINUTES_PER_DAY }
        val end = obj.optInt("endMinute", -1).takeIf { it in 0 until ScheduleLogic.MINUTES_PER_DAY }
        val breakMinutes = obj.optInt("breakMinutes", 0).coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES)
        val duration = ScheduleLogic.workDurationMinutes(start, end, breakMinutes)
        val validTime = start != null && end != null &&
            duration?.let { it > 0 } == true &&
            !ScheduleLogic.isLeaveCode(code)
        val overtimeMinutes = obj.optInt("overtimeMinutes", 0)
            .coerceAtLeast(0)
            .takeIf { validTime && it <= duration }
            ?: 0
        return ShiftEntry(
            date = date,
            code = code,
            label = obj.optString("label", code).take(MAX_CUSTOM_LENGTH),
            colorArgb = obj.optLong("color", DEFAULT_CUSTOM_COLOR),
            note = obj.optString("note", "").take(MAX_NOTE_LENGTH),
            startMinute = if (validTime) start else null,
            endMinute = if (validTime) end else null,
            breakMinutes = if (validTime) breakMinutes else 0,
            overtimeMinutes = overtimeMinutes
        )
    }

    private fun persistEntries() {
        val archive = appendArchiveDiff()
        if (archive.changedCount == 0) return

        val revisionsSinceCheckpoint =
            (archiveRevisionCount.value - lastCheckpointRevisionCount).coerceAtLeast(0)
        val emergencyActive = prefs.getBoolean(KEY_ENTRIES_EMERGENCY, false)
        val shouldCheckpoint = emergencyActive || !archive.appended || SchedulePersistencePolicy.shouldCheckpoint(
            currentSnapshotExists = context.getFileStreamPath(CURRENT_SCHEDULE_FILE).exists(),
            revisionsSinceCheckpoint = revisionsSinceCheckpoint,
            changedEntries = archive.changedCount
        )

        var durable = archive.appended
        if (shouldCheckpoint) {
            // Recovery zadržava prethodni potvrđeni checkpoint. Glavna snimka
            // zatim konsolidira journal i ponovno postaje najnoviji checkpoint.
            copyCurrentSnapshotToRecovery()
            val snapshotWritten = writeSnapshotFile(CURRENT_SCHEDULE_FILE, entries.values)
            if (snapshotWritten) {
                lastCheckpointRevisionCount = archiveRevisionCount.value
                prefs.edit()
                    .remove(KEY_ENTRIES)
                    .remove(KEY_ENTRIES_EMERGENCY)
                    .apply()
                durable = true
            } else if (emergencyActive || !archive.appended) {
                // Krajnji fallback ako je emergency stanje već aktivno ili ni
                // journal nije mogao biti trajno zapisan. Dok marker postoji,
                // kopija se osvježava pri svakoj promjeni kako nikad ne bi
                // zaostala za novijim journal revizijama.
                durable = prefs.edit()
                    .putString(KEY_ENTRIES, entriesToJson().toString())
                    .putBoolean(KEY_ENTRIES_EMERGENCY, true)
                    .commit()
            }
        }

        if (durable) {
            persistedSnapshot = entries.mapValues { (_, entry) -> entry.copy() }
        }
    }

    private fun appendArchiveDiff(): ArchiveAppendResult {
        val dates = (persistedSnapshot.keys + entries.keys).toSortedSet()
        val changed = dates.filter { date -> persistedSnapshot[date] != entries[date] }
        if (changed.isEmpty()) return ArchiveAppendResult(0, appended = true)

        val appended = runCatching {
            val output = context.openFileOutput(HISTORY_FILE, Context.MODE_APPEND)
            output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                changed.forEach { date ->
                    val before = persistedSnapshot[date]
                    val after = entries[date]
                    val revision = JSONObject().apply {
                        put("schema", ARCHIVE_SCHEMA_VERSION)
                        put("changedAt", java.time.Instant.now().toString())
                        put("date", date.toString())
                        put("before", before?.let(::entryToJsonObject) ?: JSONObject.NULL)
                        put("after", after?.let(::entryToJsonObject) ?: JSONObject.NULL)
                    }
                    writer.append(revision.toString()).append('\n')
                }
                writer.flush()
                output.fd.sync()
            }
            true
        }.getOrDefault(false)

        if (appended) {
            archiveRevisionCount.value += changed.size
            persistArchiveRevisionMetadata()
        }
        return ArchiveAppendResult(changed.size, appended)
    }

    private fun loadArchiveRevisionCount(): Int {
        val file = context.getFileStreamPath(HISTORY_FILE)
        if (!file.exists()) {
            prefs.edit()
                .putInt(KEY_ARCHIVE_REVISION_COUNT, 0)
                .putLong(KEY_ARCHIVE_FILE_LENGTH, 0L)
                .apply()
            return 0
        }

        val cachedCount = prefs.getInt(KEY_ARCHIVE_REVISION_COUNT, -1)
        val cachedLength = prefs.getLong(KEY_ARCHIVE_FILE_LENGTH, -1L)
        if (cachedCount >= 0 && cachedLength == file.length()) {
            return cachedCount
        }

        val counted = runCatching {
            file.bufferedReader(StandardCharsets.UTF_8)
                .useLines { lines -> lines.count().coerceAtMost(Int.MAX_VALUE) }
        }.getOrDefault(0)

        prefs.edit()
            .putInt(KEY_ARCHIVE_REVISION_COUNT, counted)
            .putLong(KEY_ARCHIVE_FILE_LENGTH, file.length())
            .apply()
        return counted
    }

    private fun persistArchiveRevisionMetadata() {
        val file = context.getFileStreamPath(HISTORY_FILE)
        prefs.edit()
            .putInt(KEY_ARCHIVE_REVISION_COUNT, archiveRevisionCount.value)
            .putLong(KEY_ARCHIVE_FILE_LENGTH, if (file.exists()) file.length() else 0L)
            .apply()
    }

    private fun loadUserProfile(): UserProfile {
        val raw = prefs.getString(KEY_USER_PROFILE, null) ?: return UserProfile()
        return runCatching {
            val obj = JSONObject(raw)
            UserProfile(
                fullName = obj.optString("fullName", ""),
                sector = obj.optString("sector", ""),
                industry = obj.optString("industry", ""),
                institutionType = obj.optString("institutionType", ""),
                organizationName = obj.optString("organizationName", ""),
                position = obj.optString("position", "")
            )
        }.getOrDefault(UserProfile())
    }

    private fun loadPayrollProfile(): PayrollProfile {
        val raw = prefs.getString(KEY_PAYROLL_PROFILE, null) ?: return PayrollProfile()
        return runCatching { parsePayrollProfile(JSONObject(raw)) }.getOrDefault(PayrollProfile())
    }

    private fun parsePayrollProfile(obj: JSONObject): PayrollProfile = PayrollProfile(
        enabled = obj.optBoolean("enabled", false),
        system = PayrollSystem.fromPersisted(obj.optString("system").takeIf { it.isNotBlank() }),
        rolePresetId = obj.optString("rolePresetId", "").trim().take(80),
        coefficient = obj.optDouble("coefficient", 0.0).coerceIn(0.0, 20.0),
        yearsOfService = obj.optInt("yearsOfService", 0).coerceIn(0, 70),
        manualBaseEur = obj.optDouble("manualBaseEur", 0.0).coerceIn(0.0, 20_000.0),
        taxLocalityPresetId = obj.optString("taxLocalityPresetId", "").trim().take(80),
        lowerTaxRatePercent = obj.optDouble("lowerTaxRatePercent", 0.0).coerceIn(0.0, 60.0),
        higherTaxRatePercent = obj.optDouble("higherTaxRatePercent", 0.0).coerceIn(0.0, 60.0),
        personalAllowanceEur = obj.optDouble("personalAllowanceEur", 600.0).coerceIn(0.0, 50_000.0),
        pensionMode = PensionMode.fromPersisted(obj.optString("pensionMode").takeIf { it.isNotBlank() }),
        additionalGrossEur = obj.optDouble("additionalGrossEur", 0.0).coerceIn(0.0, 100_000.0),
        nonTaxableEur = obj.optDouble("nonTaxableEur", 0.0).coerceIn(0.0, 100_000.0),
        overtimePercent = obj.optDouble("overtimePercent", 0.0).coerceIn(0.0, 300.0),
        nightPercent = obj.optDouble("nightPercent", 0.0).coerceIn(0.0, 300.0),
        saturdayPercent = obj.optDouble("saturdayPercent", 0.0).coerceIn(0.0, 300.0),
        sundayPercent = obj.optDouble("sundayPercent", 0.0).coerceIn(0.0, 300.0),
        holidayPercent = obj.optDouble("holidayPercent", 0.0).coerceIn(0.0, 300.0),
        otherEmployersGrossEur = obj.optDouble("otherEmployersGrossEur", 0.0).coerceIn(0.0, 100_000.0),
        allAdjustmentsConfirmed = obj.optBoolean("allAdjustmentsConfirmed", false)
    )

    private fun payrollProfileToJson(profile: PayrollProfile): JSONObject = JSONObject().apply {
        put("enabled", profile.enabled)
        put("system", profile.system.persistedValue)
        put("rolePresetId", profile.rolePresetId)
        put("coefficient", profile.coefficient)
        put("yearsOfService", profile.yearsOfService)
        put("manualBaseEur", profile.manualBaseEur)
        put("taxLocalityPresetId", profile.taxLocalityPresetId)
        put("lowerTaxRatePercent", profile.lowerTaxRatePercent)
        put("higherTaxRatePercent", profile.higherTaxRatePercent)
        put("personalAllowanceEur", profile.personalAllowanceEur)
        put("pensionMode", profile.pensionMode.persistedValue)
        put("additionalGrossEur", profile.additionalGrossEur)
        put("nonTaxableEur", profile.nonTaxableEur)
        put("overtimePercent", profile.overtimePercent)
        put("nightPercent", profile.nightPercent)
        put("saturdayPercent", profile.saturdayPercent)
        put("sundayPercent", profile.sundayPercent)
        put("holidayPercent", profile.holidayPercent)
        put("otherEmployersGrossEur", profile.otherEmployersGrossEur)
        put("allAdjustmentsConfirmed", profile.allAdjustmentsConfirmed)
    }

    private fun entryToJsonObject(item: ShiftEntry): JSONObject = JSONObject().apply {
        put("date", item.date.toString())
        put("code", item.code)
        put("label", item.label)
        put("color", item.colorArgb)
        put("note", item.note)
        if (item.startMinute != null) put("startMinute", item.startMinute)
        if (item.endMinute != null) put("endMinute", item.endMinute)
        put("breakMinutes", item.breakMinutes)
        put("overtimeMinutes", item.overtimeMinutes)
    }

    private fun entriesToJson(): JSONArray = JSONArray().apply {
        entries.values.sortedBy { it.date }.forEach { item -> put(entryToJsonObject(item)) }
    }

    private fun captureUndo(dates: Collection<LocalDate>, label: String): UndoState {
        val unique = dates.distinct().take(MAX_UNDO_DAYS)
        return UndoState(label, unique.associateWith { entries[it]?.copy() })
    }

    private fun commitUndo(state: UndoState) {
        if (state.entries.none { (date, previous) -> entries[date] != previous }) return
        undoState = state
        canUndo.value = true
        undoLabel.value = state.label
    }

    private fun clearUndoState() {
        undoState = null
        canUndo.value = false
        undoLabel.value = ""
    }

    private fun sanitizeCustomText(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").take(MAX_CUSTOM_LENGTH)

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun formatColorArgb(value: Long): String = "#%08X".format(Locale.ROOT, value and 0xFFFFFFFFL)

    private fun parseColorArgb(value: String?): Long? {
        val clean = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            when {
                clean.startsWith("#") && clean.length == 7 -> (0xFF000000L or clean.drop(1).toLong(16)) and 0xFFFFFFFFL
                clean.startsWith("#") && clean.length == 9 -> clean.drop(1).toLong(16) and 0xFFFFFFFFL
                else -> clean.toLong() and 0xFFFFFFFFL
            }
        }.getOrNull()
    }

    private fun detectDelimiter(firstLine: String): Char {
        val commas = firstLine.count { it == ',' }
        val semicolons = firstLine.count { it == ';' }
        return if (semicolons > commas) ';' else ','
    }

    private fun isHeader(parts: List<String>): Boolean {
        val first = parts.firstOrNull()?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return first in setOf("datum", "date", "dan")
    }

    private fun parseDate(value: String): LocalDate? {
        val clean = value.trim().trim('"')
        DATE_FORMATS.forEach { formatter ->
            try {
                return LocalDate.parse(clean, formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    private fun parseCsv(content: String, delimiter: Char): Sequence<List<String>> = sequence {
        val row = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0

        fun pushField() {
            row += current.toString().trim()
            current.clear()
        }

        while (i < content.length) {
            val c = content[i]
            when {
                c == '"' && quoted && i + 1 < content.length && content[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                c == delimiter && !quoted -> pushField()
                c == '\n' && !quoted -> {
                    pushField()
                    yield(row.toList())
                    row.clear()
                }
                c == '\r' && !quoted -> Unit
                else -> current.append(c)
            }
            i++
        }

        if (current.isNotEmpty() || row.isNotEmpty()) {
            pushField()
            yield(row.toList())
        }
    }

    private data class UndoState(val label: String, val entries: Map<LocalDate, ShiftEntry?>)
    private data class ArchiveAppendResult(val changedCount: Int, val appended: Boolean)

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val freeDays: Int,
        val valid: Boolean = true
    )
    data class PatternApplyResult(val changed: Int, val skipped: Int, val freeDays: Int)
    data class BulkEditResult(val changed: Int, val skipped: Int, val freeDays: Int)

    companion object {
        const val DEFAULT_CUSTOM_COLOR: Long = 0xFF22B8CFL
        const val MAX_CUSTOM_LENGTH = 24
        const val MAX_NOTE_LENGTH = 180
        const val DEFAULT_SHIFT_REMINDER_LEAD_MINUTES = 30
        const val MAX_SHIFT_REMINDER_LEAD_MINUTES = 7 * 24 * 60
        const val DEFAULT_STANDARD_DAILY_MINUTES = 8 * 60
        const val MIN_STANDARD_DAILY_MINUTES = 60
        const val MAX_STANDARD_DAILY_MINUTES = 24 * 60
        private const val MAX_PATTERN_DAYS = 366
        private const val MAX_BULK_DAYS = 366
        private const val MAX_CUSTOM_PRESETS = 20
        private const val MAX_WORK_TIME_PRESETS = 40
        private const val MAX_SAVED_PATTERNS = 20
        private const val MAX_IMPORT_ROWS = 100_000
        const val MAX_IMPORT_CHARS = 20_000_000
        private const val MAX_UNDO_DAYS = 1_000
        private const val MAX_PROFILE_TEXT = 120
        private const val SUGGESTION_LOOKBACK_DAYS = 90L
        private const val DATA_SCHEMA_VERSION = 11
        private const val ARCHIVE_SCHEMA_VERSION = 1
        private const val CURRENT_SNAPSHOT_SCHEMA_VERSION = 2
        private const val MAX_SNAPSHOT_BYTES = 64L * 1024L * 1024L
        private const val SNAPSHOT_COPY_BUFFER_BYTES = 64 * 1024
        private const val CURRENT_SCHEDULE_FILE = "takto_schedule_current.json"
        private const val RECOVERY_SCHEDULE_FILE = "takto_schedule_recovery.json"
        private const val HISTORY_FILE = "takto_schedule_history.jsonl"
        private const val PREFS_NAME = "takto_schedule"
        private const val KEY_ENTRIES = "entries_json"
        private const val KEY_ENTRIES_EMERGENCY = "entries_json_emergency"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_SHIFT_REMINDERS = "shift_reminders"
        private const val KEY_SHIFT_REMINDER_LEAD_MINUTES = "shift_reminder_lead_minutes"
        private const val KEY_STANDARD_DAILY_MINUTES = "standard_daily_minutes"
        private const val KEY_MONTHLY_TARGET_OVERRIDES = "monthly_target_overrides_json"
        private const val KEY_WORK_TIME_PRESETS = "work_time_presets_json"
        private const val KEY_DEFAULT_WORK_TIMES_SEEDED = "default_work_times_seeded_v1"
        private const val KEY_SHIFT_COLORS = "shift_colors_json"
        private const val KEY_CUSTOM_SHIFT_PRESETS = "custom_shift_presets_json"
        private const val KEY_SAVED_PATTERNS = "saved_patterns_json"
        private const val KEY_USER_PROFILE = "user_profile_json"
        private const val KEY_PAYROLL_PROFILE = "payroll_profile_json"
        private const val KEY_ARCHIVE_REVISION_COUNT = "archive_revision_count"
        private const val KEY_ARCHIVE_FILE_LENGTH = "archive_file_length"

        private val DATE_FORMATS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d.M.uuuu."),
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("d/M/uuuu")
        )
    }
}
