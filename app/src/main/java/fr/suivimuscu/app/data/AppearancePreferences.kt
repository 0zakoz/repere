package fr.suivimuscu.app.data

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class AppThemeId { ORIGINAL, KAWAII, PASTEL, OLED, PURE }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppearancePreferences(
    val theme: AppThemeId = AppThemeId.ORIGINAL,
    val mode: ThemeMode = ThemeMode.DARK,
)

internal fun storedTheme(value: String?): AppThemeId =
    AppThemeId.entries.firstOrNull { it.name == value } ?: AppThemeId.ORIGINAL

internal fun storedThemeMode(value: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.DARK

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance")

class AppearanceRepository(context: Context) {
    private val dataStore = context.applicationContext.appearanceDataStore

    val preferences: Flow<AppearancePreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            AppearancePreferences(
                theme = storedTheme(values[THEME]?.uppercase()),
                mode = storedThemeMode(values[MODE]?.uppercase()),
            )
        }

    suspend fun setTheme(theme: AppThemeId) {
        dataStore.edit { it[THEME] = theme.name }
    }

    suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { it[MODE] = mode.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
    }
}
