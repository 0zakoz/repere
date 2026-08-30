package fr.suivimuscu.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.data.ExerciseHistoryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.abs

internal data class ChartYRange(val min: Float, val max: Float)

internal fun chartYRange(
    values: List<Float>,
    band: Pair<Float, Float>? = null,
): ChartYRange {
    val low = minOf(values.minOrNull() ?: 0f, band?.first ?: Float.POSITIVE_INFINITY)
    val high = maxOf(values.maxOrNull() ?: 0f, band?.second ?: Float.NEGATIVE_INFINITY)
    val center = (low + high) / 2f
    val minimumHalfSpan = if (band == null) max(1f, abs(center) * .025f) else 2.5f
    val halfSpan = max(minimumHalfSpan, (high - low) * .7f)
    val min = (center - halfSpan).coerceAtLeast(0f)
    val max = if (min == 0f) maxOf(center + halfSpan, halfSpan * 2f) else center + halfSpan
    return ChartYRange(min, max)
}

internal fun chartXFraction(timestamp: Long, minTimestamp: Long, maxTimestamp: Long): Float =
    if (minTimestamp == maxTimestamp) .5f
    else ((timestamp - minTimestamp).toFloat() / (maxTimestamp - minTimestamp)).coerceIn(0f, 1f)

private val defaultHeatmapBase = Color(0xFF262B36)
private val defaultHeatmapMid = Color(0xFF6E7F2E)
private val defaultHeatmapMax = Color(0xFFB7F34A)

/** Couleur de la carte musculaire pour une intensité [0..1] : gris sombre -> olive -> lime vif, interpolation linéaire. */
internal fun heatmapColor(
    fraction: Double,
    low: Color = defaultHeatmapBase,
    mid: Color = defaultHeatmapMid,
    high: Color = defaultHeatmapMax,
): Color {
    val f = fraction.coerceIn(0.0, 1.0)
    return if (f < 0.5) {
        lerp(low, mid, (f / 0.5).toFloat())
    } else {
        lerp(mid, high, ((f - 0.5) / 0.5).toFloat())
    }
}

internal fun heatmapFraction(value: Double, maxValue: Double): Float =
    if (value <= 0.0 || maxValue <= 0.0) 0f else (value / maxValue).coerceIn(0.0, 1.0).toFloat()

