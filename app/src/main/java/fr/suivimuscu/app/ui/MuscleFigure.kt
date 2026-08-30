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

private data class RegionDef(val muscleId: String, val pathData: String)

private val baseShape = listOf(
    // tête + cou (grande)
    "M0,8 C40,8 66,34 67,70 C68,104 56,128 34,140 C40,148 43,158 43,166 L44,186 L0,186 Z",
    // torse (V-taper fin)
    "M0,184 C55,186 130,194 195,208 C235,216 272,228 286,252 C294,272 288,292 268,302 " +
        "C248,312 230,318 218,330 C200,348 184,384 168,420 C154,450 122,468 108,484 " +
        "C104,506 112,526 126,540 C106,556 78,566 50,572 C30,576 14,578 8,580 L0,580 Z",
    // bras droit (long, vertical, proche du torse)
    "M212,230 C242,220 270,228 282,252 C290,274 294,322 296,376 C298,430 300,490 301,540 " +
        "C302,566 302,590 300,610 C298,630 290,640 281,636 C273,631 268,614 265,594 " +
        "C261,560 256,504 252,452 C248,404 242,350 236,314 C231,292 225,268 220,256 " +
        "C215,246 212,238 212,230 Z",
    // jambe droite (fine)
    "M8,578 C40,576 78,568 118,556 C132,590 138,630 134,672 C130,708 122,740 112,768 " +
        "C108,792 108,816 102,844 C96,880 86,916 78,944 C96,952 110,968 114,990 " +
        "L36,990 C40,970 50,954 66,944 C62,908 58,866 56,830 C50,798 42,772 36,752 " +
        "C24,712 12,654 8,604 C7,594 7,586 8,578 Z",
)

// Toutes les régions sont disjointes (frontières nettes) pour une sélection fiable.

private val frontRegions = listOf(
    RegionDef("front_delts", "M198,218 C226,222 246,236 250,256 C253,274 249,290 238,298 C226,304 213,299 206,285 C199,268 196,244 197,228 C197,222 198,220 198,218 Z"),
    RegionDef("side_delts", "M252,246 C268,254 280,268 284,282 C286,294 280,302 270,304 C262,306 255,300 252,290 C250,276 250,260 251,252 C251,248 252,246 252,246 Z"),
    RegionDef("upper_pecs", "M8,198 C55,204 120,214 182,230 C180,244 170,252 152,254 C102,244 48,230 8,218 Z"),
    RegionDef("pecs", "M8,258 C55,268 120,282 180,296 C190,302 193,312 189,326 C183,346 164,356 136,360 C90,362 46,352 8,336 C6,310 6,282 8,258 Z"),
    RegionDef("abs", "M8,364 C36,370 70,374 98,376 C104,404 106,436 102,464 C98,492 88,512 74,526 C50,532 28,530 8,524 C6,470 6,416 8,364 Z"),
    RegionDef("biceps", "M234,306 C256,312 274,330 282,356 C288,382 290,408 288,428 C274,434 258,426 250,408 C240,380 234,340 232,318 C232,310 233,308 234,306 Z"),
    RegionDef("forearm_flexors", "M256,452 C270,460 281,478 286,498 C291,520 293,542 291,562 C280,566 271,556 266,541 C259,517 254,488 253,468 C253,458 254,454 256,452 Z"),
    RegionDef("adductors", "M8,584 C18,582 28,580 40,578 C42,620 41,664 36,702 C32,726 26,742 18,752 C13,740 9,724 8,706 C6,664 6,622 8,584 Z"),
    RegionDef("quads", "M42,590 C68,586 98,576 128,562 C140,598 144,638 140,678 C136,708 127,734 115,756 C90,764 64,762 44,754 C41,712 40,664 40,620 C40,610 40,598 42,590 Z"),
)

private val frontLines = listOf(
    "M6,262 L6,330",                                   // seam pectoraux
    "M8,368 L8,522",                                   // ligne verticale abdos
    "M8,400 C34,404 66,406 98,404",                    // abdos horizontales
    "M8,434 C36,438 68,440 100,438",
    "M8,468 C34,472 66,474 98,472",
    "M8,498 C32,502 60,504 84,504",
    "M104,380 C112,420 116,460 108,498",               // obliques (non suivi)
    "M14,190 C52,194 104,202 154,214",                 // clavicule
    "M36,768 C64,772 90,770 112,762",                  // genou
    "M78,800 C74,850 70,898 68,940",                   // tibia
    "M240,414 C254,420 268,418 282,410",               // coude
    "M272,600 C280,604 288,602 296,596",               // main
    "M36,950 C62,954 88,950 110,942",                  // pied
)

