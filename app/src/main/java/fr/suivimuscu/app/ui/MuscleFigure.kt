package fr.suivimuscu.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import fr.suivimuscu.app.data.MuscleGroup
import fr.suivimuscu.app.data.MusclePeriodStats
import kotlin.math.abs
import kotlin.math.min

// Coordonnées auteurées pour la moitié droite du corps (x >= 0, axe central = 0),
// y dans [0..1000] (tête -> pieds). Miroir gauche appliqué au rendu.
// Silhouette fine : épaules ±290, taille ±110, bras proches du torse, tête proéminente.

private const val AUTHOR_HEIGHT = 1000f

internal data class RegionDef(val muscleId: String, val pathData: String)

private val baseShape = listOf(
    "M0,14 C32,14 56,38 56,72 C56,104 40,128 20,140 L24,184 L0,184 Z",
    "M0,182 C54,184 122,192 180,204 C220,212 250,224 261,244 C269,260 263,274 249,282 C231,292 213,300 201,314 C187,332 177,366 167,402 C157,436 129,456 115,474 C110,496 118,516 132,530 C112,546 84,556 58,562 C38,566 18,569 8,571 L0,571 Z",
    "M220,226 C252,218 278,228 288,252 C296,274 299,328 299,384 C299,440 297,498 292,544 C289,572 281,588 270,585 C261,581 256,564 254,544 C250,508 246,454 242,404 C238,356 232,310 226,282 C222,260 220,240 220,226 Z",
    "M8,567 C48,565 94,557 134,545 C146,581 151,623 147,665 C143,701 135,733 125,759 C121,785 121,809 116,835 C110,871 100,909 92,937 C108,945 120,961 124,983 L46,983 C50,963 60,947 76,937 C72,901 68,859 66,823 C60,791 52,765 46,745 C34,705 22,647 18,597 C17,585 17,575 18,567 Z",
)

// Toutes les régions sont disjointes (frontières nettes) pour une sélection fiable.

internal val frontRegions = listOf(
    RegionDef("upper_pecs", "M12,198 C62,204 118,212 166,226 L162,242 C112,230 60,220 12,212 Z"),
    RegionDef("pecs", "M12,244 C60,254 114,266 158,279 C172,285 176,296 171,310 C163,332 145,344 119,348 C81,352 42,342 12,326 L12,244 Z"),
    RegionDef("front_delts", "M180,208 C208,208 230,222 236,245 C240,265 234,281 222,287 C210,291 199,283 194,267 C189,249 187,228 188,216 Z"),
    RegionDef("side_delts", "M240,236 C258,242 272,256 276,272 C279,286 272,296 261,298 C252,299 245,292 243,280 C241,266 240,250 241,242 Z"),
    RegionDef("abs", "M12,358 C40,364 70,368 94,370 C100,398 102,430 98,458 C94,486 84,506 70,520 C46,526 26,524 12,518 C10,464 10,410 12,358 Z"),
    RegionDef("biceps", "M230,300 C250,306 266,322 273,346 C279,372 281,398 279,416 C266,422 252,414 245,398 C236,372 231,336 229,314 C229,306 229,302 230,300 Z"),
    RegionDef("forearm_flexors", "M248,440 C262,448 273,466 278,486 C283,508 285,530 283,548 C272,552 263,542 258,528 C251,504 247,476 246,456 C246,446 247,442 248,440 Z"),
    RegionDef("adductors", "M12,573 C21,571 31,569 42,567 C44,609 43,653 38,691 C34,715 28,731 21,741 C16,729 12,713 11,695 C9,653 9,611 12,573 Z"),
    RegionDef("quads", "M48,579 C72,575 100,565 128,551 C140,587 144,627 140,667 C136,697 129,723 117,745 C92,753 68,751 48,743 C45,701 44,653 45,609 C45,599 46,587 48,579 Z"),
)

private val frontLines = listOf(
    "M16,192 C54,196 102,204 148,216",
    "M14,324 C58,336 106,344 148,344",
    "M12,360 L12,516",
    "M12,394 C38,398 66,400 92,398",
    "M12,428 C40,432 70,434 96,432",
    "M12,462 C38,466 68,468 94,466",
    "M98,374 C106,414 110,454 102,492",
    "M40,758 C66,762 92,760 112,752",
    "M80,790 C76,840 72,888 70,930",
    "M234,404 C248,410 262,408 276,400",
    "M260,566 C268,570 276,568 284,562",
    "M42,940 C64,944 88,940 108,932",
)

