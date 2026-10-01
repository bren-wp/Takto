package hr.takto.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import hr.takto.app.model.CustomShiftPreset
import hr.takto.app.model.ICalendarExporter
import hr.takto.app.model.DefaultShiftTypes
import hr.takto.app.model.SavedPattern
import hr.takto.app.model.ScheduleLogic
import hr.takto.app.model.ShiftEntry
import hr.takto.app.model.ShiftType
import hr.takto.app.model.WorkTimePreset
import org.json.JSONArray
import org.json.JSONObject
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
class ScheduleStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val entries = mutableStateMapOf<LocalDate, ShiftEntry>()
    val shiftColors = mutableStateMapOf<String, Long>()
    val customShiftPresets = mutableStateMapOf<String, CustomShiftPreset>()
    val savedPatterns = mutableStateListOf<SavedPattern>()
    val workTimePresets = mutableStateMapOf<String, WorkTimePreset>()
    val monthlyTargetOverrides = mutableStateMapOf<String, Int>()
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
        loadMonthlyTargetOverrides()
        loadSavedPatterns()
        loadEntries()
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