private val backRegions = listOf(
    RegionDef("traps", "M8,186 C60,188 130,198 190,214 C170,244 140,272 105,292 C75,308 45,318 18,322 C14,300 10,280 8,262 C6,236 6,210 8,186 Z"),
    RegionDef("rear_delts", "M204,224 C236,228 266,242 280,262 C286,276 280,290 266,298 C252,304 236,300 226,288 C214,272 206,248 203,232 C203,227 203,225 204,224 Z"),
    RegionDef("lats", "M8,330 C44,338 88,352 126,370 C152,382 168,400 172,420 C173,440 168,458 157,472 C144,486 126,492 104,492 C70,480 34,458 8,434 C6,400 6,364 8,330 Z"),
    RegionDef("lower_back", "M8,496 C28,502 48,510 66,520 C76,534 80,546 78,556 C58,564 34,566 8,564 C6,540 6,518 8,496 Z"),
    RegionDef("triceps", "M236,310 C258,316 276,334 284,360 C290,386 292,412 290,432 C276,438 260,430 252,412 C242,384 236,344 234,322 C234,314 235,312 236,310 Z"),
    RegionDef("forearm_extensors", "M256,456 C270,464 281,482 286,502 C291,524 293,546 291,566 C280,570 271,560 266,545 C259,521 254,492 253,472 C253,462 254,458 256,456 Z"),
    RegionDef("glutes", "M8,562 C36,556 68,556 98,564 C128,572 150,586 158,606 C164,624 156,640 140,650 C112,660 80,662 52,656 C34,652 18,644 8,632 C6,608 6,584 8,562 Z"),
    RegionDef("hamstrings", "M16,656 C52,652 94,644 132,630 C146,662 152,696 148,726 C144,750 134,770 122,784 C90,790 56,788 26,780 C18,742 13,700 12,662 C12,658 13,656 16,656 Z"),
    RegionDef("calves", "M40,796 C62,788 84,792 98,806 C108,826 110,854 104,882 C98,910 90,932 78,948 C62,950 50,942 44,928 C37,896 35,860 36,828 C37,816 38,804 40,796 Z"),
)

private val backLines = listOf(
    "M6,196 C10,310 10,440 6,556",                     // colonne
    "M36,768 C64,772 90,770 112,762",                  // genou
    "M72,906 C76,924 78,936 78,946",                   // tendon d'Achille
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

    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Carte musculaire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Touche une zone pour le détail",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
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
                val baseFill = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)
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
                    val lineWidth = 1.1.dp.toPx() / ls.first().scale
                    val selectionWidth = 2.2.dp.toPx() / ls.first().scale
                    val outlineColor = Color.White.copy(alpha = .30f)

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
                                    drawPath(path, heatmapColor(fractionOf(id).toDouble()))
                                    drawPath(path, outlineColor, style = Stroke(lineWidth))
                                }
                                figure.lines.forEach { path -> drawPath(path, outlineColor, style = Stroke(lineWidth)) }
                                figure.regions.forEach { (id, path) ->
                                    if (id == selectedId) drawPath(path, Lime, style = Stroke(selectionWidth))
                                }
                            }
                        }
                    }

                    drawFigure(ls[0], figures.first)
                    drawFigure(ls[1], figures.second)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Avant", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = Muted)
                Text("Dos", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = Muted)
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
                                listOf(heatmapColor(0.0), heatmapColor(0.5), heatmapColor(1.0)),
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
                        drawPath(tri, Lime)
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Faible", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.weight(1f))
                Text("Élevé", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            if (selectedName != null && selectedKey != null && selectedFraction != null && selectedFraction > 0f) {
                val metric = stats.getValue(selectedKey)
                Surface(color = Lime.copy(alpha = .08f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
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
                Text("$selectedName : aucune série pondérée sur la période.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}
