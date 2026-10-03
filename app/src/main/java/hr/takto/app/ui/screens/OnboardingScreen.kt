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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.ui.components.GlassCard
import hr.takto.app.ui.components.TaktoLogo
import hr.takto.app.ui.components.readableContentColor
import hr.takto.app.ui.theme.TaktoAmber
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoGreen
import hr.takto.app.ui.theme.TaktoPurple
import hr.takto.app.ui.theme.TaktoRed

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme
    val title = when (page) {
        0 -> "Dobrodošli u Takto"
        1 -> "Prilagodi Takto svom poslu"
        else -> "Tvoj mjesec na prvi pogled"
    }
    val body = when (page) {
        0 -> "Jedno mjesto za raspored, obveze, odsutnosti i radne sate — bez obzira kako izgleda tvoj radni dan."
        1 -> "Koristi gotove oznake ili napravi svoje. Postavi fond sati, radno vrijeme, podsjetnike i način prikaza."
        else -> "Pregledaj cijeli mjesec, brzo pronađi važan dan i zadrži stare i buduće rasporede spremljene."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TaktoLogo(iconSize = 52.dp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFinish) {
                    Text("Preskoči", color = colors.onSurfaceVariant)
                }
            }

            Text(
                "PLANIRAJ · OZNAČI · BUDI U TIJEKU",
                color = colors.onSurfaceVariant,
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(30.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(26.dp))

            when (page) {
                0 -> WelcomePanel()
                1 -> PersonalizationPanel()
                else -> CalendarPreviewPanel()
            }

            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(if (index == page) 10.dp else 7.dp)
                            .background(
                                if (index == page) colors.primary
                                else colors.onSurfaceVariant.copy(alpha = 0.30f),
                                CircleShape
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (page < 2) page++ else onFinish() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (page < 2) "Dalje" else "Otvori Takto", fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun WelcomePanel() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            FeatureLine(
                Icons.Default.CalendarMonth,
                "Raspored bez komplikacija",
                "Upiši rad, obvezu, odsutnost ili vlastitu oznaku za bilo koji dan."
            )
            FeatureLine(
                Icons.Default.Schedule,
                "Sati i mjesečni fond",
                "Prati evidentirano vrijeme, redovne sate i potvrđene prekovremene."
            )
            FeatureLine(
                Icons.Default.AddPhotoAlternate,
                "Uvoz rasporeda sa slike",
                "Prepoznaj datume i oznake lokalno na uređaju."
            )
        }
    }
}

@Composable
private fun PersonalizationPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactFeature(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                title = "Vlastite oznake",
                subtitle = "Naziv, kratica i boja"
            )
            CompactFeature(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                title = "Radno vrijeme",
                subtitle = "Početak, kraj i pauza"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactFeature(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Notifications,
                title = "Podsjetnici",
                subtitle = "Kad ih želiš"
            )
            CompactFeature(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Backup,
                title = "Sigurnosna kopija",
                subtitle = "Uvoz, izvoz i arhiva"
            )
        }
    }
}

@Composable
private fun CalendarPreviewPanel() {
    val colors = MaterialTheme.colorScheme
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Primjer pregleda", style = MaterialTheme.typography.titleLarge)
            Text(
                "Oznake mogu predstavljati ono što je tebi važno.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    "R" to TaktoBlue,
                    "GO" to TaktoGreen,
                    "EDU" to TaktoPurple,
                    "" to colors.surfaceVariant,
                    "BO" to TaktoAmber,
                    "+" to TaktoCyan,
                    "" to colors.surfaceVariant
                ).forEach { (code, color) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .background(color, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (code.isNotBlank()) {
                            Text(
                                code,
                                color = readableContentColor(color),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (code.length > 2) 12.sp else 16.sp
                            )
                        }
                    }
                }
            }
            Text(
                "Prazno polje znači da za taj dan nema spremljenog unosa.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CompactFeature(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val colors = MaterialTheme.colorScheme
    GlassCard(modifier = modifier, padding = androidx.compose.foundation.layout.PaddingValues(14.dp), corner = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary)
            }
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FeatureLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(48.dp)
                .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary)
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
