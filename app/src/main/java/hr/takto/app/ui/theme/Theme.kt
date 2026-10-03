package hr.takto.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Takto namjerno koristi jedan, svijetli vizualni sustav.
 *
 * Time je izgled predvidljiv na svim uređajima, bez skrivenog prebacivanja prema
 * sistemskoj tamnoj temi i bez zasebnog skupa boja koji bi mogao odstupati od QA-a.
 */
private val TaktoColorScheme = lightColorScheme(
    primary = Color(0xFF176FCE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFC),
    onPrimaryContainer = Color(0xFF0A315D),
    secondary = Color(0xFF6C43D9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE5FF),
    onSecondaryContainer = Color(0xFF321A73),
    tertiary = Color(0xFF087D68),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9F4ED),
    onTertiaryContainer = Color(0xFF075244),
    background = TaktoLightBackground,
    onBackground = TaktoLightText,
    surface = TaktoLightSurface,
    onSurface = TaktoLightText,
    surfaceVariant = TaktoLightSurface2,
    onSurfaceVariant = TaktoLightMuted,
    surfaceContainer = TaktoLightSurface2,
    surfaceContainerHigh = TaktoLightSurface3,
    outline = TaktoLightOutline,
    outlineVariant = Color(0xFFC9D4DE),
    error = Color(0xFFC6283E),
    onError = Color.White
)

@Composable
fun TaktoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TaktoColorScheme,
        typography = TaktoTypography,
        content = content
    )
}
