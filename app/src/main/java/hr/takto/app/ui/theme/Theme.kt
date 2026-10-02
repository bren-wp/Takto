package hr.takto.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import hr.takto.app.model.AppThemeMode

private val TaktoDarkScheme = darkColorScheme(
    primary = TaktoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B5684),
    onPrimaryContainer = Color(0xFFF2F7FF),
    secondary = TaktoPurple,
    onSecondary = Color.White,
    tertiary = TaktoGreen,
    onTertiary = Color(0xFF042B24),
    background = TaktoBackground,
    onBackground = TaktoText,
    surface = TaktoSurface,
    onSurface = TaktoText,
    surfaceVariant = TaktoSurface2,
    onSurfaceVariant = TaktoMuted,
    surfaceContainer = TaktoSurface2,
    surfaceContainerHigh = TaktoSurface3,
    outline = TaktoOutline,
    outlineVariant = TaktoOutline.copy(alpha = 0.78f),
    error = TaktoRed,
    onError = Color.White
)

private val TaktoLightScheme = lightColorScheme(
    primary = Color(0xFF176FCE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8FB),
    onPrimaryContainer = Color(0xFF0B315B),
    secondary = Color(0xFF7047E8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E1FF),
    onSecondaryContainer = Color(0xFF311B75),
    tertiary = Color(0xFF0C8F72),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F6EC),
    onTertiaryContainer = Color(0xFF075243),
    background = TaktoLightBackground,
    onBackground = TaktoLightText,
    surface = TaktoLightSurface,
    onSurface = TaktoLightText,
    surfaceVariant = TaktoLightSurface2,
    onSurfaceVariant = TaktoLightMuted,
    surfaceContainer = TaktoLightSurface2,
    surfaceContainerHigh = TaktoLightSurface3,
    outline = TaktoLightOutline,
    outlineVariant = Color(0xFFC9D3DD),
    error = Color(0xFFC6283E),
    onError = Color.White
)

@Composable
fun TaktoTheme(
    mode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (mode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) TaktoDarkScheme else TaktoLightScheme,
        typography = TaktoTypography,
        content = content
    )
}
