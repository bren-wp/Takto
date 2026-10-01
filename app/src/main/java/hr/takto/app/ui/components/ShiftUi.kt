package hr.takto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.takto.app.model.ShiftType

@Composable
fun ShiftChoice(type: ShiftType, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(84.dp)
            .background(type.color, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = type.code,
                color = Color.White,
                fontSize = if (type.code.length <= 1) 30.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = type.name,
                color = Color.White.copy(alpha = 0.95f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun shiftCodeFontSize(code: String): androidx.compose.ui.unit.TextUnit = when {
    code.length <= 1 -> 25.sp
    code.length <= 3 -> 19.sp
    code.length <= 5 -> 15.sp
    code.length <= 8 -> 12.sp
    else -> 10.sp
}

fun shiftCodeCompactFontSize(code: String): androidx.compose.ui.unit.TextUnit = when {
    code.length <= 1 -> 18.sp
    code.length <= 3 -> 14.sp
    code.length <= 5 -> 11.sp
    code.length <= 8 -> 9.sp
    else -> 8.sp
}
