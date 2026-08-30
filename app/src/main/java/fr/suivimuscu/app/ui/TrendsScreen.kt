package fr.suivimuscu.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.MainViewModel
import fr.suivimuscu.app.data.AppState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TrendTab { EXERCISE, SESSION, MUSCLE }

@Composable
fun TrendsScreen(state: AppState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var trendTab by remember { mutableStateOf(TrendTab.EXERCISE) }
    var weeks by remember { mutableStateOf<Int?>(12) }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("Tendances", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Des données brutes, sans score opaque.", color = Muted)
        }
        PrimaryTabRow(selectedTabIndex = trendTab.ordinal) {
            Tab(
                selected = trendTab == TrendTab.EXERCISE,
                onClick = { trendTab = TrendTab.EXERCISE },
                text = { Text("Exercices") },
                icon = { Icon(Icons.Default.ShowChart, null) },
            )
            Tab(
                selected = trendTab == TrendTab.SESSION,
                onClick = { trendTab = TrendTab.SESSION },
                text = { Text("Séances") },
                icon = { Icon(Icons.Default.FitnessCenter, null) },
            )
            Tab(
                selected = trendTab == TrendTab.MUSCLE,
                onClick = { trendTab = TrendTab.MUSCLE },
                text = { Text("Muscles") },
                icon = { Icon(Icons.Default.MonitorHeart, null) },
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf(4, 12, 52, null)) { value ->
                FilterChip(
                    selected = weeks == value,
                    onClick = { weeks = value },
                    label = { Text(value?.let { "$it sem." } ?: "Tout") },
                )
            }
        }
        when (trendTab) {
            TrendTab.EXERCISE -> ExerciseTrend(state, viewModel, weeks)
            TrendTab.SESSION -> SessionTrend(state, viewModel, weeks)
            TrendTab.MUSCLE -> MuscleTrend(state, viewModel, weeks)
        }
    }
}

