package hr.takto.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TaktoScheme = darkColorScheme(
    primary = TaktoBlue,
    onPrimary = Color.White,
    secondary = TaktoPurple,
    tertiary = TaktoGreen,
    background = TaktoBackground,
    onBackground = TaktoText,
    surface = TaktoSurface,
    onSurface = TaktoText,
    surfaceVariant = TaktoSurface2,
    onSurfaceVariant = TaktoMuted,
    outline = TaktoOutline,
    error = TaktoRed
)

@Composable
fun TaktoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TaktoScheme, typography = TaktoTypography, content = content)
}
