package hr.takto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(colors.surfaceContainerHigh.copy(alpha = 0.94f), colors.surface.copy(alpha = 0.98f))
                ),
                shape
            )
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.90f), shape)
            .padding(padding),
        content = content
    )
}
