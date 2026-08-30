package fr.suivimuscu.app

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.suivimuscu.app.data.CsvExporter
import fr.suivimuscu.app.data.CompleteMarkdownExporter
import fr.suivimuscu.app.data.WeightCsvExporter
import fr.suivimuscu.app.ui.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as SuiviMuscuApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuiviMuscuTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val tab by viewModel.tab.collectAsStateWithLifecycle()
                if (state == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AppRoot(viewModel, tab)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel, tab: MainTab) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appState = state ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(CsvExporter.export(appState))
            }
            Toast.makeText(context, "Export CSV enregistré", Toast.LENGTH_SHORT).show()
        }
    }
    val weightCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(WeightCsvExporter.export(appState))
            }
            Toast.makeText(context, "Export des pesées enregistré", Toast.LENGTH_SHORT).show()
        }
    }
    val markdownLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(CompleteMarkdownExporter.export(appState, BuildConfig.VERSION_NAME))
            }
            Toast.makeText(context, "Export complet enregistré", Toast.LENGTH_SHORT).show()
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(viewModel.exportBackup())
            }
            Toast.makeText(context, "Sauvegarde enregistrée", Toast.LENGTH_SHORT).show()
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
            if (text == null) Toast.makeText(context, "Fichier illisible", Toast.LENGTH_LONG).show()
            else pendingRestore = text
        }
    }

    val draft = appState.workoutLogs.firstOrNull {
        it.status == fr.suivimuscu.app.data.WorkoutStatus.DRAFT && it.deletedAt == null
    }
    val draftMinimized by viewModel.draftMinimized.collectAsStateWithLifecycle()
    if (draft != null && !draftMinimized) {
        WorkoutScreen(
            state = appState,
            workout = draft,
            viewModel = viewModel,
        )
        return
    }

    var showSettings by remember { mutableStateOf(false) }
    var libraryTab by remember { mutableStateOf(LibraryTab.PROGRAMS) }
    var createTemplateRequest by remember { mutableIntStateOf(0) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.JOURNAL,
                    onClick = { viewModel.tab.value = MainTab.JOURNAL },
                    icon = { Icon(Icons.Default.FitnessCenter, null) },
                    label = { Text("Journal") },
                )
                NavigationBarItem(
                    selected = tab == MainTab.WEIGHT,
                    onClick = { viewModel.tab.value = MainTab.WEIGHT },
                    icon = { Icon(Icons.Default.MonitorWeight, null) },
                    label = { Text("Poids") },
                )
                NavigationBarItem(
                    selected = tab == MainTab.TRENDS,
                    onClick = { viewModel.tab.value = MainTab.TRENDS },
                    icon = { Icon(Icons.Default.ShowChart, null) },
                    label = { Text("Tendances") },
                )
                NavigationBarItem(
                    selected = tab == MainTab.LIBRARY,
                    onClick = { viewModel.tab.value = MainTab.LIBRARY },
                    icon = { Icon(Icons.Default.MenuBook, null) },
                    label = { Text("Bibliothèque") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            MainTab.JOURNAL -> JournalScreen(
                state = appState,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
                onSettings = { showSettings = true },
                onCreateTemplate = {
                    libraryTab = LibraryTab.TEMPLATES
                    createTemplateRequest++
                    viewModel.tab.value = MainTab.LIBRARY
                },
                onOpenPrograms = {
                    libraryTab = LibraryTab.PROGRAMS
                    viewModel.tab.value = MainTab.LIBRARY
                },
                onDeleted = {
                    scope.launch {
                        val result = snackbar.showSnackbar("Séance supprimée", "Annuler", duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                    }
                },
            )
            MainTab.WEIGHT -> WeightScreen(
                state = appState,
                viewModel = viewModel,
                onExport = { weightCsvLauncher.launch("suivi-poids-${LocalDate.now()}.csv") },
                modifier = Modifier.padding(padding),
            )
            MainTab.TRENDS -> TrendsScreen(appState, viewModel, Modifier.padding(padding))
            MainTab.LIBRARY -> LibraryScreen(
                state = appState,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
                selectedTab = libraryTab,
                onTabSelected = { libraryTab = it },
                createTemplateRequest = createTemplateRequest,
                onCreateTemplateHandled = { createTemplateRequest = 0 },
            )
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            icon = { Icon(Icons.Default.Settings, null) },
            title = { Text("Données et fichiers") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ListItem(
                        headlineContent = { Text("Exporter les performances") },
                        supportingContent = { Text("CSV, une ligne par série") },
                        leadingContent = { Icon(Icons.Default.TableView, null) },
                        modifier = Modifier,
                    )
                    Button(onClick = {
                        showSettings = false
                        csvLauncher.launch("suivi-muscu-${LocalDate.now()}.csv")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer le CSV") }
                    OutlinedButton(onClick = {
                        showSettings = false
                        weightCsvLauncher.launch("suivi-poids-${LocalDate.now()}.csv")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.MonitorWeight, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exporter les pesées")
                    }
                    OutlinedButton(onClick = {
                        showSettings = false
                        markdownLauncher.launch("suivi-muscu-complet-${LocalDate.now()}.md")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Description, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exporter tout pour ChatGPT (.md)")
                    }
                    HorizontalDivider()
                    Button(onClick = {
                        showSettings = false
                        backupLauncher.launch("suivi-muscu-backup-${LocalDate.now()}.json")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sauvegarde complète")
                    }
                    OutlinedButton(onClick = {
                        showSettings = false
                        restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restaurer un fichier")
                    }
                    Text(
                        "La restauration remplace toutes les données locales après validation du fichier.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Fermer") } },
        )
    }
    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Remplacer toutes les données ?") },
            text = { Text("La sauvegarde choisie remplacera la bibliothèque, les programmes, les pesées et tout l’historique local.") },
            confirmButton = {
                TextButton(onClick = {
                    val result = viewModel.restoreBackup(backup)
                    pendingRestore = null
                    Toast.makeText(
                        context,
                        if (result.isSuccess) "Sauvegarde restaurée"
                        else "Restauration impossible : ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }) { Text("Restaurer") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Annuler") } },
        )
    }
}
