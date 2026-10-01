package hr.takto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.Canvas
import hr.takto.app.ui.theme.TaktoBackground
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoPurple

/**
 * Zajednička Takto pozadina prema dostavljenom vizualnom identitetu.
 *
 * Glavni sloj ostaje vrlo taman kako bi obojene smjene imale maksimalan
 * kontrast, a cijan/plavi/ljubičasti "aurora" sjaj daje isti premium osjećaj
 * kao na referentnim ekranima bez bitmap pozadina i bez dodatnog GPU tereta.
 */
@Composable
fun TaktoAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF071426),
                    0.52f to TaktoBackground,
                    1f to Color(0xFF050B16)
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
                TaktoBlue.copy(alpha = 0.20f),
                TaktoPurple.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(w * 0.92f, h * 0.06f),
            radius = w * 0.95f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                TaktoCyan.copy(alpha = 0.13f),
                TaktoBlue.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = Offset(w * 0.08f, h * 0.62f),
            radius = w * 0.82f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                TaktoPurple.copy(alpha = 0.14f),
                Color.Transparent
            ),
            center = Offset(w * 0.78f, h * 0.92f),
            radius = w * 0.66f
        )
    )
}
