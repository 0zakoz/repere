package fr.suivimuscu.app.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import fr.suivimuscu.app.data.AppThemeId
import fr.suivimuscu.app.data.AppearancePreferences
import fr.suivimuscu.app.data.ThemeMode
import fr.suivimuscu.app.R

@Immutable
data class AppVisuals(
    val success: Color,
    val warning: Color,
    val info: Color,
    val chartSeries: List<Color>,
    val chartBackground: Color,
    val chartGrid: Color,
    val heatmapLow: Color,
    val heatmapMid: Color,
    val heatmapHigh: Color,
    val activeContainer: Color,
    val pausedContainer: Color,
    val warningContainer: Color,
    val decorationPrimary: Color,
    val decorationSecondary: Color,
    val showKawaiiDecorations: Boolean,
    val kawaiiSurfaces: List<Color>,
    val kawaiiBackdrop: List<Color>,
)

internal data class AppThemeDefinition(
    val colors: ColorScheme,
    val visuals: AppVisuals,
    val typography: Typography,
    val shapes: Shapes,
)

private val OriginalBackground = Color(0xFF0F1115)
private val OriginalSurface = Color(0xFF191C22)
private val OriginalLime = Color(0xFFB7F34A)
private val OriginalText = Color(0xFFF2F5F0)
private val OriginalCyan = Color(0xFF56D6E7)
private val OriginalPurple = Color(0xFFB89CFF)
private val OriginalOrange = Color(0xFFFFB45C)

private val DefaultTypography = Typography()
@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resourceId: Int, vararg weights: FontWeight): FontFamily = FontFamily(
    *weights.map { weight ->
        Font(
            resourceId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    }.toTypedArray(),
)

private val Fredoka = variableFontFamily(
    R.font.fredoka_variable,
    FontWeight.Normal,
    FontWeight.SemiBold,
    FontWeight.Bold,
)
private val Nunito = variableFontFamily(
    R.font.nunito_variable,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)
private val RoundedTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = Nunito),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Nunito),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Nunito),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
)
private val PastelTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = Nunito),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = Nunito),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Nunito),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Nunito),
)
private val TechTypography = Typography(
    headlineMedium = DefaultTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = FontFamily.Monospace),
)
private val PureTypography = Typography(
    headlineMedium = DefaultTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
)

private val OriginalShapes = Shapes()
private val KawaiiShapes = themedShapes(18, 24, 30, 36, 44)
private val PastelShapes = themedShapes(10, 14, 18, 24, 28)
private val OledShapes = themedShapes(4, 6, 8, 12, 16)
private val PureShapes = themedShapes(8, 12, 16, 20, 24)

private fun themedShapes(extraSmall: Int, small: Int, medium: Int, large: Int, extraLarge: Int) = Shapes(
    extraSmall = RoundedCornerShape(extraSmall.dp),
    small = RoundedCornerShape(small.dp),
    medium = RoundedCornerShape(medium.dp),
    large = RoundedCornerShape(large.dp),
    extraLarge = RoundedCornerShape(extraLarge.dp),
)

private fun visuals(
    success: Color,
    warning: Color,
    info: Color,
    series: List<Color>,
    chartBackground: Color,
    chartGrid: Color,
    heatmap: Triple<Color, Color, Color>,
    kawaii: Boolean = false,
    kawaiiSurfaces: List<Color> = emptyList(),
    kawaiiBackdrop: List<Color> = emptyList(),
) = AppVisuals(
    success = success,
    warning = warning,
    info = info,
    chartSeries = series,
    chartBackground = chartBackground,
    chartGrid = chartGrid,
    heatmapLow = heatmap.first,
    heatmapMid = heatmap.second,
    heatmapHigh = heatmap.third,
    activeContainer = success.copy(alpha = .12f),
    pausedContainer = info.copy(alpha = .10f),
    warningContainer = warning.copy(alpha = .10f),
    decorationPrimary = series.first(),
    decorationSecondary = series.getOrElse(2) { info },
    showKawaiiDecorations = kawaii,
    kawaiiSurfaces = kawaiiSurfaces,
    kawaiiBackdrop = kawaiiBackdrop,
)

private fun definition(
    colors: ColorScheme,
    visuals: AppVisuals,
    typography: Typography,
    shapes: Shapes,
) = AppThemeDefinition(colors, visuals, typography, shapes)

