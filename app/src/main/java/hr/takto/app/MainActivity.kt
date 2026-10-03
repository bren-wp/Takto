package hr.takto.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.AppThemeMode
import hr.takto.app.reminders.ReminderScheduler
import hr.takto.app.ui.screens.CalendarScreen
import hr.takto.app.ui.screens.HomeScreen
import hr.takto.app.ui.screens.OnboardingScreen
import hr.takto.app.ui.screens.PatternsScreen
import hr.takto.app.ui.screens.SettingsScreen
import hr.takto.app.ui.screens.StatsScreen
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val openDateRequest = mutableStateOf<LocalDate?>(null)
    private val openRequestNonce = mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeNavigationIntent(intent)
        val store = (application as TaktoApplication).scheduleStore
        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkAppearance = when (store.themeMode.value) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> systemDark
            }
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkAppearance
                controller.isAppearanceLightNavigationBars = !darkAppearance
            }

            TaktoTheme(mode = store.themeMode.value) {
                TaktoRoot(
                    store = store,
                    requestedDate = openDateRequest.value,
                    requestNonce = openRequestNonce.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNavigationIntent(intent)
    }

    private fun consumeNavigationIntent(intent: Intent?) {
        val date = intent?.getStringExtra(EXTRA_OPEN_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return
        openDateRequest.value = date
        openRequestNonce.value = openRequestNonce.value + 1
    }

    companion object {
        const val EXTRA_OPEN_DATE = "hr.takto.app.extra.OPEN_DATE"
    }
}

enum class MainSection(val label: String, val icon: ImageVector) {
    HOME("Početna", Icons.Default.Home),
    CALENDAR("Kalendar", Icons.Default.CalendarMonth),
    STATS("Statistika", Icons.Default.BarChart),
    PATTERNS("Uzorci", Icons.Default.Link),
    MORE("Više", Icons.Default.MoreHoriz)
}

@Composable
private fun TaktoRoot(
    store: ScheduleStore,
    requestedDate: LocalDate?,
    requestNonce: Long
) {
    var currentName by rememberSaveable { mutableStateOf(MainSection.HOME.name) }
    var calendarDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    val current = MainSection.entries.firstOrNull { it.name == currentName } ?: MainSection.HOME
    val calendarDate = calendarDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val context = LocalContext.current
    // Čitanje snapshot mape ovdje osigurava da se alarm sljedećeg rada ponovno
    // izračuna nakon dodavanja, brisanja ili promjene vremena bez ručnog poziva iz svakog ekrana.
    val reminderScheduleSignature = store.entries.values
        .asSequence()
        .filter { it.hasWorkTime }
        .sortedWith(compareBy({ it.date }, { it.startMinute ?: -1 }))
        .joinToString("|") { "${it.date}:${it.startMinute}:${it.endMinute}:${it.breakMinutes}:${it.code}" }

    LaunchedEffect(
        reminderScheduleSignature,
        store.remindersEnabled.value,
        store.reminderHour.value,
        store.reminderMinute.value,
        store.shiftRemindersEnabled.value,
        store.shiftReminderLeadMinutes.value
    ) {
        ReminderScheduler.sync(context, store)
    }

    LaunchedEffect(requestNonce, requestedDate, store.onboardingDone.value) {
        if (store.onboardingDone.value && requestedDate != null) {
            calendarDateIso = requestedDate.toString()
            currentName = MainSection.CALENDAR.name
        }
    }
    if (!store.onboardingDone.value) {
        OnboardingScreen(onFinish = store::finishOnboarding)
        return
    }

    BackHandler(enabled = current != MainSection.HOME) {
        currentName = MainSection.HOME.name
        calendarDateIso = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                TaktoBottomBar(current = current, onSelect = { section ->
                    if (section == MainSection.CALENDAR && current != MainSection.CALENDAR) {
                        calendarDateIso = null
                    }
                    currentName = section.name
                })
            }
        ) { padding ->
            when (current) {
                MainSection.HOME -> HomeScreen(
                    store = store,
                    contentPadding = padding,
                    onOpenCalendar = { date ->
                        calendarDateIso = date?.toString()
                        currentName = MainSection.CALENDAR.name
                    }
                )
                MainSection.CALENDAR -> CalendarScreen(store = store, contentPadding = padding, initialDate = calendarDate)
                MainSection.STATS -> StatsScreen(store = store, contentPadding = padding)
                MainSection.PATTERNS -> PatternsScreen(store = store, contentPadding = padding)
                MainSection.MORE -> SettingsScreen(store = store, contentPadding = padding)
            }
        }
    }
}

@Composable
private fun TaktoBottomBar(current: MainSection, onSelect: (MainSection) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        MainSection.entries.forEach { section ->
            NavigationBarItem(
                selected = current == section,
                onClick = { onSelect(section) },
                icon = { Icon(section.icon, contentDescription = section.label) },
                label = { Text(section.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TaktoBlue,
                    selectedTextColor = TaktoBlue,
                    indicatorColor = TaktoBlue.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
