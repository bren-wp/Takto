package hr.takto.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.takto.app.ui.theme.TaktoBlue
import hr.takto.app.ui.theme.TaktoCyan
import hr.takto.app.ui.theme.TaktoOutline
import hr.takto.app.ui.theme.TaktoPurple
import hr.takto.app.ui.theme.TaktoSurface
import hr.takto.app.ui.theme.TaktoSurface2

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val outline = Brush.linearGradient(
        listOf(
            TaktoCyan.copy(alpha = 0.34f),
            TaktoBlue.copy(alpha = 0.22f),
            TaktoOutline.copy(alpha = 0.78f),
            TaktoPurple.copy(alpha = 0.24f)
        )
    )

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        TaktoSurface2.copy(alpha = 0.96f),
                        TaktoSurface.copy(alpha = 0.985f)
                    )
                ),
                shape
            )
            .border(BorderStroke(1.dp, outline), shape)
            .padding(padding),
        content = content
    )
}
