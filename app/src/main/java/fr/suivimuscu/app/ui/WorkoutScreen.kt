package fr.suivimuscu.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.MainViewModel
import fr.suivimuscu.app.MAX_EDITABLE_DURATION_SECONDS
import fr.suivimuscu.app.adjustedReps
import fr.suivimuscu.app.data.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    state: AppState,
    workout: WorkoutLog,
    viewModel: MainViewModel,
) {
    var showAddExercise by remember { mutableStateOf(false) }
    var showCreateExercise by remember { mutableStateOf(false) }
    var showAbandon by remember { mutableStateOf(false) }
    var showPause by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showDuration by remember { mutableStateOf(false) }
    var exerciseQuery by remember { mutableStateOf("") }
    var elapsedNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val activeRest = workout.exercises.firstOrNull { it.restStartedAt != null }
    LaunchedEffect(workout.exercises.map { it.restStartedAt }) {
        while (workout.exercises.any { it.restStartedAt != null }) {
            elapsedNow = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(workout.startedAt, workout.endedAt) {
        while (workout.endedAt == null) {
            elapsedNow = System.currentTimeMillis()
            delay(1000)
        }
    }
    BackHandler {
        if (showAbandon) showAbandon = false else showAbandon = true
    }

    Scaffold(
        modifier = Modifier.appBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Séance ${workout.templateNameSnapshot}", fontWeight = FontWeight.Bold)
                            KawaiiHeaderDecoration("🐱")
                        }
                        Text(workout.localDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showAbandon = true }) { Icon(Icons.Default.Close, "Abandonner") }
                },
                actions = {
                    IconButton(onClick = { showDate = true }) {
                        Icon(Icons.Default.CalendarMonth, "Modifier la date")
                    }
                    IconButton(onClick = { showNote = true }) {
                        Icon(if (workout.note.isBlank()) Icons.Default.Notes else Icons.Default.StickyNote2, "Note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = kawaiiContainer(0, MaterialTheme.colorScheme.surface),
                ),
            )
        },
        bottomBar = {
            Surface(
                Modifier.navigationBarsPadding(),
                color = kawaiiContainer(2, MaterialTheme.colorScheme.surface),
                shadowElevation = 8.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showAddExercise = true }, Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, null)
                        Text(" Exercice")
                    }
                    Button(onClick = viewModel::finishWorkout, Modifier.weight(1.35f)) {
                        Icon(Icons.Default.Check, null)
                        Text(" Terminer")
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (activeRest == null) 12.dp else 84.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        color = kawaiiContainer(1, appVisuals.pausedContainer),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            "Poids + répétitions requis • RIR facultatif • touche ✓ pour valider. Toute modification invalide la série.",
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    val durationSeconds = (((workout.endedAt ?: elapsedNow) - workout.startedAt) / 1000L)
                        .coerceIn(0, MAX_EDITABLE_DURATION_SECONDS.toLong()).toInt()
                    OutlinedButton(
                        onClick = { showDuration = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Durée ${formatSessionDuration(durationSeconds)}")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Edit, "Modifier la durée")
                    }
                }
                itemsIndexed(workout.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                    ExerciseLogCard(
                        exercise = exercise,
                        canMoveUp = index > 0,
                        canMoveDown = index < workout.exercises.lastIndex,
                        viewModel = viewModel,
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            activeRest?.let { exercise ->
                RestTimerBar(
                    exercise = exercise,
                    now = elapsedNow,
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (showAddExercise) {
        val already = workout.exercises.map { it.exerciseId }.toSet()
        val available = state.exercises
            .filter { !it.archived && it.id !in already }
            .filter { it.name.contains(exerciseQuery.trim(), ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
        AlertDialog(
            onDismissRequest = { showAddExercise = false; exerciseQuery = "" },
            title = { Text("Ajouter un exercice") },
            text = {
                Column(Modifier.heightIn(max = 520.dp)) {
                    OutlinedButton(
                        onClick = { showCreateExercise = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AddCircle, null)
                        Text(" Créer un nouvel exercice")
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    OutlinedTextField(
                        value = exerciseQuery,
                        onValueChange = { exerciseQuery = it },
                        label = { Text("Rechercher dans la bibliothèque") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (available.isEmpty()) {
                        Text(
                            if (exerciseQuery.isBlank()) "Tous les exercices disponibles sont déjà dans la séance."
                            else "Aucun exercice ne correspond à cette recherche.",
                            modifier = Modifier.padding(vertical = 18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            items(available, key = { it.id }) { exercise ->
                                ListItem(
                                    headlineContent = { Text(exercise.name) },
                                    supportingContent = { Text("${exercise.defaultRepMin}–${exercise.defaultRepMax} reps") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingContent = {
                                        IconButton(onClick = {
                                            viewModel.addExerciseToDraft(exercise.id)
                                            showAddExercise = false
                                            exerciseQuery = ""
                                        }) { Icon(Icons.Default.AddCircle, "Ajouter") }
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddExercise = false; exerciseQuery = "" }) { Text("Fermer") } },
        )
    }
    if (showCreateExercise) {
        ExerciseEditDialog(
            initial = Exercise("", "", 6, 10),
            muscles = state.muscles.filterNot { it.archived },
            onDismiss = { showCreateExercise = false },
            onSave = { exercise ->
                viewModel.createExerciseInDraft(exercise)
                showCreateExercise = false
                showAddExercise = false
            },
        )
    }
    if (showAbandon) {
        AlertDialog(
            onDismissRequest = { showAbandon = false },
            title = { Text("Quitter la séance ?") },
            text = {
                Text(
                    if (workout.editingCompletedLog) "Tu peux continuer la séance, revenir à l’application en gardant les modifications, ou abandonner (la séance restera dans l’historique avec les modifications déjà saisies)."
                    else "Tu peux continuer la séance, la mettre en pause pour revenir plus tard, ou l’abandonner définitivement.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAbandon = false
                    viewModel.abandonDraft()
                }) { Text("Abandonner", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showAbandon = false }) { Text("Continuer") }
                    TextButton(onClick = {
                        showAbandon = false
                        viewModel.minimizeDraft()
                    }) { Text(if (workout.editingCompletedLog) "Revenir" else "Mettre en pause") }
                }
            },
        )
    }
    if (showDate) {
        var date by remember(workout.localDate) { mutableStateOf(workout.localDate) }
        AlertDialog(
            onDismissRequest = { showDate = false },
            title = { Text("Date de la séance") },
            text = {
                OutlinedTextField(
                    date,
                    { date = it },
                    label = { Text("AAAA-MM-JJ") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateWorkoutDate(date)
                    showDate = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Annuler") } },
        )
    }
    if (showNote) {
        var note by remember(workout.note) { mutableStateOf(workout.note) }
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text("Note de séance") },
            text = {
                OutlinedTextField(note, { note = it }, label = { Text("Sommeil, gêne, machine…") }, minLines = 3)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateWorkoutNote(note)
                    showNote = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showNote = false }) { Text("Annuler") } },
        )
    }
    if (showDuration) {
        val durationSeconds = (((workout.endedAt ?: elapsedNow) - workout.startedAt) / 1000L)
            .coerceIn(0, MAX_EDITABLE_DURATION_SECONDS.toLong()).toInt()
        DurationEditDialog(
            title = "Durée de la séance",
            initialSeconds = durationSeconds,
            allowClear = false,
            onDismiss = { showDuration = false },
            onSave = { seconds ->
                viewModel.updateWorkoutDuration(seconds ?: 0, elapsedNow)
                showDuration = false
            },
        )
    }
}

@Composable
private fun RestTimerBar(
    exercise: LoggedExercise,
    now: Long,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val start = exercise.restStartedAt ?: return
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, appVisuals.success),
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Timer, null, tint = appVisuals.success)
            Column(Modifier.weight(1f)) {
                Text(
                    "Repos • ${exercise.nameSnapshot}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(formatSeconds(((now - start) / 1000).toInt()), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { viewModel.ignoreRest(exercise.id) }) { Text("Ignorer") }
            Button(onClick = { viewModel.startNextSet(exercise.id) }) { Text("Série suivante") }
        }
    }
}

@Composable
private fun ExerciseLogCard(
    exercise: LoggedExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    viewModel: MainViewModel,
) {
    var confirmRemove by remember { mutableStateOf(false) }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = kawaiiContainer(exercise.id.hashCode(), MaterialTheme.colorScheme.surfaceContainerLow),
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                KawaiiCardMascot(kawaiiMascot(exercise.id.hashCode()), Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(exercise.nameSnapshot, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${exercise.repMinSnapshot}–${exercise.repMaxSnapshot} reps", color = appVisuals.success)
                    if (exercise.instructionSnapshot.isNotBlank()) {
                        Text(exercise.instructionSnapshot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(
                    enabled = canMoveUp,
                    onClick = { viewModel.moveExerciseInDraft(exercise.id, -1) },
                ) { Icon(Icons.Default.ArrowUpward, "Monter l’exercice") }
                IconButton(
                    enabled = canMoveDown,
                    onClick = { viewModel.moveExerciseInDraft(exercise.id, 1) },
                ) { Icon(Icons.Default.ArrowDownward, "Descendre l’exercice") }
                IconButton(onClick = { confirmRemove = true }) {
                    Icon(Icons.Default.DeleteOutline, "Retirer", tint = MaterialTheme.colorScheme.error)
                }
            }
            exercise.sets.forEach { set ->
                SetRow(exercise, set, viewModel)
            }
            TextButton(onClick = { viewModel.addSet(exercise.id) }) {
                Icon(Icons.Default.Add, null)
                Text(" Ajouter une série")
            }
        }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Retirer ${exercise.nameSnapshot} ?") },
            text = { Text("Les séries de cet exercice seront retirées du brouillon.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.removeExerciseFromDraft(exercise.id)
                }) { Text("Retirer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun SetRow(exercise: LoggedExercise, set: WorkoutSet, viewModel: MainViewModel) {
    val focusManager = LocalFocusManager.current
    var showRestEdit by remember { mutableStateOf(false) }
    val valid = set.reps.toIntOrNull()?.let { it > 0 } == true &&
        set.weightKg.replace(',', '.').toDoubleOrNull()?.let { it >= 0 } == true
    val repsNumber = set.reps.toIntOrNull()
    val rangeColor = when {
        repsNumber == null -> MaterialTheme.colorScheme.onSurfaceVariant
        repsNumber < exercise.repMinSnapshot -> appVisuals.warning
        repsNumber > exercise.repMaxSnapshot -> appVisuals.info
        else -> appVisuals.success
    }
    Surface(
        color = kawaiiContainer(set.order + 1, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)),
        shape = MaterialTheme.shapes.medium,
        border = if (set.completed) {
            androidx.compose.foundation.BorderStroke(1.5.dp, appVisuals.success)
        } else null,
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("${set.order}", fontWeight = FontWeight.Bold, color = rangeColor, modifier = Modifier.width(18.dp))
                OutlinedTextField(
                    value = set.weightKg,
                    onValueChange = { value ->
                        if (value.matches(Regex("""\d{0,4}([.,]\d{0,2})?"""))) {
                            viewModel.updateSet(exercise.id, set.id, weight = value)
                        }
                    },
                    label = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = set.reps,
                    onValueChange = { value ->
                        if (value.matches(Regex("""\d{0,3}"""))) viewModel.updateSet(exercise.id, set.id, reps = value)
                    },
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = valid && !set.completed,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        viewModel.validateSet(exercise.id, set.id)
                    },
                ) {
                    Icon(
                        if (set.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        if (set.completed) "Série validée" else "Valider la série",
                        tint = when {
                            set.completed -> appVisuals.success
                            valid -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (exercise.sets.size > 1) {
                    IconButton(onClick = { viewModel.removeSet(exercise.id, set.id) }) {
                        Icon(Icons.Default.RemoveCircleOutline, "Supprimer série", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = repsNumber != null && repsNumber > 1,
                    onClick = {
                        viewModel.updateSet(
                            exercise.id,
                            set.id,
                            reps = adjustedReps(set.reps, -1),
                        )
                    },
                ) { Text("−1 rep") }
                TextButton(
                    enabled = repsNumber == null || repsNumber < 999,
                    onClick = {
                        viewModel.updateSet(
                            exercise.id,
                            set.id,
                            reps = adjustedReps(set.reps, 1),
                        )
                    },
                ) { Text("+1 rep") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showRestEdit = true }) {
                    Icon(Icons.Default.Timer, null)
                    Spacer(Modifier.width(4.dp))
                    Text(set.restBeforeSeconds?.let { "Repos ${formatSeconds(it)}" } ?: "Repos —")
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("RIR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                (0..3).forEach { rir ->
                    FilterChip(
                        selected = set.rir == rir,
                        onClick = {
                            viewModel.updateSet(
                                exercise.id, set.id,
                                rir = rir.takeUnless { set.rir == rir },
                                updateRir = true,
                            )
                        },
                        label = { Text(rir.toString()) },
                        modifier = Modifier.padding(end = 5.dp),
                    )
                }
            }
        }
    }
    if (showRestEdit) {
        DurationEditDialog(
            title = "Repos avant la série ${set.order}",
            initialSeconds = set.restBeforeSeconds ?: 0,
            allowClear = true,
            onDismiss = { showRestEdit = false },
            onSave = { seconds ->
                viewModel.updateSetRest(exercise.id, set.id, seconds)
                showRestEdit = false
            },
        )
    }
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun formatSessionDuration(seconds: Int): String = "%d:%02d:%02d".format(
    seconds / 3600,
    (seconds % 3600) / 60,
    seconds % 60,
)

@Composable
private fun DurationEditDialog(
    title: String,
    initialSeconds: Int,
    allowClear: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit,
) {
    var hours by remember(initialSeconds) { mutableStateOf((initialSeconds / 3600).toString()) }
    var minutes by remember(initialSeconds) { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }
    var seconds by remember(initialSeconds) { mutableStateOf((initialSeconds % 60).toString()) }
    val total = (hours.toIntOrNull() ?: -1) * 3600 +
        (minutes.toIntOrNull() ?: -1) * 60 + (seconds.toIntOrNull() ?: -1)
    val valid = hours.toIntOrNull()?.let { it in 0..24 } == true &&
        minutes.toIntOrNull()?.let { it in 0..59 } == true &&
        seconds.toIntOrNull()?.let { it in 0..59 } == true &&
        total in 0..MAX_EDITABLE_DURATION_SECONDS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("h" to hours, "min" to minutes, "s" to seconds).forEach { (label, value) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { next ->
                            if (next.matches(Regex("""\d{0,2}"""))) when (label) {
                                "h" -> hours = next
                                "min" -> minutes = next
                                else -> seconds = next
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(total) }) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (allowClear) TextButton(onClick = { onSave(null) }) { Text("Effacer") }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
}
