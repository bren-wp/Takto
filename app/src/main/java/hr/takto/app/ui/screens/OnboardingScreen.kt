package hr.takto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.model.DefaultShiftTypes
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.ShiftChoice
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.theme.TaktoBackground
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoGreen
import hr.takto.app.ui.theme.TaktoMuted
import hr.takto.app.ui.theme.TaktoPurple
import hr.takto.app.ui.theme.TaktoSurface
import hr.takto.app.ui.theme.TaktoText

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val title = when (page) {
        0 -> "Dobrodošli u Takto"
        1 -> "Dodijeli svoju smjenu"
        else -> "Pogledaj svoj mjesec odmah"
    }
    val body = when (page) {
        0 -> "Jednostavan način za planiranje i upravljanje smjenama."
        1 -> "Jednim dodirom postavi oznaku smjene koja ti odgovara."
        else -> "Dobij jasan pregled rasporeda. Prazna kućica znači slobodan dan."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111B3E), TaktoBackground, Color(0xFF080D1C))
                )
            )
    ) {
        // ambient brand glow
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(260.dp)
                .background(
                    Brush.radialGradient(listOf(TaktoCyan.copy(alpha = 0.18f), Color.Transparent)),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TaktoLogo(iconSize = 54.dp)
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onFinish) {
                    Text("Preskoči", color = TaktoMuted)
                }
            }
            Text(
                "Dodirni. Označi. Radi.",
                color = TaktoMuted,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(36.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(body, color = TaktoMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(28.dp))

            when (page) {
                0 -> WelcomePanel()
                1 -> ShiftPanel()
                else -> CalendarPreviewPanel()
            }

            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(if (index == page) 10.dp else 7.dp)
                            .background(if (index == page) TaktoBlue else TaktoMuted.copy(alpha = 0.35f), CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (page < 2) page++ else onFinish()
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (page < 2) "Dalje" else "Počni planirati", fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun WelcomePanel() {
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            FeatureLine(Icons.Default.CalendarMonth, "Jednostavno i brzo", "Velike kućice kalendara za lagani dodir.")
            FeatureLine(Icons.Default.GridView, "Stvoreno za smjenski rad", "D, N, GO, BO i PD uvijek su jasno vidljivi.")
            FeatureLine(Icons.Default.Insights, "Tvoj raspored. Na tvoj način.", "Vlastiti unos može sadržavati bilo što.")
        }
    }
}

@Composable
private fun ShiftPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftChoice(DefaultShiftTypes.day, Modifier.weight(1f)) {}
            ShiftChoice(DefaultShiftTypes.night, Modifier.weight(1f)) {}
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftChoice(DefaultShiftTypes.annual, Modifier.weight(1f)) {}
            ShiftChoice(DefaultShiftTypes.sick, Modifier.weight(1f)) {}
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftChoice(DefaultShiftTypes.paid, Modifier.weight(1f)) {}
            GlassCard(modifier = Modifier.weight(1f).height(84.dp), padding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Edit, null, tint = TaktoText)
                    Text("Vlastiti unos", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CalendarPreviewPanel() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ožujak 2025.", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    "D" to DefaultShiftTypes.day.color,
                    "N" to DefaultShiftTypes.night.color,
                    "GO" to DefaultShiftTypes.annual.color,
                    "" to TaktoSurface,
                    "BO" to DefaultShiftTypes.sick.color,
                    "PD" to DefaultShiftTypes.paid.color,
                    "" to TaktoSurface
                ).forEach { (code, color) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .background(color, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (code.isNotBlank()) Text(code, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text("Prazna kućica = slobodan dan", color = TaktoMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(48.dp).background(TaktoBlue.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = TaktoCyan) }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TaktoMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
