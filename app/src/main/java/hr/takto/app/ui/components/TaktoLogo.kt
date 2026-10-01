package hr.takto.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoGreen
import hr.takto.app.ui.theme.TaktoPurple
import hr.takto.app.ui.theme.TaktoText

@Composable
fun TaktoLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 34.dp,
    showWordmark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaktoMark(size = iconSize)
        if (showWordmark) {
            Text(
                text = "akto",
                color = TaktoText,
                fontSize = (iconSize.value * 0.76f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp
            )
        }
    }
}

@Composable
fun TaktoMark(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        drawTaktoMark()
    }
}

private fun DrawScope.drawTaktoMark() {
    val brush = Brush.linearGradient(
        colors = listOf(TaktoGreen, TaktoCyan, Color(0xFF3B82F6), TaktoPurple),
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height)
    )
    val r = size.minDimension * 0.16f
    drawRoundRect(
        brush = brush,
        topLeft = Offset(size.width * 0.06f, size.height * 0.12f),
        size = Size(size.width * 0.88f, size.height * 0.32f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
    drawRoundRect(
        brush = brush,
        topLeft = Offset(size.width * 0.42f, size.height * 0.27f),
        size = Size(size.width * 0.33f, size.height * 0.63f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
    // Small inner shadow-like cut for the recognizable bent T silhouette.
    drawRoundRect(
        color = Color(0xFF0F172A).copy(alpha = 0.30f),
        topLeft = Offset(size.width * 0.62f, size.height * 0.28f),
        size = Size(size.width * 0.12f, size.height * 0.22f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.6f, r * 0.6f)
    )
}
