package fr.suivimuscu.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.MainViewModel
import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.NutritionDayTotal
import fr.suivimuscu.app.data.NutritionEntry
import fr.suivimuscu.app.normalizedCalories
import fr.suivimuscu.app.normalizedProteinGrams
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NutritionScreen(
    state: AppState,
    viewModel: MainViewModel,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var weeks by remember { mutableStateOf<Int?>(12) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<NutritionEntry?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val dayEntries = state.nutritionEntries
        .filter { it.date == selectedDate.toString() }
        .sortedByDescending { it.createdAt }
    val caloriesTotal = dayEntries.sumOf { it.caloriesKcal }
    val proteinTotal = dayEntries.sumOf { it.proteinGrams }

    fun clearForm() {
        editingId = null
        caloriesText = ""
        proteinText = ""
        focusManager.clearFocus()
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date.coerceAtMost(today)
        clearForm()
        feedback = null
    }

    fun save() {
        val result = viewModel.saveNutritionEntry(
            editingId,
            selectedDate.toString(),
            caloriesText,
            proteinText,
        )
        feedback = if (result.isSuccess) {
            val message = if (editingId == null) "Apport ajouté" else "Apport mis à jour"
            clearForm()
            message
        } else result.exceptionOrNull()?.message
        focusManager.clearFocus()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nutrition", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        KawaiiHeaderDecoration("🍓")
                    }
                    Text("Ajoute tes apports au fil de la journée.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onExport) { Icon(Icons.Default.FileDownload, "Exporter la nutrition en CSV") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = kawaiiContainer(0, MaterialTheme.colorScheme.surfaceContainer))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectDate(selectedDate.minusDays(1)) }) {
                            Icon(Icons.Default.ChevronLeft, "Jour précédent")
                        }
                        OutlinedButton(
                            onClick = { showDateDialog = true },
                            modifier = Modifier.weight(1f),
                        ) { Text(nutritionDisplayDate(selectedDate), fontWeight = FontWeight.Bold) }
                        IconButton(
                            enabled = selectedDate.isBefore(today),
                            onClick = { selectDate(selectedDate.plusDays(1)) },
                        ) { Icon(Icons.Default.ChevronRight, "Jour suivant") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionMetric("Calories", "$caloriesTotal kcal", Modifier.weight(1f), appVisuals.chartSeries[0])
                        NutritionMetric("Protéines", "${formatProtein(proteinTotal)} g", Modifier.weight(1f), appVisuals.chartSeries[1])
                    }
                    Text(
                        "${dayEntries.size} ${if (dayEntries.size > 1) "apports enregistrés" else "apport enregistré"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = kawaiiContainer(1, MaterialTheme.colorScheme.surfaceContainerLow))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (editingId == null) "Ajouter un apport" else "Modifier l’apport",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        KawaiiCardMascot("🐰")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = caloriesText,
                            onValueChange = { if (it.matches(Regex("\\d{0,5}"))) { caloriesText = it; feedback = null } },
                            label = { Text("Calories") },
                            suffix = { Text("kcal") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = proteinText,
                            onValueChange = { if (it.matches(Regex("\\d{0,4}([,.]\\d?)?"))) { proteinText = it; feedback = null } },
                            label = { Text("Protéines") },
                            suffix = { Text("g") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingId != null) {
                            OutlinedButton(onClick = { clearForm(); feedback = null }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Cancel, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Annuler")
                            }
                        }
                        Button(
                            enabled = normalizedCalories(caloriesText) != null && normalizedProteinGrams(proteinText) != null,
                            onClick = { save() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (editingId == null) "Ajouter" else "Enregistrer")
                        }
                    }
                    feedback?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.startsWith("Apport ")) appVisuals.success else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        if (dayEntries.isEmpty()) {
            item { Text("Aucun apport pour cette journée.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item { Text("Apports du jour", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(dayEntries, key = { it.id }) { entry ->
                NutritionEntryCard(
                    entry = entry,
                    onEdit = {
                        editingId = entry.id
                        caloriesText = entry.caloriesKcal.toString()
                        proteinText = formatProtein(entry.proteinGrams)
                        feedback = null
                    },
                    onDelete = { pendingDelete = entry },
                )
            }
        }
        item {
            Text("Historique", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Totaux quotidiens, même avec plusieurs saisies par jour.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        val trend = viewModel.nutritionTrend(weeks)
        item {
            NutritionChart("Calories", "kcal", trend, { it.caloriesKcal.toDouble() }, appVisuals.chartSeries[0])
        }
        item {
            NutritionChart("Protéines", "g", trend, { it.proteinGrams }, appVisuals.chartSeries[1])
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer cet apport ?") },
            text = { Text("${entry.caloriesKcal} kcal et ${formatProtein(entry.proteinGrams)} g de protéines seront retirés du total du jour.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNutritionEntry(entry.id)
                    if (editingId == entry.id) clearForm()
                    pendingDelete = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Annuler") } },
        )
    }
    if (showDateDialog) {
        var dateText by remember(selectedDate) { mutableStateOf(selectedDate.toString()) }
        val parsed = runCatching { LocalDate.parse(dateText) }.getOrNull()
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("Date des apports") },
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
}

@Composable
private fun NutritionMetric(label: String, value: String, modifier: Modifier, color: Color) {
    Surface(modifier = modifier, color = color.copy(alpha = .13f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun NutritionEntryCard(entry: NutritionEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${entry.caloriesKcal} kcal · ${formatProtein(entry.proteinGrams)} g protéines", fontWeight = FontWeight.SemiBold)
                Text(nutritionTime(entry.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Modifier") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun NutritionChart(
    title: String,
    unit: String,
    points: List<NutritionDayTotal>,
    value: (NutritionDayTotal) -> Double,
    color: Color,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (points.isEmpty()) {
                EmptyChart("Pas encore de données", Modifier.fillMaxWidth().height(150.dp))
                return@Column
            }
            val values = points.map(value)
            val range = chartYRange(values.map { it.toFloat() })
            val minX = points.first().timestamp
            val maxX = points.last().timestamp
            val gridColor = appVisuals.chartGrid
            Row(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.width(52.dp).height(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(formatNutritionAxis(range.max, unit), style = MaterialTheme.typography.labelSmall)
                    Text(formatNutritionAxis((range.max + range.min) / 2, unit), style = MaterialTheme.typography.labelSmall)
                    Text(formatNutritionAxis(range.min, unit), style = MaterialTheme.typography.labelSmall)
                }
                Canvas(Modifier.weight(1f).height(180.dp).padding(start = 8.dp)) {
                    val left = 2.dp.toPx()
                    val right = size.width - 4.dp.toPx()
                    val top = 8.dp.toPx()
                    val bottom = size.height - 8.dp.toPx()
                    repeat(3) { index ->
                        val y = top + index * (bottom - top) / 2f
                        drawLine(gridColor, Offset(left, y), Offset(right, y), 1f)
                    }
                    fun x(timestamp: Long) = left + chartXFraction(timestamp, minX, maxX) * (right - left)
                    fun y(raw: Double) = bottom - (raw.toFloat() - range.min) / (range.max - range.min).coerceAtLeast(1f) * (bottom - top)
                    if (points.size > 1) {
                        val path = Path()
                        points.forEachIndexed { index, point ->
                            val offset = Offset(x(point.timestamp), y(value(point)))
                            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                        }
                        drawPath(path, color, style = Stroke(3.dp.toPx()))
                    }
                    points.forEach { point -> drawCircle(color, 4.dp.toPx(), Offset(x(point.timestamp), y(value(point)))) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 60.dp),
                horizontalArrangement = if (points.size == 1) Arrangement.Center else Arrangement.SpaceBetween,
            ) {
                Text(nutritionShortDate(points.first().date), style = MaterialTheme.typography.labelSmall)
                if (points.size > 1) Text(nutritionShortDate(points.last().date), style = MaterialTheme.typography.labelSmall)
            }
            val latest = points.last()
            Text(
                "Dernier jour : ${if (unit == "kcal") latest.caloriesKcal else formatProtein(latest.proteinGrams)} $unit · ${latest.entryCount} apport${if (latest.entryCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatProtein(value: Double): String = String.format(Locale.FRANCE, "%.1f", value)
private fun nutritionTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
private fun nutritionDisplayDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.FRANCE))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }
private fun nutritionShortDate(date: String): String =
    runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yy")) }.getOrDefault(date)
private fun formatNutritionAxis(value: Float, unit: String): String =
    if (unit == "kcal") value.toInt().toString() else String.format(Locale.FRANCE, "%.0f", value)
