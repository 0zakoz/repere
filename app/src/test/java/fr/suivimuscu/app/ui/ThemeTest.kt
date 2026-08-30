package fr.suivimuscu.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import fr.suivimuscu.app.data.AppThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ThemeTest {
    @Test
    fun everyThemeHasReadableCoreTextAndControls() {
        AppThemeId.entries.forEach { theme ->
            listOf(false, true).forEach { dark ->
                val colors = themeDefinition(theme, dark).colors
                assertTrue("$theme/$dark background", contrast(colors.onBackground, colors.background) >= 4.5)
                assertTrue("$theme/$dark surface", contrast(colors.onSurface, colors.surface) >= 4.5)
                assertTrue("$theme/$dark primary", contrast(colors.onPrimary, colors.primary) >= 4.5)
                assertTrue("$theme/$dark error", contrast(colors.onError, colors.error) >= 4.5)
            }
        }
    }

    @Test
    fun chartSeriesStayDistinctAndHeatmapsHaveRange() {
        AppThemeId.entries.forEach { theme ->
            listOf(false, true).forEach { dark ->
                val visuals = themeDefinition(theme, dark).visuals
                assertEquals(4, visuals.chartSeries.size)
                visuals.chartSeries.forEachIndexed { index, color ->
                    visuals.chartSeries.drop(index + 1).forEachIndexed { offset, other ->
                        assertTrue(
                            "$theme/$dark chart colors $index/${index + offset + 1}",
                            colorDistance(color, other) >= 0.18,
                        )
                    }
                }
                assertTrue(
                    "$theme/$dark heatmap",
                    abs(visuals.heatmapHigh.luminance() - visuals.heatmapLow.luminance()) >= 0.20f,
                )
            }
        }
    }

    @Test
    fun originalDarkKeepsHistoricalPalette() {
        val definition = themeDefinition(AppThemeId.ORIGINAL, true)
        assertEquals(Color(0xFF0F1115), definition.colors.background)
        assertEquals(Color(0xFF191C22), definition.colors.surface)
        assertEquals(Color(0xFFB7F34A), definition.colors.primary)
        assertEquals(Color(0xFF56D6E7), definition.colors.secondary)
        assertEquals(Color(0xFFB89CFF), definition.colors.tertiary)
    }

    private fun contrast(first: Color, second: Color): Double {
        val high = max(first.luminance(), second.luminance()).toDouble()
        val low = min(first.luminance(), second.luminance()).toDouble()
        return (high + 0.05) / (low + 0.05)
    }

    private fun colorDistance(first: Color, second: Color): Double = sqrt(
        (first.red - second.red).let { it * it } +
            (first.green - second.green).let { it * it } +
            (first.blue - second.blue).let { it * it },
    ).toDouble()
}
