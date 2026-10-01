package hr.takto.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import hr.takto.app.model.AppThemeMode
import hr.takto.app.model.CustomShiftPreset
import hr.takto.app.model.ICalendarExporter
import hr.takto.app.model.DefaultShiftTypes
import hr.takto.app.model.SavedPattern
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.ShiftEntry
import hr.takto.app.model.ShiftType
import hr.takto.app.model.WorkTimePreset
import hr.takto.app.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

/**
 * Jedini izvor istine za Takto raspored i korisničke postavke.
 *
 * Namjerno ne koristi bazu podataka: jedan korisnik ima najviše nekoliko tisuća
 * dnevnih zapisa pa je verzionirani JSON u SharedPreferences dovoljno malen,
 * brz i jednostavan za sigurnosnu kopiju. Sve bulk operacije spremaju stanje
 * samo jednom kako bi uvoz i primjena uzoraka ostali brzi.
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
    val themeMode = mutableStateOf(AppThemeMode.fromPersisted(prefs.getString(KEY_THEME_MODE, null)))
    val archiveRevisionCount = mutableStateOf(0)
    private var persistedSnapshot: Map<LocalDate, ShiftEntry> = emptyMap()
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
        seedReferenceShortcutsOnce()
        loadWorkTimePresets()
        loadMonthlyTargetOverrides()
        loadSavedPatterns()
        loadEntries()
        persistedSnapshot = entries.mapValues { (_, entry) -> entry.copy() }
        archiveRevisionCount.value = countArchiveRevisions()
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

    fun suggestedShiftTypes(referenceDate: LocalDate = LocalDate.now()): List<ShiftType> {
        val all = allShiftTypes()
        if (all.size <= 1) return all

        val windowStart = referenceDate.minusDays(SUGGESTION_LOOKBACK_DAYS)
        val usage = entries.values
            .asSequence()
            .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(referenceDate) }
            .groupingBy { it.code.uppercase(Locale.ROOT) }
            .eachCount()

        return all.sortedWith(
            compareByDescending<ShiftType> { usage[it.code.uppercase(Locale.ROOT)] ?: 0 }
                .thenBy { if (it.isPreset) 1 else 0 }
                .thenBy { it.name.lowercase(Locale.forLanguageTag("hr")) }
        )
    }

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
        val duration = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes) ?: return false
        if (duration <= 0) return false
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
            breakMinutes = current?.breakMinutes ?: 0
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

    fun updateWorkTime(date: LocalDate, startMinute: Int, endMinute: Int, breakMinutes: Int): Boolean {
        val current = entries[date] ?: return false
        if (ScheduleLogic.isLeaveCode(current.code)) return false
        val duration = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes) ?: return false
        if (duration <= 0) return false
        val safeBreak = breakMinutes.coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES)
        val next = current.copy(
            startMinute = startMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
            endMinute = endMinute.coerceIn(0, ScheduleLogic.MINUTES_PER_DAY - 1),
            breakMinutes = safeBreak
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
        if (!current.hasWorkTime && current.breakMinutes == 0) return false
        val before = captureUndo(listOf(date), "Uklanjanje radnog vremena ${date}")
        entries[date] = current.copy(startMinute = null, endMinute = null, breakMinutes = 0)
        persistEntries()
        commitUndo(before)
        return true
    }

    fun updateWorkTime(
        dates: Collection<LocalDate>,
        startMinute: Int,
        endMinute: Int,
        breakMinutes: Int
    ): BulkEditResult {
        val duration = ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)
            ?: return BulkEditResult(0, 0, 0)
        if (duration <= 0) return BulkEditResult(0, 0, 0)
        val unique = dates.distinct().sorted().take(MAX_BULK_DAYS)
        val before = captureUndo(unique, "Radno vrijeme za ${unique.size} dana")
        val safeBreak = breakMinutes.coerceIn(0, ScheduleLogic.MAX_BREAK_MINUTES)
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
                breakMinutes = safeBreak
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
                breakMinutes = current?.breakMinutes ?: 0
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

    fun setThemeMode(mode: AppThemeMode) {
        themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.persistedValue).apply()
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

    fun exportArchiveJsonLines(): String =
        runCatching { context.getFileStreamPath(HISTORY_FILE).takeIf { it.exists() }?.readText().orEmpty() }
            .getOrDefault("")

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

    fun totalSundayWorkMinutes(items: Collection<ShiftEntry>): Int = items.sumOf { entry ->
        ScheduleLogic.sundayWorkMinutes(entry.date, entry.startMinute, entry.endMinute, entry.breakMinutes)
    }

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
        val sb = StringBuilder("datum,sifra,naziv,napomena,boja,pocetak,kraj,pauza_min\n")
        entries.values.sortedBy { it.date }.forEach { item ->
            sb.append(csv(item.date.toString())).append(',')
                .append(csv(item.code)).append(',')
                .append(csv(item.label)).append(',')
                .append(csv(item.note)).append(',')
                .append(csv(formatColorArgb(item.colorArgb))).append(',')
                .append(csv(item.startMinute?.let(ScheduleLogic::formatClock).orEmpty())).append(',')
                .append(csv(item.endMinute?.let(ScheduleLogic::formatClock).orEmpty())).append(',')
                .append(csv(item.breakMinutes.takeIf { item.hasWorkTime }?.toString().orEmpty())).append('\n')
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
            val hasValidTime = startMinute != null && endMinute != null &&
                ScheduleLogic.workDurationMinutes(startMinute, endMinute, breakMinutes)?.let { it > 0 } == true
            if (preset != null) {
                entries[date] = ShiftEntry(
                    date = date,
                    code = preset.code,
                    label = preset.name,
                    colorArgb = preset.color.toArgb().toLong() and 0xFFFFFFFFL,
                    note = note,
                    startMinute = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) startMinute else null,
                    endMinute = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) endMinute else null,
                    breakMinutes = if (hasValidTime && !ScheduleLogic.isLeaveCode(preset.code)) breakMinutes else 0
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
                    breakMinutes = if (hasValidTime) breakMinutes else 0
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
        put("settings", JSONObject().apply {
            put("remindersEnabled", remindersEnabled.value)
            put("reminderHour", reminderHour.value)
            put("reminderMinute", reminderMinute.value)
            put("shiftRemindersEnabled", shiftRemindersEnabled.value)
            put("shiftReminderLeadMinutes", shiftReminderLeadMinutes.value)
            put("standardDailyMinutes", standardDailyMinutes.value)
            put("themeMode", themeMode.value.persistedValue)
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

        root.optJSONObject("settings")?.let { settings ->
            remindersEnabled.value = settings.optBoolean("remindersEnabled", remindersEnabled.value)
            reminderHour.value = settings.optInt("reminderHour", reminderHour.value).coerceIn(0, 23)
            reminderMinute.value = settings.optInt("reminderMinute", reminderMinute.value).coerceIn(0, 59)
            shiftRemindersEnabled.value = settings.optBoolean("shiftRemindersEnabled", shiftRemindersEnabled.value)
            shiftReminderLeadMinutes.value = settings.optInt("shiftReminderLeadMinutes", shiftReminderLeadMinutes.value)
                .coerceIn(0, MAX_SHIFT_REMINDER_LEAD_MINUTES)
            standardDailyMinutes.value = settings.optInt("standardDailyMinutes", standardDailyMinutes.value)
                .coerceIn(MIN_STANDARD_DAILY_MINUTES, MAX_STANDARD_DAILY_MINUTES)
            themeMode.value = AppThemeMode.fromPersisted(
                settings.optString("themeMode", themeMode.value.persistedValue)
            )
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
                .putString(KEY_THEME_MODE, themeMode.value.persistedValue)
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
                keepCurrentTime -> current?.startMinute
                else -> defaultTime?.startMinute
            },
            endMinute = when {
                isLeave -> null
                keepCurrentTime -> current?.endMinute
                else -> defaultTime?.endMinute
            },
            breakMinutes = when {
                isLeave -> 0
                keepCurrentTime -> current?.breakMinutes ?: 0
                else -> defaultTime?.breakMinutes ?: 0
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

    private fun seedReferenceShortcutsOnce() {
        if (prefs.getBoolean(KEY_REFERENCE_SHORTCUTS_SEEDED, false)) return

        val defaults = listOf(
            CustomShiftPreset("J", "J", 0xFF64748BL),
            CustomShiftPreset("SD", "SD", 0xFF334155L)
        )
        defaults.forEach { preset ->
            if (
                customShiftPresets.keys.none { it.equals(preset.code, ignoreCase = true) } &&
                DefaultShiftTypes.presets.none { it.code.equals(preset.code, ignoreCase = true) } &&
                customShiftPresets.size < MAX_CUSTOM_PRESETS
            ) {
                customShiftPresets[preset.code] = preset
            }
        }
        persistCustomShiftPresets()
        prefs.edit().putBoolean(KEY_REFERENCE_SHORTCUTS_SEEDED, true).apply()
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
        runCatching {
            val array = JSONArray(raw)
            repeat(array.length().coerceAtMost(MAX_CUSTOM_PRESETS)) { index ->
                parseCustomShiftPreset(array.optJSONObject(index))?.let { preset ->
                    if (DefaultShiftTypes.presets.none { it.code.equals(preset.code, ignoreCase = true) }) {
                        customShiftPresets[preset.code] = preset
                    }
                }
            }
        }.onFailure { customShiftPresets.clear() }
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
        runCatching {
            val array = JSONArray(raw)
            repeat(array.length().coerceAtMost(MAX_WORK_TIME_PRESETS)) { index ->
                parseWorkTimePreset(array.optJSONObject(index))?.let { preset ->
                    if (shiftType(preset.code) != null && !ScheduleLogic.isLeaveCode(preset.code)) {
                        workTimePresets[preset.code] = preset
                    }
                }
            }
        }.onFailure { workTimePresets.clear() }
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
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            repeat(array.length()) { i ->
                parseEntry(array.optJSONObject(i))?.let { entries[it.date] = it }
            }
        }.onFailure {
            entries.clear()
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
        val validTime = start != null && end != null &&
            ScheduleLogic.workDurationMinutes(start, end, breakMinutes)?.let { it > 0 } == true &&
            !ScheduleLogic.isLeaveCode(code)
        return ShiftEntry(
            date = date,
            code = code,
            label = obj.optString("label", code).take(MAX_CUSTOM_LENGTH),
            colorArgb = obj.optLong("color", DEFAULT_CUSTOM_COLOR),
            note = obj.optString("note", "").take(MAX_NOTE_LENGTH),
            startMinute = if (validTime) start else null,
            endMinute = if (validTime) end else null,
            breakMinutes = if (validTime) breakMinutes else 0
        )
    }

    private fun persistEntries() {
        appendArchiveDiff()
        prefs.edit().putString(KEY_ENTRIES, entriesToJson().toString()).apply()
        persistedSnapshot = entries.mapValues { (_, entry) -> entry.copy() }
    }

    private fun appendArchiveDiff() {
        val dates = (persistedSnapshot.keys + entries.keys).toSortedSet()
        val changed = dates.filter { date -> persistedSnapshot[date] != entries[date] }
        if (changed.isEmpty()) return

        runCatching {
            context.openFileOutput(HISTORY_FILE, Context.MODE_APPEND).bufferedWriter().use { writer ->
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
            }
            archiveRevisionCount.value += changed.size
        }
    }

    private fun countArchiveRevisions(): Int =
        runCatching {
            context.getFileStreamPath(HISTORY_FILE)
                .takeIf { it.exists() }
                ?.bufferedReader()
                ?.useLines { lines -> lines.count().coerceAtMost(Int.MAX_VALUE) }
                ?: 0
        }.getOrDefault(0)

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

    private fun entryToJsonObject(item: ShiftEntry): JSONObject = JSONObject().apply {
        put("date", item.date.toString())
        put("code", item.code)
        put("label", item.label)
        put("color", item.colorArgb)
        put("note", item.note)
        if (item.startMinute != null) put("startMinute", item.startMinute)
        if (item.endMinute != null) put("endMinute", item.endMinute)
        put("breakMinutes", item.breakMinutes)
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
        private const val DATA_SCHEMA_VERSION = 8
        private const val ARCHIVE_SCHEMA_VERSION = 1
        private const val HISTORY_FILE = "takto_schedule_history.jsonl"
        private const val PREFS_NAME = "takto_schedule"
        private const val KEY_ENTRIES = "entries_json"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_SHIFT_REMINDERS = "shift_reminders"
        private const val KEY_SHIFT_REMINDER_LEAD_MINUTES = "shift_reminder_lead_minutes"
        private const val KEY_STANDARD_DAILY_MINUTES = "standard_daily_minutes"
        private const val KEY_MONTHLY_TARGET_OVERRIDES = "monthly_target_overrides_json"
        private const val KEY_WORK_TIME_PRESETS = "work_time_presets_json"
        private const val KEY_SHIFT_COLORS = "shift_colors_json"
        private const val KEY_CUSTOM_SHIFT_PRESETS = "custom_shift_presets_json"
        private const val KEY_SAVED_PATTERNS = "saved_patterns_json"
        private const val KEY_USER_PROFILE = "user_profile_json"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_REFERENCE_SHORTCUTS_SEEDED = "reference_shortcuts_seeded"

        private val DATE_FORMATS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d.M.uuuu."),
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("d/M/uuuu")
        )
    }
}
