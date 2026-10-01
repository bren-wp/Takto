package hr.takto.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hr.takto.app.data.ScheduleStore
import hr.takto.app.reminders.ReminderScheduler
import hr.takto.app.ui.components.TaktoAmbientBackground
import hr.takto.app.ui.screens.CalendarScreen
import hr.takto.app.ui.screens.HomeScreen
import hr.takto.app.ui.screens.OnboardingScreen
import hr.takto.app.ui.screens.PatternsScreen
import hr.takto.app.ui.screens.SettingsScreen
import hr.takto.app.ui.screens.StatsScreen
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoPurple
import hr.takto.app.ui.theme.TaktoSurface
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
            TaktoTheme {
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
    var current by remember { mutableStateOf(MainSection.HOME) }
    var calendarDate by remember { mutableStateOf<LocalDate?>(null) }
    val context = LocalContext.current

    // Čitanje snapshot mape ovdje osigurava da se alarm sljedeće smjene ponovno
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
            calendarDate = requestedDate
            current = MainSection.CALENDAR
        }
    }

    if (!store.onboardingDone.value) {
        OnboardingScreen(onFinish = store::finishOnboarding)
        return
    }

    TaktoAmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                TaktoBottomBar(current = current, onSelect = { section ->
                    if (section == MainSection.CALENDAR && current != MainSection.CALENDAR) {
                        calendarDate = null
                    }
                    current = section
                })
            }
        ) { padding ->
            when (current) {
                MainSection.HOME -> HomeScreen(
                    store = store,
                    contentPadding = padding,
                    onOpenCalendar = { date ->
                        calendarDate = date
                        current = MainSection.CALENDAR
                    },
                    onOpenStats = { current = MainSection.STATS },
                    onOpenPatterns = { current = MainSection.PATTERNS },
                    onOpenLeave = {
                        calendarDate = LocalDate.now()
                        current = MainSection.CALENDAR
                    }
                )

                MainSection.CALENDAR -> CalendarScreen(
                    store = store,
                    contentPadding = padding,
                    initialDate = calendarDate
                )

                MainSection.STATS -> StatsScreen(store = store, contentPadding = padding)
                MainSection.PATTERNS -> PatternsScreen(store = store, contentPadding = padding)
                MainSection.MORE -> SettingsScreen(store = store, contentPadding = padding)
            }
        }
    }
}

@Composable
private fun TaktoBottomBar(current: MainSection, onSelect: (MainSection) -> Unit) {
    Column {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            TaktoCyan.copy(alpha = 0.62f),
                            TaktoBlue.copy(alpha = 0.72f),
                            TaktoPurple.copy(alpha = 0.56f),
                            Color.Transparent
                        )
                    )
                )
        )
        NavigationBar(containerColor = TaktoSurface.copy(alpha = 0.985f)) {
            MainSection.entries.forEach { section ->
                NavigationBarItem(
                    selected = current == section,
                    onClick = { onSelect(section) },
                    icon = { Icon(section.icon, contentDescription = section.label) },
                    label = { Text(section.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TaktoCyan,
                        selectedTextColor = TaktoCyan,
                        indicatorColor = TaktoBlue.copy(alpha = 0.10f),
                        unselectedIconColor = Color(0xFF95A7C2),
                        unselectedTextColor = Color(0xFF95A7C2)
                    )
                )
            }
        }
    }
}
