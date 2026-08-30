package fr.suivimuscu.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.MainViewModel
import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.BodyWeightTrendPoint
import fr.suivimuscu.app.normalizedWeightKg
import fr.suivimuscu.app.previousBodyWeightEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightScreen(
    state: AppState,
    viewModel: MainViewModel,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var weeks by remember { mutableStateOf<Int?>(12) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val existing = state.bodyWeights.firstOrNull { it.date == selectedDate.toString() }
    val previous = previousBodyWeightEntry(state.bodyWeights, selectedDate)
    var weightText by remember(selectedDate, existing?.updatedAt, previous?.updatedAt) {
        mutableStateOf((existing?.weightKg ?: previous?.weightKg)?.let(::formatWeightInput).orEmpty())
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date.coerceAtMost(today)
        feedback = null
        focusManager.clearFocus()
    }

    fun adjust(delta: Double) {
        val current = normalizedWeightKg(weightText) ?: return
        weightText = formatWeightInput((current + delta).coerceIn(0.1, 500.0))
        feedback = null
    }

    fun save() {
        val result = viewModel.saveBodyWeight(selectedDate.toString(), weightText)
        feedback = if (result.isSuccess) "Mesure enregistrée" else result.exceptionOrNull()?.message
        focusManager.clearFocus()
    }

    val trend = viewModel.bodyWeightTrend(weeks)
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Poids", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Une mesure quand tu veux, sans jour obligatoire.", color = Muted)
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, "Exporter les pesées en CSV")
                }
            }
        }
        item {
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectDate(selectedDate.minusDays(1)) }) {
                            Icon(Icons.Default.ChevronLeft, "Jour précédent")
                        }
                        OutlinedButton(
                            onClick = { showDateDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.CalendarMonth, null)
                            Spacer(Modifier.width(7.dp))
                            Text(formatDisplayDate(selectedDate))
                        }
                        IconButton(
                            enabled = selectedDate.isBefore(today),
                            onClick = { selectDate(selectedDate.plusDays(1)) },
                        ) { Icon(Icons.Default.ChevronRight, "Jour suivant") }
                    }
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { value ->
                            if (value.matches(Regex("""\d{0,3}([,.]\d?)?"""))) {
                                weightText = value
                                feedback = null
                            }
                        },
                        label = { Text("Poids") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        keyboardActions = KeyboardActions(onDone = { save() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        when {
                            existing != null -> "Mesure déjà enregistrée pour cette date"
                            previous != null -> "Prérempli depuis le ${formatShortDate(LocalDate.parse(previous.date))}"
                            else -> "Saisis ta première mesure"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        WeightStepButton("+1", normalizedWeightKg(weightText) != null, Modifier.weight(1f)) { adjust(1.0) }
                        WeightStepButton("+0,5", normalizedWeightKg(weightText) != null, Modifier.weight(1f)) { adjust(0.5) }
                        WeightStepButton("+0,1", normalizedWeightKg(weightText) != null, Modifier.weight(1f)) { adjust(0.1) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        WeightStepButton("−1", normalizedWeightKg(weightText) != null, Modifier.weight(1f)) { adjust(-1.0) }
                        WeightStepButton("−0,5", normalizedWeightKg(weightText) != null, Modifier.weight(1f)) { adjust(-0.5) }
                        WeightStepButton("−0,1", normalizedWeightKg(weightText) != null, Modifier.weight(1f)) { adjust(-0.1) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (existing != null) {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Supprimer")
                            }
                        }
                        Button(
                            enabled = normalizedWeightKg(weightText) != null,
                            onClick = { save() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (existing == null) "Enregistrer" else "Mettre à jour")
                        }
                    }
                    feedback?.let {
                        Text(
                            it,
                            color = if (it.startsWith("Mesure ")) Lime else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Text("Évolution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Mesures brutes et moyenne des mesures disponibles sur 7 jours.", color = Muted)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(4, 12, 52, null)) { value ->
                    FilterChip(
                        selected = weeks == value,
                        onClick = { weeks = value },
                        label = { Text(value?.let { "$it sem." } ?: "Tout") },
                    )
                }
            }
        }
        item { BodyWeightChart(trend) }
        if (state.bodyWeights.isNotEmpty()) {
            item { Text("Dernières mesures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(
                state.bodyWeights.sortedByDescending { it.date }.take(12),
                key = { it.id },
            ) { entry ->
                ListItem(
                    headlineContent = { Text("${formatWeightInput(entry.weightKg)} kg", fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(runCatching { formatDisplayDate(LocalDate.parse(entry.date)) }.getOrDefault(entry.date))
                    },
                    trailingContent = { Icon(Icons.Default.Edit, "Modifier") },
                    modifier = Modifier.clickable {
                        runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let(::selectDate)
                    },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showDateDialog) {
        var dateText by remember(selectedDate) { mutableStateOf(selectedDate.toString()) }
        val parsed = runCatching { LocalDate.parse(dateText) }.getOrNull()
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("Date de la mesure") },
            text = {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("AAAA-MM-JJ") },
                    supportingText = {
                        if (parsed == null || parsed.isAfter(today)) Text("Entre une date valide, au plus tard aujourd’hui")
                    },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = parsed != null && !parsed.isAfter(today),
                    onClick = {
                        parsed?.let(::selectDate)
                        showDateDialog = false
                    },
                ) { Text("Choisir") }
            },
            dismissButton = { TextButton(onClick = { showDateDialog = false }) { Text("Annuler") } },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette mesure ?") },
            text = { Text("${formatDisplayDate(selectedDate)} • ${existing?.weightKg?.let(::formatWeightInput)} kg") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBodyWeight(selectedDate.toString())
                    showDeleteDialog = false
                    feedback = "Mesure supprimée"
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun WeightStepButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { Text(label) }
}

@Composable
private fun BodyWeightChart(points: List<BodyWeightTrendPoint>) {
    if (points.isEmpty()) {
        EmptyChart("Enregistre une mesure pour commencer le graphique")
        return
    }
    var selectedTimestamp by remember(points) { mutableLongStateOf(points.last().timestamp) }
    val selected = points.minBy { abs(it.timestamp - selectedTimestamp) }
    val minX = points.minOf { it.timestamp }
    val maxX = points.maxOf { it.timestamp }
    val range = chartYRange(points.flatMap { listOf(it.weightKg.toFloat(), it.average7DaysKg.toFloat()) })

    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(230.dp)) {
                Column(
                    Modifier.width(52.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(formatAxisWeight(range.max), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Text(formatAxisWeight((range.min + range.max) / 2f), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Text(formatAxisWeight(range.min), style = MaterialTheme.typography.labelSmall, color = Muted)
                }
                Spacer(Modifier.width(7.dp))
                Canvas(
                    Modifier.weight(1f).fillMaxHeight()
                        .background(AppSurface)
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (minX == maxX) selectedTimestamp = minX
                                else {
                                    val ratio = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val target = minX + ((maxX - minX) * ratio).toLong()
                                    selectedTimestamp = points.minBy { abs(it.timestamp - target) }.timestamp
                                }
                            }
                        },
                ) {
                    val left = 10.dp.toPx()
                    val right = size.width - 10.dp.toPx()
                    val top = 12.dp.toPx()
                    val bottom = size.height - 14.dp.toPx()
                    fun x(timestamp: Long) = left + chartXFraction(timestamp, minX, maxX) * (right - left)
                    fun y(weight: Double) = bottom -
                        (weight.toFloat() - range.min) / (range.max - range.min).coerceAtLeast(1f) * (bottom - top)

                    repeat(4) { index ->
                        val lineY = top + index * (bottom - top) / 3f
                        drawLine(Color.White.copy(alpha = .08f), Offset(left, lineY), Offset(right, lineY), 1f)
                    }
                    fun drawSeries(selector: (BodyWeightTrendPoint) -> Double, color: Color, width: Float) {
                        if (points.size > 1) {
                            val path = Path()
                            points.forEachIndexed { index, point ->
                                val offset = Offset(x(point.timestamp), y(selector(point)))
                                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                            }
                            drawPath(path, color, style = Stroke(width))
                        }
                    }
                    drawSeries({ it.weightKg }, Lime, 2.dp.toPx())
                    drawSeries({ it.average7DaysKg }, Cyan, 3.dp.toPx())
                    points.forEach { point ->
                        drawCircle(Lime, 3.5.dp.toPx(), Offset(x(point.timestamp), y(point.weightKg)))
                    }
                    drawCircle(
                        Cyan,
                        6.dp.toPx(),
                        Offset(x(selected.timestamp), y(selected.average7DaysKg)),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 59.dp),
                horizontalArrangement = if (minX == maxX) Arrangement.Center else Arrangement.SpaceBetween,
            ) {
                Text(formatTrendDate(points.first().date), style = MaterialTheme.typography.labelSmall, color = Muted)
                if (minX != maxX) Text(formatTrendDate(points.last().date), style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                LegendDot(Lime, "Mesures brutes")
                Spacer(Modifier.width(18.dp))
                LegendDot(Cyan, "Moyenne 7 jours")
            }
            Surface(color = Lime.copy(alpha = .08f), shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(formatDisplayDate(LocalDate.parse(selected.date)), fontWeight = FontWeight.Bold)
                        Text("Mesure : ${formatWeightInput(selected.weightKg)} kg", color = Lime)
                    }
                    Text("Moy. 7 j\n${formatWeightInput(selected.average7DaysKg)} kg", color = Cyan)
                }
            }
            Text("Touche le graphique pour inspecter une date.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

private fun formatWeightInput(value: Double): String =
    String.format(Locale.FRANCE, "%.1f", value)

private fun formatAxisWeight(value: Float): String =
    String.format(Locale.FRANCE, "%.1f", value)

private fun formatDisplayDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.FRANCE))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }

private fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE))

private fun formatTrendDate(date: String): String =
    runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yy")) }.getOrDefault(date)