internal val backRegions = listOf(
    RegionDef("traps", "M12,182 C62,184 128,194 182,210 C164,240 136,266 104,286 C76,302 46,312 22,316 C18,294 14,274 12,256 C10,230 10,204 12,182 Z"),
    RegionDef("rear_delts", "M194,212 C224,216 252,230 266,250 C273,264 267,278 254,286 C241,292 226,288 217,276 C206,260 198,238 195,222 Z"),
    RegionDef("lats", "M12,324 C48,332 92,346 130,364 C156,376 172,394 176,414 C177,434 172,452 161,466 C148,480 130,486 108,484 C74,472 38,450 12,426 C10,392 10,358 12,324 Z"),
    RegionDef("lower_back", "M12,490 C32,496 52,504 70,514 C80,528 84,540 82,550 C62,558 38,560 12,558 C10,534 10,512 12,490 Z"),
    RegionDef("triceps", "M232,304 C252,310 268,326 275,350 C281,376 283,402 281,420 C268,426 254,418 247,402 C238,376 233,338 231,316 C231,308 231,306 232,304 Z"),
    RegionDef("forearm_extensors", "M248,444 C262,452 273,470 278,490 C283,512 285,534 283,552 C272,556 263,546 258,532 C251,508 247,480 246,460 C246,450 247,446 248,446 Z"),
    RegionDef("glutes", "M12,556 C40,550 72,550 100,558 C126,566 142,580 148,598 C152,612 146,624 134,631 C106,640 76,642 50,636 C32,632 20,624 12,612 C10,593 10,573 12,556 Z"),
    RegionDef("hamstrings", "M12,656 C52,652 94,648 128,636 C142,668 146,702 142,732 C138,756 130,776 120,788 C90,794 58,792 28,784 C20,746 15,704 14,666 C14,662 13,658 12,656 Z"),
    RegionDef("calves", "M44,796 C66,788 88,792 102,806 C112,826 114,854 108,882 C102,910 94,932 82,944 C66,946 54,938 48,924 C41,892 39,856 40,824 C41,812 42,804 44,796 Z"),
)

private val backLines = listOf(
    "M8,192 C12,306 12,432 8,552",
    "M38,354 C68,376 98,416 114,450",
    "M28,634 C66,644 112,644 142,632",
    "M40,758 C66,762 92,760 112,752",
    "M74,896 C78,914 80,926 80,936",
)

private class ParsedFigure(
    val base: List<Path>,
    val regions: List<Pair<String, Path>>,
    val lines: List<Path>,
    val hitRegions: List<Pair<String, android.graphics.Region>>,
)

private fun parsePath(data: String): Path =
    PathParser.createPathFromPathData(data).asComposePath()

private fun parseFigure(base: List<String>, regions: List<RegionDef>, lines: List<String>): ParsedFigure {
    val androidPaths = regions.map { PathParser.createPathFromPathData(it.pathData) }
    val hitRegions = regions.mapIndexed { index, def ->
        val clip = android.graphics.Region(-500, -100, 500, 1100)
        def.muscleId to android.graphics.Region().apply { setPath(androidPaths[index], clip) }
    }
    return ParsedFigure(
        base = base.map(::parsePath),
        regions = regions.map { it.muscleId to parsePath(it.pathData) },
        lines = lines.map(::parsePath),
        hitRegions = hitRegions,
    )
}

private data class FigureLayout(val scale: Float, val centerX: Float, val topY: Float)

private data class Size2(val width: Float, val height: Float)

