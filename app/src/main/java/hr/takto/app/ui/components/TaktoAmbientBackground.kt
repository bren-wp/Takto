package hr.takto.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoPurple

@Composable
fun TaktoAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.surfaceContainerHigh,
                    0.52f to colors.background,
                    1f to colors.background
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawTaktoAurora()
        }
        content()
    }
}

private fun DrawScope.drawTaktoAurora() {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                TaktoBlue.copy(alpha = 0.13f),
                TaktoPurple.copy(alpha = 0.06f),
                Color.Transparent
            ),
            center = Offset(w * 0.92f, h * 0.06f),
            radius = w * 0.95f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                TaktoCyan.copy(alpha = 0.09f),
                TaktoBlue.copy(alpha = 0.03f),
                Color.Transparent
            ),
            center = Offset(w * 0.08f, h * 0.62f),
            radius = w * 0.82f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                TaktoPurple.copy(alpha = 0.09f),
                Color.Transparent
            ),
            center = Offset(w * 0.78f, h * 0.92f),
            radius = w * 0.66f
        )
    )
}