@Composable
fun ExerciseCharts(points: List<ExerciseHistoryPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) {
        EmptyChart("Pas encore de données", modifier)
        return
    }
    var selectedTimestamp by remember(points) { mutableStateOf(points.last().timestamp) }
    val selectedPoints = points.filter { it.timestamp == selectedTimestamp }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Charge (kg)", fontWeight = FontWeight.SemiBold)
        MultiSeriesLineChart(
            values = points.map { Triple(it.timestamp, it.setOrder, it.weight) },
            modifier = Modifier.fillMaxWidth(),
            unit = "kg",
            onTimestampSelected = { selectedTimestamp = it },
        )
        Text("Répétitions", fontWeight = FontWeight.SemiBold)
        MultiSeriesLineChart(
            values = points.map { Triple(it.timestamp, it.setOrder, it.reps) },
            modifier = Modifier.fillMaxWidth(),
            unit = "reps",
            band = points.minOf { it.repMin }.toFloat() to points.maxOf { it.repMax }.toFloat(),
            onTimestampSelected = { selectedTimestamp = it },
        )
        if (selectedPoints.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .08f), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(formatChartDate(selectedPoints.first().timestamp), fontWeight = FontWeight.Bold)
                    selectedPoints.sortedBy { it.setOrder }.forEach { point ->
                        Text(
                            "Série ${point.setOrder} : ${point.weight} kg × ${point.reps.toInt()} reps" +
                                point.rir?.let { " • RIR $it" }.orEmpty() +
                                point.restSeconds?.let { " • repos ${it}s" }.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Text("${points.map { it.date }.distinct().size} séance(s) • touche un graphique pour inspecter la date la plus proche",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MultiSeriesLineChart(
    values: List<Triple<Long, Int, Float>>,
    modifier: Modifier,
    unit: String,
    band: Pair<Float, Float>? = null,
    onTimestampSelected: (Long) -> Unit,
) {
    val visuals = appVisuals
    val seriesColors = visuals.chartSeries
    val primary = MaterialTheme.colorScheme.primary
    val minX = values.minOf { it.first }
    val maxX = values.maxOf { it.first }
    val yRange = chartYRange(values.map { it.third }, band)
    val minY = yRange.min
    val maxY = yRange.max

    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth().height(176.dp), verticalAlignment = Alignment.Top) {
            Column(
                Modifier.width(54.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatAxis(maxY), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatAxis((minY + maxY) / 2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatAxis(minY), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(6.dp))
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(values) {
                        detectTapGestures { offset ->
                            if (minX == maxX) {
                                onTimestampSelected(minX)
                            } else {
                                val ratio = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                val target = minX + ((maxX - minX) * ratio).toLong()
                                onTimestampSelected(values.minBy { abs(it.first - target) }.first)
                            }
                        }
                    }
                    .background(visuals.chartBackground)
            ) {
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 10.dp.toPx()
                val bottom = size.height - 12.dp.toPx()
                fun x(value: Long) = left + chartXFraction(value, minX, maxX) * (right - left)
                fun y(value: Float) = bottom - (value - minY) / (maxY - minY).coerceAtLeast(1f) * (bottom - top)

                band?.let {
                    drawRect(
                        color = primary.copy(alpha = .08f),
                        topLeft = Offset(left, y(it.second)),
                        size = androidx.compose.ui.geometry.Size(right - left, y(it.first) - y(it.second)),
                    )
                }
                repeat(4) { index ->
                    val lineY = top + index * (bottom - top) / 3f
                    drawLine(visuals.chartGrid, Offset(left, lineY), Offset(right, lineY), 1f)
                }
                values.groupBy { it.second }.toSortedMap().forEach { (order, series) ->
                    val sorted = series.sortedBy { it.first }
                    val color = seriesColors[(order - 1).mod(seriesColors.size)]
                    if (sorted.size > 1) {
                        val path = Path()
                        sorted.forEachIndexed { index, point ->
                            if (index == 0) path.moveTo(x(point.first), y(point.third))
                            else path.lineTo(x(point.first), y(point.third))
                        }
                        drawPath(path, color, style = Stroke(2.5.dp.toPx()))
                    }
                    sorted.forEach { drawCircle(color, 4.dp.toPx(), Offset(x(it.first), y(it.third))) }
                }
            }
        }
        if (minX == maxX) {
            Text(
                formatShortDate(minX),
                modifier = Modifier.fillMaxWidth().padding(start = 60.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(start = 60.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatShortDate(minX), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatShortDate(maxX), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (band != null) {
            Text(
                "Zone cible : ${formatAxis(band.first)}–${formatAxis(band.second)} $unit",
                modifier = Modifier.fillMaxWidth().padding(start = 60.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 60.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            values.map { it.second }.distinct().sorted().forEach { order ->
                Row(
                    Modifier.padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(8.dp)
                            .background(seriesColors[(order - 1).mod(seriesColors.size)], CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Série $order", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun HorizontalBars(values: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    if (values.isEmpty() || values.all { it.second == 0.0 }) {
        EmptyChart("Pas encore de volume", modifier)
        return
    }
    val max = values.maxOf { it.second }.coerceAtLeast(1.0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        values.filter { it.second > 0 }.sortedByDescending { it.second }.forEach { (label, value) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                    Text(String.format(java.util.Locale.FRANCE, "%.1f", value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .18f))) {
                    Box(Modifier.fillMaxWidth((value / max).toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

@Composable
fun EmptyChart(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(130.dp).background(appVisuals.chartBackground), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatAxis(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString()
    else String.format(java.util.Locale.FRANCE, "%.1f", value)

private fun formatShortDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("dd/MM/yy")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

private fun formatChartDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))
