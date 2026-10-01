package hr.takto.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeModeTest {
    @Test
    fun missingValueDefaultsToDark() {
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromPersisted(null))
    }

    @Test
    fun invalidValueDefaultsToDark() {
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromPersisted("unknown"))
    }

    @Test
    fun explicitModesArePreserved() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromPersisted("system"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromPersisted("dark"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromPersisted("light"))
    }
}