@Composable
private fun ExerciseTrend(state: AppState, viewModel: MainViewModel, weeks: Int?) {
    val active = state.exercises.filterNot { it.archived }
    var selectedId by remember(active) { mutableStateOf(active.firstOrNull()?.id) }
    val selected = state.exercises.firstOrNull { it.id == selectedId }
    val points = selectedId?.let { viewModel.exerciseHistory(it, weeks) }.orEmpty()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(active, key = { it.id }) { exercise ->
                    FilterChip(
                        selected = selectedId == exercise.id,
                        onClick = { selectedId = exercise.id },
                        label = { Text(exercise.name) },
                    )
                }
            }
        }
        selected?.let { exercise ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${exercise.defaultRepMin}–${exercise.defaultRepMax} reps", color = Lime)
                    }
                    Text("${points.map { it.date }.distinct().size} séance(s)", color = Muted)
                }
            }
            item { ExerciseCharts(points) }
            if (points.isNotEmpty()) {
                item { Text("Dernières séries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(points.takeLast(12).reversed()) { point ->
                    ListItem(
                        headlineContent = { Text("${point.weight} kg × ${point.reps.toInt()} reps") },
                        supportingContent = {
                            Text("${point.date} • série ${point.setOrder}" +
                                point.rir?.let { " • RIR $it" }.orEmpty() +
                                point.restSeconds?.let { " • repos ${it}s" }.orEmpty())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionTrend(state: AppState, viewModel: MainViewModel, weeks: Int?) {
    val templates = state.templates.filterNot { it.archived }
    var selectedId by remember(templates) { mutableStateOf(templates.firstOrNull()?.id) }
    val summaries = selectedId?.let { viewModel.sessionSummaries(it, weeks) }.orEmpty()
    val periodStats = selectedId?.let { viewModel.sessionPeriodStats(it, weeks) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates, key = { it.id }) { template ->
                    FilterChip(
                        selected = selectedId == template.id,
                        onClick = { selectedId = template.id },
                        label = { Text("Séance ${template.name}") },
                    )
                }
            }
        }
        if (summaries.isEmpty()) item { EmptyChart("Pas encore de séance terminée") }
        periodStats?.let { stats ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Lime.copy(alpha = .10f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Synthèse de la période", fontWeight = FontWeight.Bold)
                                Text(
                                    weeks?.let { "$it dernières semaines" } ?: "Toutes les séances",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                )
                            }
                            Text("${stats.sessionCount} séance(s)", color = Lime)
                        }
                        LinearProgressIndicator(
                            progress = { stats.completionRate.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${formatMetric(stats.averageCompletedSets)}/${formatMetric(stats.averagePlannedSets)} séries en moyenne" +
                                " • ${(stats.completionRate * 100).toInt()} % réalisées"
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Durée moy. : ${formatDuration(stats.averageDurationSeconds.toLong())}", color = Muted)
                            Text("RIR moy. : ${stats.averageRir?.let(::formatMetric) ?: "—"}", color = Muted)
                        }
                        Text(
                            "Repos moyen : ${stats.averageRest?.let { "${it.toInt()} s" } ?: "—"}",
                            color = Muted,
                        )
                    }
                }
            }
            item {
                Text("Séries moyennes par exercice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(stats.exercises, key = { it.exerciseId }) { exercise ->
                val ratio = if (exercise.averagePlannedSets > 0) {
                    exercise.averageCompletedSets / exercise.averagePlannedSets
                } else 0.0
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${formatMetric(exercise.averageCompletedSets)}/${formatMetric(exercise.averagePlannedSets)}",
                                color = Lime,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ratio.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("séries réalisées en moyenne par séance", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            item {
                Text("Détail des séances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        items(summaries.reversed()) { summary ->
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatSessionDate(summary.timestamp), fontWeight = FontWeight.Bold)
                        Text(formatDuration(summary.durationSeconds), color = Lime)
                    }
                    LinearProgressIndicator(
                        progress = { (summary.completedSets.toFloat() / summary.plannedSets.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${summary.completedSets}/${summary.plannedSets} séries")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RIR moyen : ${summary.averageRir?.let { "%.1f".format(it) } ?: "—"}", color = Muted)
                        Text("Repos moyen : ${summary.averageRest?.let { "${it.toInt()} s" } ?: "—"}", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleTrend(state: AppState, viewModel: MainViewModel, weeks: Int?) {
    val weeksData = viewModel.muscleWeeklyVolume(weeks)
    val stats = viewModel.musclePeriodStats(weeks)
    val totals = state.muscles.filterNot { it.archived }.map { muscle ->
        muscle.name to (stats[muscle.id]?.weightedSets ?: 0.0)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Séries pondérées", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Principal ×1 • secondaire ×0,5 • tertiaire ×0,25", color = Muted)
            Text("Semaine du lundi au dimanche", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        item { HorizontalBars(totals) }
        item {
            MuscleFigure(
                muscles = state.muscles,
                stats = stats,
            )
        }
        if (stats.isNotEmpty()) {
            item {
                Text("Moyennes sur la période", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(
                state.muscles.filterNot { it.archived }
                    .filter { (stats[it.id]?.weightedSets ?: 0.0) > 0 }
                    .sortedByDescending { stats[it.id]?.weightedSets ?: 0.0 },
                key = { it.id },
            ) { muscle ->
                val metric = stats.getValue(muscle.id)
                ListItem(
                    headlineContent = { Text(muscle.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(
                            "${formatMetric(metric.weightedSets)} séries pondérées • " +
                                "${metric.averageReps?.let { "${formatMetric(it)} reps moy." } ?: "reps moy. —"} • " +
                                "${metric.averageRir?.let { "RIR moy. ${formatMetric(it)}" } ?: "RIR moy. —"}"
                        )
                    },
                )
            }
        }
        if (weeksData.isNotEmpty()) {
            item { Text("Détail hebdomadaire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(weeksData.reversed()) { week ->
                val total = week.volumes.values.sum()
                Card {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Semaine du ${week.weekLabel}")
                        Text("%.1f séries pondérées".format(total), color = Lime)
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "${minutes} min"
        minutes % 60 == 0L -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}

private fun formatSessionDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

private fun formatMetric(value: Double): String =
    String.format(java.util.Locale.FRANCE, "%.1f", value)
