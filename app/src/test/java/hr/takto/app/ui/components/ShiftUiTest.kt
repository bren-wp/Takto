package hr.takto.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftUiTest {
    @Test
    fun readableContentColorUsesDarkTextOnLightBackgrounds() {
        assertEquals(Color(0xFF071018), readableContentColor(Color.White))
        assertEquals(Color(0xFF071018), readableContentColor(Color(0xFFFFD54F)))
    }

    @Test
    fun readableContentColorUsesWhiteTextOnDarkBackgrounds() {
        assertEquals(Color.White, readableContentColor(Color.Black))
        assertEquals(Color.White, readableContentColor(Color(0xFF1E3A8A)))
    }
}
