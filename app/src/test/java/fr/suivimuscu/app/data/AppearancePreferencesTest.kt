package fr.suivimuscu.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun defaultsPreserveOriginalDarkAppearance() {
        assertEquals(
            AppearancePreferences(AppThemeId.ORIGINAL, ThemeMode.DARK),
            AppearancePreferences(),
        )
    }

    @Test
    fun storedValuesDecodeAllThemesAndModes() {
        AppThemeId.entries.forEach { assertEquals(it, storedTheme(it.name)) }
        ThemeMode.entries.forEach { assertEquals(it, storedThemeMode(it.name)) }
    }

    @Test
    fun missingOrUnknownValuesFallBackSafely() {
        assertEquals(AppThemeId.ORIGINAL, storedTheme(null))
        assertEquals(AppThemeId.ORIGINAL, storedTheme("UNKNOWN"))
        assertEquals(ThemeMode.DARK, storedThemeMode(null))
        assertEquals(ThemeMode.DARK, storedThemeMode("UNKNOWN"))
    }
}
