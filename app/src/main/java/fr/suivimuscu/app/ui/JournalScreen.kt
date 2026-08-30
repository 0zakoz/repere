package fr.suivimuscu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.MainViewModel
import fr.suivimuscu.app.workoutChronologyTimestamp
import fr.suivimuscu.app.data.AppState
import fr.suivimuscu.app.data.WorkoutLog
import fr.suivimuscu.app.data.WorkoutStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalScreen(
    state: AppState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onDeleted: () -> Unit,
    onCreateTemplate: () -> Unit,
    onOpenPrograms: () -> Unit,
) {
    val suggested = viewModel.suggestedTemplate()
    val program = viewModel.activeProgram()
    var showTemplates by remember { mutableStateOf(false) }
    var alternateTemplateId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var showMissed by remember { mutableStateOf(viewModel.missedSlotCount() > 0) }
    val missedCount = viewModel.missedSlotCount()
    val history = state.workoutLogs.filter {
        it.status == WorkoutStatus.COMPLETED && it.deletedAt == null
    }.sortedByDescending { workoutChronologyTimestamp(it) }
    val pausedDraft = state.workoutLogs.firstOrNull {
        it.status == WorkoutStatus.DRAFT && it.deletedAt == null
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Suivi Muscu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Ton carnet, sans friction.", color = Muted)
                }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Réglages") }
            }
        }
        if (pausedDraft != null) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Cyan.copy(alpha = .10f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PauseCircle, null, tint = Cyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Séance en pause", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Séance ${pausedDraft.templateNameSnapshot} • " +
                                "${pausedDraft.exercises.sumOf { ex -> ex.sets.count { it.completed } }} série(s) validée(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        Button(onClick = viewModel::resumeDraft, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reprendre la séance")
                        }
                    }
                }
            }
        }
        if (program == null) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Orange.copy(alpha = .10f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Aucun programme actif", fontWeight = FontWeight.Bold)
                        Text(
                            "Sélectionne un programme existant ou crée-en un pour retrouver la prochaine séance suggérée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        OutlinedButton(onClick = onOpenPrograms, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AccountTree, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Créer ou sélectionner un programme")
                        }
                    }
                }
            }
        }
        if (suggested != null) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Lime.copy(alpha = .12f))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PROCHAINE SÉANCE", style = MaterialTheme.typography.labelMedium, color = Lime)
                        Text(
                            "${program?.name.orEmpty()} • Séance ${suggested.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("${suggested.exercises.size} exercices • ${suggested.exercises.sumOf { it.targetSets }} séries prévues")
                        Button(
                            onClick = { viewModel.startWorkout(suggested.id, true) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Démarrer ${suggested.name}")
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showTemplates = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nouvelle séance")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Historique", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${history.size} séance(s)", color = Muted)
            }
        }
        if (history.isEmpty()) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = Muted)
                        Spacer(Modifier.height(8.dp))
                        Text("Ta première séance apparaîtra ici.", color = Muted)
                    }
                }
            }
        }
        items(history, key = { it.id }) { log ->
            val count = log.exercises.sumOf { ex -> ex.sets.count { it.completed } }
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Séance ${log.templateNameSnapshot}", fontWeight = FontWeight.Bold)
                        Text(
                            "${formatWorkoutDateTime(log)} • ${formatWorkoutDuration(log)} • $count séries" +
                                log.programNameSnapshot?.let { " • $it" }.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                        if (log.note.isNotBlank()) Text(log.note, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.editWorkout(log.id) }) {
                        Icon(Icons.Default.Edit, "Modifier")
                    }
                    IconButton(onClick = { pendingDelete = log.id }) {
                        Icon(Icons.Default.DeleteOutline, "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showTemplates) {
        AlertDialog(
            onDismissRequest = { showTemplates = false },
            title = { Text("Choisir une séance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.templates.filterNot { it.archived }.forEach { template ->
                        OutlinedButton(
                            onClick = {
                                showTemplates = false
                                val expected = suggested?.id == template.id
                                if (expected || suggested == null) viewModel.startWorkout(template.id, expected)
                                else alternateTemplateId = template.id
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${template.name} • ${template.exercises.size} exercices")
                        }
                    }
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = {
                            showTemplates = false
                            onCreateTemplate()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AddCircleOutline, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Créer une séance")
                    }
                    Text(
                        "Une séance différente de la suggestion sera enregistrée hors programme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showTemplates = false }) { Text("Annuler") } },
        )
    }

    alternateTemplateId?.let { templateId ->
        val template = state.templates.firstOrNull { it.id == templateId }
        AlertDialog(
            onDismissRequest = { alternateTemplateId = null },
            title = { Text("Séance ${template?.name.orEmpty()} à la place de ${suggested?.name.orEmpty()} ?") },
            text = { Text("Choisis si elle doit modifier l’avancement du programme.") },
            confirmButton = {
                TextButton(onClick = {
                    alternateTemplateId = null
                    viewModel.startWorkout(templateId, true)
                }) { Text("Remplace la séance prévue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    alternateTemplateId = null
                    viewModel.startWorkout(templateId, false)
                }) { Text("Hors programme") }
            },
        )
    }

    if (showMissed && missedCount > 0) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("$missedCount créneau(x) manqué(s)") },
            text = { Text("Veux-tu reprendre la prochaine séance attendue ou avancer le cycle ?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.acknowledgeMissedSlots()
                    showMissed = false
                }) { Text("Reprendre") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.skipMissedSlots(missedCount)
                    showMissed = false
                }) { Text("Sauter les créneaux") }
            },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer cette séance ?") },
            text = { Text("Elle disparaîtra aussi des tendances. Tu pourras annuler juste après.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteWorkout(id)
                    onDeleted()
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Garder") } },
        )
    }
}

private fun formatWorkoutDateTime(log: WorkoutLog): String {
    val date = runCatching {
        LocalDate.parse(log.localDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }.getOrDefault(log.localDate)
    val time = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(log.startedAt))
    return "$date à $time"
}

private fun formatWorkoutDuration(log: WorkoutLog): String {
    val seconds = ((log.endedAt ?: log.startedAt) - log.startedAt).coerceAtLeast(0) / 1000
    val minutes = seconds / 60
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0L -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}