internal fun themeDefinition(theme: AppThemeId, dark: Boolean): AppThemeDefinition = when (theme to dark) {
    AppThemeId.ORIGINAL to true -> definition(
        darkColorScheme(
            primary = OriginalLime, onPrimary = Color(0xFF172000),
            secondary = OriginalCyan, tertiary = OriginalPurple,
            background = OriginalBackground, onBackground = OriginalText,
            surface = OriginalSurface, onSurface = OriginalText,
            surfaceVariant = Color(0xFF252A33), onSurfaceVariant = Color(0xFFD2D6DE),
            outline = Color(0xFF4B515D), error = Color(0xFFFF6B6B),
        ),
        visuals(OriginalLime, OriginalOrange, OriginalCyan,
            listOf(OriginalLime, OriginalCyan, OriginalPurple, OriginalOrange),
            OriginalSurface, Color.White.copy(alpha = .08f),
            Triple(Color(0xFF262B36), Color(0xFF6E7F2E), OriginalLime)),
        DefaultTypography, OriginalShapes,
    )
    AppThemeId.ORIGINAL to false -> definition(
        lightColorScheme(
            primary = Color(0xFF4F6F00), onPrimary = Color.White,
            primaryContainer = Color(0xFFD8F5A2), onPrimaryContainer = Color(0xFF172000),
            secondary = Color(0xFF006876), tertiary = Color(0xFF644D9B),
            background = Color(0xFFF7F9F2), onBackground = Color(0xFF1A1C18),
            surface = Color.White, onSurface = Color(0xFF1A1C18),
            surfaceVariant = Color(0xFFE5E9DE), onSurfaceVariant = Color(0xFF454940),
            outline = Color(0xFF75796E), error = Color(0xFFBA1A1A)),
        visuals(Color(0xFF4F6F00), Color(0xFF8A5700), Color(0xFF006876),
            listOf(Color(0xFF4F6F00), Color(0xFF006876), Color(0xFF644D9B), Color(0xFF9A4D00)),
            Color.White, Color(0x1F1A1C18),
            Triple(Color(0xFFE5E9DE), Color(0xFFA8C96D), Color(0xFF4F6F00))),
        DefaultTypography, OriginalShapes,
    )
    AppThemeId.KAWAII to false -> definition(
        lightColorScheme(
            primary = Color(0xFFF3A8C7), onPrimary = Color(0xFF4F1730),
            primaryContainer = Color(0xFFFFCFE3), onPrimaryContainer = Color(0xFF451329),
            secondary = Color(0xFFFFDEA0), onSecondary = Color(0xFF4A3900),
            secondaryContainer = Color(0xFFFFEDBD), onSecondaryContainer = Color(0xFF3C2F00),
            tertiary = Color(0xFFB8DCFA), onTertiary = Color(0xFF143954),
            tertiaryContainer = Color(0xFFD8ECFF), onTertiaryContainer = Color(0xFF102F46),
            background = Color(0xFFFFEAF3), onBackground = Color(0xFF38202C),
            surface = Color(0xFFFFFBFD), onSurface = Color(0xFF38202C),
            surfaceVariant = Color(0xFFFFE8B8), onSurfaceVariant = Color(0xFF5B4250),
            surfaceContainerLowest = Color(0xFFFFF7FB),
            surfaceContainerLow = Color(0xFFFFDDEA),
            surfaceContainer = Color(0xFFFFEBC0),
            surfaceContainerHigh = Color(0xFFDDEEFF),
            surfaceContainerHighest = Color(0xFFFFF7FB),
            outline = Color(0xFF9C7084), outlineVariant = Color(0xFFD7AEC0),
            error = Color(0xFFBA1A1A)),
        visuals(Color(0xFF7A6412), Color(0xFFA44E72), Color(0xFF4A82AA),
            listOf(Color(0xFFC45682), Color(0xFFAA8612), Color(0xFF4A82AA), Color(0xFF66485A)),
            Color(0xFFFFFBFD), Color(0x335B4250),
            Triple(Color(0xFFFFE8F1), Color(0xFFF0AFCB), Color(0xFFB84A78)), true,
            kawaiiSurfaces = listOf(Color(0xFFFFDDEA), Color(0xFFFFEBC0), Color(0xFFDDEEFF), Color(0xFFFFF9FC), Color(0xFFFFD2E5)),
            kawaiiBackdrop = listOf(Color(0xFFFFE8F2), Color(0xFFFFF4D2), Color(0xFFE4F2FF), Color(0xFFFFFBFD), Color(0xFFFFE8F2))),
        RoundedTypography, KawaiiShapes,
    )
    AppThemeId.KAWAII to true -> definition(
        darkColorScheme(
            primary = Color(0xFFFFB4D1), onPrimary = Color(0xFF522039),
            primaryContainer = Color(0xFF713A58), onPrimaryContainer = Color(0xFFFFD8E8),
            secondary = Color(0xFFFFE09A), onSecondary = Color(0xFF493900),
            secondaryContainer = Color(0xFF615532), onSecondaryContainer = Color(0xFFFFEDB8),
            tertiary = Color(0xFFAFD8FA), onTertiary = Color(0xFF18384E),
            tertiaryContainer = Color(0xFF344F66), onTertiaryContainer = Color(0xFFD8ECFF),
            background = Color(0xFF2A1B28), onBackground = Color(0xFFFFE8F2),
            surface = Color(0xFF171218), onSurface = Color(0xFFFFE8F2),
            surfaceVariant = Color(0xFF4F4134), onSurfaceVariant = Color(0xFFF2CEDD),
            surfaceContainerLowest = Color(0xFF241722),
            surfaceContainerLow = Color(0xFF57384A),
            surfaceContainer = Color(0xFF51482E),
            surfaceContainerHigh = Color(0xFF30495C),
            surfaceContainerHighest = Color(0xFF211A22),
            outline = Color(0xFFC58FA8), outlineVariant = Color(0xFF76586A),
            error = Color(0xFFFFB4AB)),
        visuals(Color(0xFFFFE09A), Color(0xFFFFA6C7), Color(0xFFAFD8FA),
            listOf(Color(0xFFFF9FC8), Color(0xFFFFD878), Color(0xFF8DC8FA), Color(0xFFFFF2F7)),
            Color(0xFF171218), Color(0x33FFE8F2),
            Triple(Color(0xFF4B3544), Color(0xFFB75A82), Color(0xFFFF9FC8)), true,
            kawaiiSurfaces = listOf(Color(0xFF57384A), Color(0xFF51482E), Color(0xFF30495C), Color(0xFF171218), Color(0xFF62354D)),
            kawaiiBackdrop = listOf(Color(0xFF2A1B28), Color(0xFF3A2935), Color(0xFF37352A), Color(0xFF293A46), Color(0xFF171218), Color(0xFF2A1B28))),
        RoundedTypography, KawaiiShapes,
    )
    AppThemeId.PASTEL to false -> definition(
        lightColorScheme(
            primary = Color(0xFFA53D68), onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9E4), onPrimaryContainer = Color(0xFF3F001E),
            secondary = Color(0xFF32678F), secondaryContainer = Color(0xFFCDE7FF),
            tertiary = Color(0xFF756000), tertiaryContainer = Color(0xFFFFEFA5),
            background = Color(0xFFFFF9F2), onBackground = Color(0xFF211A1D),
            surface = Color(0xFFFFFCF8), onSurface = Color(0xFF211A1D),
            surfaceVariant = Color(0xFFF3E3E7), onSurfaceVariant = Color(0xFF514348),
            outline = Color(0xFF847378), error = Color(0xFFBA1A1A)),
        visuals(Color(0xFF32678F), Color(0xFF756000), Color(0xFFA53D68),
            listOf(Color(0xFFA53D68), Color(0xFF32678F), Color(0xFF756000), Color(0xFF72558F)),
            Color(0xFFFFFCF8), Color(0x1F514348),
            Triple(Color(0xFFF3E3E7), Color(0xFFE5A5BA), Color(0xFFA53D68))),
        PastelTypography, PastelShapes,
    )
    AppThemeId.PASTEL to true -> definition(
        darkColorScheme(
            primary = Color(0xFFFFB0CE), onPrimary = Color(0xFF5F1138),
            secondary = Color(0xFFA9D6F5), tertiary = Color(0xFFFFE083),
            background = Color(0xFF1C1A20), onBackground = Color(0xFFEDE0E5),
            surface = Color(0xFF28252C), onSurface = Color(0xFFEDE0E5),
            surfaceVariant = Color(0xFF484047), onSurfaceVariant = Color(0xFFD7C1C9),
            outline = Color(0xFF9A8990), error = Color(0xFFFFB4AB)),
        visuals(Color(0xFFA9D6F5), Color(0xFFFFE083), Color(0xFFFFB0CE),
            listOf(Color(0xFFFFB0CE), Color(0xFFA9D6F5), Color(0xFFFFE083), Color(0xFFC9B5FF)),
            Color(0xFF28252C), Color(0x26EDE0E5),
            Triple(Color(0xFF484047), Color(0xFFB45E83), Color(0xFFFFB0CE))),
        PastelTypography, PastelShapes,
    )
    AppThemeId.OLED to false -> definition(
        lightColorScheme(
            primary = Color(0xFF3047B8), onPrimary = Color.White,
            secondary = Color(0xFF007683), tertiary = Color(0xFF7A2F8F),
            background = Color(0xFFF7F9FF), onBackground = Color(0xFF171B26),
            surface = Color.White, onSurface = Color(0xFF171B26),
            surfaceVariant = Color(0xFFE2E7F5), onSurfaceVariant = Color(0xFF424754),
            outline = Color(0xFF727784), error = Color(0xFFBA1A1A)),
        visuals(Color(0xFF007683), Color(0xFF8B5800), Color(0xFF7A2F8F),
            listOf(Color(0xFF3047B8), Color(0xFF007683), Color(0xFF7A2F8F), Color(0xFF9A4B00)),
            Color.White, Color(0x24171B26),
            Triple(Color(0xFFE2E7F5), Color(0xFF7186C8), Color(0xFF3047B8))),
        TechTypography, OledShapes,
    )
    AppThemeId.OLED to true -> definition(
        darkColorScheme(
            primary = Color(0xFF4DE7FF), onPrimary = Color(0xFF00363D),
            secondary = Color(0xFF9E8CFF), tertiary = Color(0xFFFF4FD8),
            background = Color.Black, onBackground = Color(0xFFEAF8FF),
            surface = Color(0xFF080A0F), onSurface = Color(0xFFEAF8FF),
            surfaceVariant = Color(0xFF171B25), onSurfaceVariant = Color(0xFFC5CEDD),
            outline = Color(0xFF526070), error = Color(0xFFFF6B7A)),
        visuals(Color(0xFF9E8CFF), Color(0xFFFFC857), Color(0xFFFF4FD8),
            listOf(Color(0xFF4DE7FF), Color(0xFF9E8CFF), Color(0xFFFF4FD8), Color(0xFFFFC857)),
            Color.Black, Color(0x3350E6FF),
            Triple(Color(0xFF10131A), Color(0xFF087A8C), Color(0xFF4DE7FF))),
        TechTypography, OledShapes,
    )
    AppThemeId.PURE to false -> definition(
        lightColorScheme(
            primary = Color(0xFF4E6175), onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE8F5), onPrimaryContainer = Color(0xFF0A1D2C),
            secondary = Color(0xFF756B36), secondaryContainer = Color(0xFFF3E9B4),
            tertiary = Color(0xFF5E6470),
            background = Color(0xFFFAF9F6), onBackground = Color(0xFF1B1C1D),
            surface = Color.White, onSurface = Color(0xFF1B1C1D),
            surfaceVariant = Color(0xFFEDEFF1), onSurfaceVariant = Color(0xFF45474A),
            outline = Color(0xFF77797C), error = Color(0xFFBA1A1A)),
        visuals(Color(0xFF756B36), Color(0xFFA2542D), Color(0xFF8A5060),
            listOf(Color(0xFF4E6175), Color(0xFF756B36), Color(0xFF8A5060), Color(0xFFA2542D)),
            Color.White, Color(0x181B1C1D),
            Triple(Color(0xFFEDEFF1), Color(0xFFA9BBCB), Color(0xFF4E6175))),
        PureTypography, PureShapes,
    )
    AppThemeId.PURE to true -> definition(
        darkColorScheme(
            primary = Color(0xFFAED7F4), onPrimary = Color(0xFF173449),
            primaryContainer = Color(0xFF273C4C), onPrimaryContainer = Color(0xFFD3ECFF),
            secondary = Color(0xFFF1E28D), secondaryContainer = Color(0xFF4E481D),
            tertiary = Color(0xFFD8C6E2),
            background = Color(0xFF08090A), onBackground = Color(0xFFE7E8E9),
            surface = Color(0xFF111315), onSurface = Color(0xFFE7E8E9),
            surfaceVariant = Color(0xFF202326), onSurfaceVariant = Color(0xFFC4C7CA),
            outline = Color(0xFF606468), error = Color(0xFFFFB4AB)),
        visuals(Color(0xFFF1E28D), Color(0xFFFFB38A), Color(0xFFD8C6E2),
            listOf(Color(0xFFAED7F4), Color(0xFFF1E28D), Color(0xFFD8C6E2), Color(0xFFFFB38A)),
            Color(0xFF0B0C0D), Color(0x24E7E8E9),
            Triple(Color(0xFF202326), Color(0xFF657E91), Color(0xFFAED7F4))),
        PureTypography, PureShapes,
    )
    else -> error("Unsupported theme combination")
}

