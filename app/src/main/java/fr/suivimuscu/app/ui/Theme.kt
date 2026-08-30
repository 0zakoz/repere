package fr.suivimuscu.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF0F1115)
val AppSurface = Color(0xFF191C22)
val Lime = Color(0xFFB7F34A)
val AppText = Color(0xFFF2F5F0)
val Muted = Color(0xFFA7ADB8)
val Cyan = Color(0xFF56D6E7)
val Purple = Color(0xFFB89CFF)
val Orange = Color(0xFFFFB45C)

private val Colors = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF172000),
    secondary = Cyan,
    tertiary = Purple,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = Color(0xFF252A33),
    onSurfaceVariant = Color(0xFFD2D6DE),
    outline = Color(0xFF4B515D),
    error = Color(0xFFFF6B6B),
)

@Composable
fun SuiviMuscuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
