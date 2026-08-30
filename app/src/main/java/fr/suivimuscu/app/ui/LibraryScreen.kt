package fr.suivimuscu.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.suivimuscu.app.MainViewModel
import fr.suivimuscu.app.exerciseUsedInHistory
import fr.suivimuscu.app.muscleUsedInHistory
import fr.suivimuscu.app.programUsedInHistory
import fr.suivimuscu.app.templateUsedInHistory
import fr.suivimuscu.app.data.*

enum class LibraryTab { PROGRAMS, TEMPLATES, EXERCISES, MUSCLES }

@Composable
fun LibraryScreen(
    state: AppState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    selectedTab: LibraryTab = LibraryTab.PROGRAMS,
    onTabSelected: (LibraryTab) -> Unit = {},
    createTemplateRequest: Int = 0,
    onCreateTemplateHandled: () -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bibliothèque", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                KawaiiHeaderDecoration("🎀")
            }
            Text("Tout reste modifiable et archivable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScrollableTabRow(selectedTabIndex = selectedTab.ordinal, edgePadding = 12.dp) {
            LibraryTab.entries.forEach { item ->
                Tab(
                    selected = selectedTab == item,
                    onClick = { onTabSelected(item) },
                    text = {
                        val emoji = when (item) {
                            LibraryTab.PROGRAMS -> "🐰"
                            LibraryTab.TEMPLATES -> "🐼"
                            LibraryTab.EXERCISES -> "🐱"
                            LibraryTab.MUSCLES -> "🌸"
                        }
                        Text(
                            (if (appVisuals.showKawaiiDecorations) "$emoji " else "") + when (item) {
                                LibraryTab.PROGRAMS -> "Programmes"
                                LibraryTab.TEMPLATES -> "Séances"
                                LibraryTab.EXERCISES -> "Exercices"
                                LibraryTab.MUSCLES -> "Muscles"
                            }
                        )
                    }
                )
            }
        }
        when (selectedTab) {
            LibraryTab.PROGRAMS -> ProgramsLibrary(state, viewModel)
            LibraryTab.TEMPLATES -> TemplatesLibrary(
                state,
                viewModel,
                createRequest = createTemplateRequest,
                onCreateRequestHandled = onCreateTemplateHandled,
            )
            LibraryTab.EXERCISES -> ExercisesLibrary(state, viewModel)
            LibraryTab.MUSCLES -> MusclesLibrary(state, viewModel)
        }
    }
}

@Composable
private fun ExercisesLibrary(state: AppState, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var creating by remember { mutableStateOf(false) }
    LibraryList(
        onAdd = { creating = true },
        addLabel = "Nouvel exercice",
    ) {
        items(state.exercises.sortedWith(compareBy<Exercise> { it.archived }.thenBy { it.name }), key = { it.id }) { exercise ->
            val deletable = !exerciseUsedInHistory(state, exercise.id)
            LibraryRow(
                title = exercise.name,
                subtitle = "${exercise.defaultRepMin}–${exercise.defaultRepMax} reps • " +
                    "${exercise.muscles.count { it.role == MuscleRole.PRIMARY }} principal(aux), " +
                    "${exercise.muscles.count { it.role == MuscleRole.SECONDARY }} secondaire(s), " +
                    "${exercise.muscles.count { it.role == MuscleRole.TERTIARY }} tertiaire(s)",
                archived = exercise.archived,
                onEdit = { editing = exercise },
                onArchive = { viewModel.archiveExercise(exercise.id) },
                onDelete = if (deletable) ({ viewModel.deleteExercise(exercise.id) }) else null,
                deleteDialogTitle = "Supprimer « ${exercise.name} » ?",
                deleteDialogText = "Cet exercice n'apparaît dans aucune séance terminée. " +
                    "Il sera aussi retiré des modèles de séances qui le référencent (les modèles devenus vides seront supprimés).",
            )
        }
    }
    if (creating || editing != null) {
        ExerciseEditDialog(
            initial = editing ?: Exercise("", "", 6, 10),
            muscles = state.muscles.filterNot { it.archived },
            onDismiss = { creating = false; editing = null },
            onSave = { viewModel.saveExercise(it); creating = false; editing = null },
        )
    }
}