@Composable
fun MuscleFigure(
    muscles: List<MuscleGroup>,
    stats: Map<String, MusclePeriodStats>,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val maxSets = stats.values.maxOfOrNull { it.weightedSets } ?: 0.0

    val figures = remember {
        parseFigure(baseShape, frontRegions, frontLines) to parseFigure(baseShape, backRegions, backLines)
    }

    fun fractionOf(muscleId: String): Float {
        val value = stats[muscleId]?.weightedSets ?: 0.0
        return heatmapFraction(value, maxSets)
    }
    val visuals = appVisuals

    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Carte musculaire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Touche une zone pour le détail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(234.dp)
                    .onSizeChanged { canvasSize = it },
            ) {
                val layouts = remember(canvasSize) {
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) null
                    else {
                        val w = canvasSize.width.toFloat()
                        val h = canvasSize.height.toFloat()
                        val scale = min(h / (AUTHOR_HEIGHT + 16f), (w * 0.44f) / 310f)
                        listOf(
                            FigureLayout(scale, w * 0.25f, (h - AUTHOR_HEIGHT * scale) / 2f),
                            FigureLayout(scale, w * 0.75f, (h - AUTHOR_HEIGHT * scale) / 2f),
                        )
                    }
                }
                val baseFill = MaterialTheme.colorScheme.surfaceVariant
                val outlineColor = MaterialTheme.colorScheme.outline
                val selectionColor = visuals.success
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(figures, layouts, maxSets) {
                            detectTapGestures { offset ->
                                val ls = layouts ?: return@detectTapGestures
                                selectedId = ls.firstNotNullOfOrNull { l ->
                                    val x = abs(offset.x - l.centerX) / l.scale
                                    val y = (offset.y - l.topY) / l.scale
                                    val figure = if (l.centerX < canvasSize.width / 2f) figures.first else figures.second
                                    figure.hitRegions
                                        .firstOrNull { (_, region) -> region.contains(x.toInt(), y.toInt()) }
                                        ?.first
                                }
                            }
                        },
                ) {
                    val ls = layouts ?: return@Canvas
                    val lineWidth = 1.7.dp.toPx() / ls.first().scale
                    val selectionWidth = 3.2.dp.toPx() / ls.first().scale
                    fun drawFigure(l: FigureLayout, figure: ParsedFigure) {
                        listOf(false, true).forEach { mirror ->
                            withTransform({
                                val sx = if (mirror) -l.scale else l.scale
                                translate(l.centerX, l.topY)
                                scale(sx, l.scale, pivot = Offset.Zero)
                            }) {
                                figure.base.forEach { path ->
                                    drawPath(path, baseFill)
                                    drawPath(path, outlineColor, style = Stroke(lineWidth))
                                }
                                figure.regions.forEach { (id, path) ->
                                    drawPath(path, heatmapColor(fractionOf(id).toDouble(), visuals.heatmapLow, visuals.heatmapMid, visuals.heatmapHigh))
                                    drawPath(path, outlineColor, style = Stroke(lineWidth))
                                }
                                figure.lines.forEach { path -> drawPath(path, outlineColor, style = Stroke(lineWidth)) }
                                figure.regions.forEach { (id, path) ->
                                    if (id == selectedId) drawPath(path, selectionColor, style = Stroke(selectionWidth))
                                }
                            }
                        }
                    }

                    drawFigure(ls[0], figures.first)
                    drawFigure(ls[1], figures.second)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Avant", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dos", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val selectedKey = selectedId
            val selectedFraction = selectedKey?.let(::fractionOf)
            val selectedName = muscles.firstOrNull { it.id == selectedKey }?.name
            BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp)) {
                val barWidth = maxWidth
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    heatmapColor(0.0, visuals.heatmapLow, visuals.heatmapMid, visuals.heatmapHigh),
                                    heatmapColor(0.5, visuals.heatmapLow, visuals.heatmapMid, visuals.heatmapHigh),
                                    heatmapColor(1.0, visuals.heatmapLow, visuals.heatmapMid, visuals.heatmapHigh),
                                ),
                            ),
                            RoundedCornerShape(4.dp),
                        ),
                )
                if (selectedFraction != null && selectedFraction > 0f) {
                    Canvas(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = barWidth * selectedFraction - 7.dp)
                            .size(14.dp, 11.dp),
                    ) {
                        val tri = Path().apply {
                            moveTo(7.dp.toPx(), 11.dp.toPx())
                            lineTo(0f, 0f)
                            lineTo(14.dp.toPx(), 0f)
                            close()
                        }
                        drawPath(tri, visuals.success)
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Faible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("Élevé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selectedName != null && selectedKey != null && selectedFraction != null && selectedFraction > 0f) {
                val metric = stats.getValue(selectedKey)
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .08f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        String.format(
                            java.util.Locale.FRANCE,
                            "%s — %.1f séries pond. • %s reps • RIR %s",
                            selectedName,
                            metric.weightedSets,
                            metric.averageReps?.let { String.format(java.util.Locale.FRANCE, "%.1f", it) } ?: "—",
                            metric.averageRir?.let { String.format(java.util.Locale.FRANCE, "%.1f", it) } ?: "—",
                        ),
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (selectedName != null) {
                Text("$selectedName : aucune série pondérée sur la période.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
