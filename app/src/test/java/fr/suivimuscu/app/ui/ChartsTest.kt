package fr.suivimuscu.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartsTest {
    @Test
    fun oneWeightValueIsCenteredVerticallyAndHorizontally() {
        val range = chartYRange(listOf(84.5f))

        assertEquals(84.5f, (range.min + range.max) / 2f, 0.001f)
        assertEquals(.5f, chartXFraction(100, 100, 100), 0.001f)
    }

    @Test
    fun repetitionRangeContainsTargetBandAndObservedValuesWithMargins() {
        val range = chartYRange(listOf(11f, 12f), 6f to 10f)

        assertTrue(range.min < 6f)
        assertTrue(range.max > 12f)
    }

    @Test
    fun heatmapColorCoversFullRangeWithStrongContrast() {
        assertEquals(heatmapColor(0.0), heatmapColor(-1.0))
        assertEquals(heatmapColor(1.0), heatmapColor(1.5))

        val base = heatmapColor(0.0)
        val mid = heatmapColor(0.5)
        val max = heatmapColor(1.0)

        assertTrue(base.luminance() < mid.luminance())
        assertTrue(mid.luminance() < max.luminance())
        assertTrue(max.luminance() - base.luminance() > 0.3f)
    }

    @Test
    fun heatmapFractionIsZeroForEmptyValuesAndClampedToOne() {
        assertEquals(0f, heatmapFraction(0.0, 10.0), 0.001f)
        assertEquals(0f, heatmapFraction(5.0, 0.0), 0.001f)
        assertEquals(0.25f, heatmapFraction(2.5, 10.0), 0.001f)
        assertEquals(1f, heatmapFraction(15.0, 10.0), 0.001f)
    }

    @Test
    fun figureRegionColorSpansBaseToHeat() {
        val base = Color(0xFF222222)
        val heat = Color(0xFFEEEEEE)

        assertEquals(base, figureRegionColor(base, heat, 0f))
        assertEquals(heat, figureRegionColor(base, heat, 1f))
        assertEquals(base, figureRegionColor(base, heat, -0.5f))
        assertEquals(heat, figureRegionColor(base, heat, 2f))

        val mid = figureRegionColor(base, heat, 0.5f)
        assertTrue(mid.luminance() > base.luminance())
        assertTrue(heat.luminance() > mid.luminance())
    }
}