private val LocalAppVisuals = staticCompositionLocalOf {
    themeDefinition(AppThemeId.ORIGINAL, true).visuals
}

val appVisuals: AppVisuals
    @Composable get() = LocalAppVisuals.current

@Composable
fun SuiviMuscuTheme(
    appearance: AppearancePreferences = AppearancePreferences(),
    content: @Composable () -> Unit,
) {
    val dark = when (appearance.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val definition = themeDefinition(appearance.theme, dark)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = definition.colors.background.toArgb()
            window.navigationBarColor = definition.colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalAppVisuals provides definition.visuals) {
        MaterialTheme(
            colorScheme = definition.colors,
            typography = definition.typography,
            shapes = definition.shapes,
            content = content,
        )
    }
}

@Composable
fun Modifier.appBackground(): Modifier {
    val visuals = appVisuals
    return if (visuals.showKawaiiDecorations && visuals.kawaiiBackdrop.isNotEmpty()) {
        background(Brush.verticalGradient(visuals.kawaiiBackdrop))
    } else {
        background(MaterialTheme.colorScheme.background)
    }
}

@Composable
fun kawaiiContainer(index: Int, fallback: Color): Color {
    val colors = appVisuals.kawaiiSurfaces
    return if (appVisuals.showKawaiiDecorations && colors.isNotEmpty()) {
        colors[Math.floorMod(index, colors.size)]
    } else {
        fallback
    }
}

