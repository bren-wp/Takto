package hr.takto.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberUtilsTest {
    @Test
    fun formatsEuroWithCroatianDecimalSeparator() {
        assertEquals("1.234,50 €", formatEuro(1234.5))
        assertEquals("0,00 €", formatEuro(0.0))
    }
}