@Composable
fun ExerciseEditDialog(
    initial: Exercise,
    muscles: List<MuscleGroup>,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var min by remember { mutableStateOf(initial.defaultRepMin.toString()) }
    var max by remember { mutableStateOf(initial.defaultRepMax.toString()) }
    var instruction by remember { mutableStateOf(initial.instruction) }
    var links by remember {
        mutableStateOf(initial.muscles.associate { it.muscleId to it.role })
    }
    val valid = name.isNotBlank() && min.toIntOrNull()?.let { it > 0 } == true &&
        max.toIntOrNull()?.let { it >= (min.toIntOrNull() ?: 1) } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Nouvel exercice" else "Modifier l’exercice") },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            min, { min = it.filter(Char::isDigit) }, label = { Text("Reps min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), singleLine = true,
                        )
                        OutlinedTextField(
                            max, { max = it.filter(Char::isDigit) }, label = { Text("Reps max") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), singleLine = true,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        instruction, { instruction = it },
                        label = { Text("Consigne permanente") },
                        minLines = 2,
                    )
                }
                item {
                    Text("Muscles sollicités", fontWeight = FontWeight.Bold)
                    Text("P : ×1 • S : ×0,5 • T : ×0,25", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(muscles, key = { it.id }) { muscle ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(muscle.name, Modifier.weight(1f))
                        FilterChip(
                            selected = links[muscle.id] == MuscleRole.PRIMARY,
                            onClick = {
                                links = links.toMutableMap().also {
                                    if (it[muscle.id] == MuscleRole.PRIMARY) it.remove(muscle.id)
                                    else it[muscle.id] = MuscleRole.PRIMARY
                                }
                            },
                            label = { Text("P") },
                        )
                        Spacer(Modifier.width(5.dp))
                        FilterChip(
                            selected = links[muscle.id] == MuscleRole.SECONDARY,
                            onClick = {
                                links = links.toMutableMap().also {
                                    if (it[muscle.id] == MuscleRole.SECONDARY) it.remove(muscle.id)
                                    else it[muscle.id] = MuscleRole.SECONDARY
                                }
                            },
                            label = { Text("S") },
                        )
                        Spacer(Modifier.width(5.dp))
                        FilterChip(
                            selected = links[muscle.id] == MuscleRole.TERTIARY,
                            onClick = {
                                links = links.toMutableMap().also {
                                    if (it[muscle.id] == MuscleRole.TERTIARY) it.remove(muscle.id)
                                    else it[muscle.id] = MuscleRole.TERTIARY
                                }
                            },
                            label = { Text("T") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(initial.copy(
                        name = name.trim(),
                        defaultRepMin = min.toInt(),
                        defaultRepMax = max.toInt(),
                        instruction = instruction.trim(),
                        muscles = links.map { MuscleAssignment(it.key, it.value) },
                    ))
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun TemplatesLibrary(
    state: AppState,
    viewModel: MainViewModel,
    createRequest: Int = 0,
    onCreateRequestHandled: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(createRequest) {
        if (createRequest > 0) {
            creating = true
            onCreateRequestHandled()
        }
    }
    LibraryList(onAdd = { creating = true }, addLabel = "Nouvelle séance") {
        items(state.templates.sortedWith(compareBy<WorkoutTemplate> { it.archived }.thenBy { it.name }), key = { it.id }) { template ->
            val deletable = !templateUsedInHistory(state, template.id)
            LibraryRow(
                title = "Séance ${template.name}",
                subtitle = "${template.exercises.size} exercices • ${template.exercises.sumOf { it.targetSets }} séries",
                archived = template.archived,
                onEdit = { editing = template },
                onArchive = { viewModel.archiveTemplate(template.id) },
                onDelete = if (deletable) ({ viewModel.deleteTemplate(template.id) }) else null,
                deleteDialogTitle = "Supprimer la séance « ${template.name} » ?",
                deleteDialogText = "Aucune séance terminée n'y est rattachée. " +
                    "Elle sera retirée du cycle des programmes qui la référencent.",
            )
        }
    }
    if (creating || editing != null) {
        TemplateDialog(
            initial = editing ?: WorkoutTemplate("", "", emptyList()),
            exercises = state.exercises.filterNot { it.archived },
            onDismiss = { creating = false; editing = null },
            onSave = { viewModel.saveTemplate(it); creating = false; editing = null },
        )
    }
}

@Composable
private fun TemplateDialog(
    initial: WorkoutTemplate,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (WorkoutTemplate) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var selected by remember { mutableStateOf(initial.exercises) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Nouvelle séance" else "Modifier la séance") },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true) }
                item { Text("Exercices et séries", fontWeight = FontWeight.Bold) }
                items(exercises, key = { it.id }) { exercise ->
                    val entry = selected.firstOrNull { it.exerciseId == exercise.id }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = entry != null,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + TemplateExercise(exercise.id, 2)
                                        else selected.filterNot { it.exerciseId == exercise.id }
                                    },
                                )
                                Text(exercise.name, Modifier.weight(1f))
                                if (entry != null) {
                                    IconButton(onClick = {
                                        if (entry.targetSets > 1) selected = selected.map {
                                            if (it.exerciseId == exercise.id) it.copy(targetSets = it.targetSets - 1) else it
                                        }
                                    }) { Icon(Icons.Default.Remove, null) }
                                    Text(entry.targetSets.toString(), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        selected = selected.map {
                                            if (it.exerciseId == exercise.id) it.copy(targetSets = (it.targetSets + 1).coerceAtMost(20)) else it
                                        }
                                    }) { Icon(Icons.Default.Add, null) }
                                }
                            }
                            if (entry != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = entry.repMinOverride?.toString().orEmpty(),
                                        onValueChange = { value ->
                                            if (value.matches(Regex("""\d{0,3}"""))) selected = selected.map {
                                                if (it.exerciseId == exercise.id) it.copy(repMinOverride = value.toIntOrNull()) else it
                                            }
                                        },
                                        label = { Text("Min (déf. ${exercise.defaultRepMin})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = entry.repMaxOverride?.toString().orEmpty(),
                                        onValueChange = { value ->
                                            if (value.matches(Regex("""\d{0,3}"""))) selected = selected.map {
                                                if (it.exerciseId == exercise.id) it.copy(repMaxOverride = value.toIntOrNull()) else it
                                            }
                                        },
                                        label = { Text("Max (déf. ${exercise.defaultRepMax})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && selected.isNotEmpty(),
                onClick = { onSave(initial.copy(name = name.trim(), exercises = selected)) },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun ProgramsLibrary(state: AppState, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf<TrainingProgram?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TrainingProgram?>(null) }
    LibraryList(onAdd = { creating = true }, addLabel = "Nouveau programme") {
        items(state.programs.sortedWith(compareByDescending<TrainingProgram> { it.active }.thenBy { it.archived }), key = { it.id }) { program ->
            val cycleNames = program.templateCycle.mapNotNull { id -> state.templates.firstOrNull { it.id == id }?.name }
            val deletable = !programUsedInHistory(state, program.id)
            ProgramRow(
                program = program,
                subtitle = cycleNames.joinToString(" → ") + " • " +
                    program.trainingDays.joinToString("/") { dayShort(it) },
                onActivate = { viewModel.activateProgram(program.id) },
                onEdit = { editing = program },
                onArchive = { viewModel.archiveProgram(program.id) },
                onDelete = if (deletable) ({ pendingDelete = program }) else null,
            )
        }
    }
    if (creating || editing != null) {
        ProgramDialog(
            initial = editing ?: TrainingProgram("", "", emptyList()),
            templates = state.templates.filterNot { it.archived },
            onDismiss = { creating = false; editing = null },
            onSave = { viewModel.saveProgram(it); creating = false; editing = null },
        )
    }
    pendingDelete?.let { program ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer définitivement « ${program.name} » ?") },
            text = {
                Text("Le programme et ses règles d’alternance seront supprimés. Les séances, exercices et performances resteront intacts.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProgram(program.id)
                    pendingDelete = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun ProgramDialog(
    initial: TrainingProgram,
    templates: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onSave: (TrainingProgram) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var cycle by remember { mutableStateOf(initial.templateCycle) }
    var days by remember { mutableStateOf(initial.trainingDays.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Nouveau programme" else "Modifier le programme") },
        text = {
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true) }
                item { Text("Cycle des séances", fontWeight = FontWeight.Bold) }
                items(templates, key = { it.id }) { template ->
                    val index = cycle.indexOf(template.id)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = index >= 0,
                            onCheckedChange = { checked ->
                                cycle = if (checked) cycle + template.id else cycle.filterNot { it == template.id }
                            },
                        )
                        Text("Séance ${template.name}", Modifier.weight(1f))
                        if (index > 0) IconButton(onClick = {
                            cycle = cycle.toMutableList().also {
                                val value = it.removeAt(index); it.add(index - 1, value)
                            }
                        }) { Icon(Icons.Default.ArrowUpward, "Monter") }
                        if (index >= 0 && index < cycle.lastIndex) IconButton(onClick = {
                            cycle = cycle.toMutableList().also {
                                val value = it.removeAt(index); it.add(index + 1, value)
                            }
                        }) { Icon(Icons.Default.ArrowDownward, "Descendre") }
                    }
                }
                item { Text("Jours indicatifs", fontWeight = FontWeight.Bold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items((1..7).toList()) { day ->
                            FilterChip(
                                selected = day in days,
                                onClick = { days = if (day in days) days - day else days + day },
                                label = { Text(dayShort(day)) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && cycle.isNotEmpty(),
                onClick = { onSave(initial.copy(name = name.trim(), templateCycle = cycle, trainingDays = days.sorted())) },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun ProgramRow(
    program: TrainingProgram,
    subtitle: String,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val container = when {
        program.archived -> MaterialTheme.colorScheme.surface.copy(alpha = .45f)
        program.active -> kawaiiContainer(program.id.hashCode(), appVisuals.activeContainer)
        else -> kawaiiContainer(program.id.hashCode(), MaterialTheme.colorScheme.surface)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (program.active) androidx.compose.foundation.BorderStroke(1.dp, appVisuals.success) else null,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KawaiiCardMascot(kawaiiMascot(program.id.hashCode()), Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            program.name,
                            fontWeight = FontWeight.Bold,
                            color = if (program.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        if (program.active) {
                            Spacer(Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("ACTIF") },
                                icon = { Icon(Icons.Default.CheckCircle, null) },
                            )
                        }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (program.archived) {
                        Text("ARCHIVÉ", style = MaterialTheme.typography.labelSmall, color = appVisuals.warning)
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Modifier") }
                IconButton(onClick = onArchive) {
                    Icon(
                        if (program.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                        if (program.archived) "Réactiver" else "Archiver",
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteForever, "Supprimer définitivement", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (!program.archived && !program.active) {
                OutlinedButton(onClick = onActivate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Activer ce programme")
                }
            }
        }
    }
}

@Composable
private fun MusclesLibrary(state: AppState, viewModel: MainViewModel) {
    var editing by remember { mutableStateOf<MuscleGroup?>(null) }
    var creating by remember { mutableStateOf(false) }
    LibraryList(onAdd = { creating = true }, addLabel = "Nouveau muscle") {
        items(state.muscles.sortedWith(compareBy<MuscleGroup> { it.archived }.thenBy { it.name }), key = { it.id }) { muscle ->
            val count = state.exercises.count { ex -> ex.muscles.any { it.muscleId == muscle.id } }
            val deletable = !muscleUsedInHistory(state, muscle.id)
            LibraryRow(
                title = muscle.name,
                subtitle = "Utilisé par $count exercice(s)",
                archived = muscle.archived,
                onEdit = { editing = muscle },
                onArchive = { viewModel.archiveMuscle(muscle.id) },
                onDelete = if (deletable) ({ viewModel.deleteMuscle(muscle.id) }) else null,
                deleteDialogTitle = "Supprimer « ${muscle.name} » ?",
                deleteDialogText = "Ce muscle n'apparaît dans aucune séance terminée. " +
                    "Il sera retiré des associations de tous les exercices.",
            )
        }
    }
    if (creating || editing != null) {
        var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { creating = false; editing = null },
            title = { Text(if (editing == null) "Nouveau muscle" else "Modifier le muscle") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true) },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.saveMuscle((editing ?: MuscleGroup("", "")).copy(name = name.trim()))
                        creating = false; editing = null
                    },
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun LibraryList(
    onAdd: () -> Unit,
    addLabel: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Button(onClick = onAdd, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(addLabel)
            }
        }
        content()
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    archived: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteDialogTitle: String = "",
    deleteDialogText: String = "",
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (archived) {
                MaterialTheme.colorScheme.surface.copy(alpha = .45f)
            } else {
                kawaiiContainer(title.hashCode(), MaterialTheme.colorScheme.surface)
            },
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KawaiiCardMascot(kawaiiMascot(title.hashCode()), Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (archived) Text("ARCHIVÉ", style = MaterialTheme.typography.labelSmall, color = appVisuals.warning)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Modifier") }
            IconButton(onClick = onArchive) {
                Icon(if (archived) Icons.Default.Unarchive else Icons.Default.Archive, if (archived) "Réactiver" else "Archiver")
            }
            if (onDelete != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.DeleteForever, "Supprimer définitivement", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(deleteDialogTitle) },
            text = { Text(deleteDialogText) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete?.invoke()
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

private fun dayShort(day: Int) = listOf("", "L", "Ma", "Me", "J", "V", "S", "D").getOrElse(day) { "?" }