fun kawaiiMascot(index: Int): String = listOf("🐰", "🐼", "🐱")[Math.floorMod(index, 3)]

@Composable
fun KawaiiCardMascot(emoji: String, modifier: Modifier = Modifier) {
    if (!appVisuals.showKawaiiDecorations) return
    Surface(
        modifier = modifier.size(32.dp),
        color = kawaiiContainer(emoji.hashCode(), MaterialTheme.colorScheme.primaryContainer),
        shape = CircleShape,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (emoji == "🐱") {
                SiameseCatIcon(Modifier.size(23.dp).clearAndSetSemantics { })
            } else {
                Text(emoji, fontSize = 18.sp, modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }
}

@Composable
fun KawaiiNavigationIcon(emoji: String, fallback: ImageVector) {
    if (appVisuals.showKawaiiDecorations) {
        if (emoji == "🐱") {
            SiameseCatIcon(Modifier.size(25.dp).clearAndSetSemantics { })
        } else {
            Text(emoji, fontSize = 21.sp, modifier = Modifier.clearAndSetSemantics { })
        }
    } else {
        Icon(fallback, null)
    }
}

@Composable
private fun SiameseCatIcon(modifier: Modifier = Modifier) {
    val cream = Color(0xFFFFE9C9)
    val mask = Color(0xFF63433F)
    val eyes = Color(0xFF77C7F2)
    Canvas(modifier) {
        val side = size.minDimension
        val center = Offset(size.width / 2f, size.height * .55f)
        val leftEar = Path().apply {
            moveTo(size.width * .20f, size.height * .38f)
            lineTo(size.width * .27f, size.height * .02f)
            lineTo(size.width * .46f, size.height * .28f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(size.width * .54f, size.height * .28f)
            lineTo(size.width * .73f, size.height * .02f)
            lineTo(size.width * .80f, size.height * .38f)
            close()
        }
        drawPath(leftEar, mask)
        drawPath(rightEar, mask)
        drawCircle(cream, radius = side * .36f, center = center)
        drawOval(
            color = mask,
            topLeft = Offset(size.width * .29f, size.height * .30f),
            size = Size(size.width * .42f, size.height * .50f),
        )
        drawCircle(eyes, radius = side * .055f, center = Offset(size.width * .40f, size.height * .48f))
        drawCircle(eyes, radius = side * .055f, center = Offset(size.width * .60f, size.height * .48f))
        drawCircle(Color(0xFF2B1D25), radius = side * .025f, center = Offset(size.width * .40f, size.height * .48f))
        drawCircle(Color(0xFF2B1D25), radius = side * .025f, center = Offset(size.width * .60f, size.height * .48f))
        drawCircle(Color(0xFFF2A0B7), radius = side * .035f, center = Offset(size.width * .50f, size.height * .63f))
    }
}

@Composable
fun KawaiiHeaderDecoration(emoji: String = "🐰") {
    if (!appVisuals.showKawaiiDecorations) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        KawaiiCardMascot(emoji, Modifier.size(27.dp))
        Icon(Icons.Default.Favorite, null, tint = appVisuals.decorationPrimary, modifier = Modifier.size(15.dp))
        Icon(Icons.Default.AutoAwesome, null, tint = appVisuals.decorationSecondary, modifier = Modifier.size(17.dp))
    }
}
