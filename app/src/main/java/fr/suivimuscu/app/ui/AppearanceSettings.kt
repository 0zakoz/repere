package fr.suivimuscu.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.suivimuscu.app.data.AppThemeId
import fr.suivimuscu.app.data.AppearancePreferences
import fr.suivimuscu.app.data.ThemeMode

private data class ThemeChoice(val id: AppThemeId, val name: String, val description: String)

private val themeChoices = listOf(
    ThemeChoice(AppThemeId.ORIGINAL, "Original", "Graphite & lime"),
    ThemeChoice(AppThemeId.KAWAII, "Kawaii", "Rose, jaune, bleu & mascottes"),
    ThemeChoice(AppThemeId.PASTEL, "Pastel", "Rose, jaune & bleu"),
    ThemeChoice(AppThemeId.OLED, "OLED", "Noir & néons"),
    ThemeChoice(AppThemeId.PURE, "Épuré", "Sobre & premium"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    appearance: AppearancePreferences,
    onThemeSelected: (AppThemeId) -> Unit,
    onModeSelected: (ThemeMode) -> Unit,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Apparence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Choisis une ambiance. Le changement est immédiat et reste propre à ce téléphone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            themeChoices.forEach { choice ->
                ThemePreviewCard(
                    choice = choice,
                    selected = appearance.theme == choice.id,
                    dark = when (appearance.mode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> systemDark
                    },
                    onClick = { onThemeSelected(choice.id) },
                )
            }
        }
        Text("Luminosité", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val modes = listOf(
                ThemeMode.LIGHT to "Clair",
                ThemeMode.DARK to "Sombre",
                ThemeMode.SYSTEM to "Système",
            )
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = appearance.mode == mode,
                    onClick = { onModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(choice: ThemeChoice, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val preview = themeDefinition(choice.id, dark)
    Card(
        modifier = Modifier.width(142.dp).selectable(
            selected = selected,
            onClick = onClick,
            role = Role.RadioButton,
        ),
        shape = preview.shapes.medium,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .45f),
        ),
        colors = CardDefaults.cardColors(containerColor = preview.colors.background),
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PreviewDot(preview.colors.primary)
                    PreviewDot(preview.colors.secondary)
                    PreviewDot(preview.colors.tertiary)
                    if (choice.id == AppThemeId.KAWAII) PreviewDot(preview.visuals.info)
                }
                if (selected) {
                    Icon(Icons.Default.CheckCircle, "Thème sélectionné", tint = preview.colors.primary, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                Modifier.fillMaxWidth().height(34.dp)
                    .clip(preview.shapes.small)
                    .background(preview.colors.surfaceVariant)
                    .border(1.dp, preview.colors.outline.copy(alpha = .35f), preview.shapes.small),
            ) {
                Row(Modifier.align(Alignment.Center).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (choice.id == AppThemeId.KAWAII) {
                        preview.visuals.kawaiiSurfaces.take(3).forEach { color ->
                            Box(Modifier.height(7.dp).weight(1f).clip(CircleShape).background(color))
                            Spacer(Modifier.width(3.dp))
                        }
                        Text("🐰🐼🐱", fontSize = 10.sp)
                        Icon(Icons.Default.Favorite, null, tint = preview.colors.primary, modifier = Modifier.size(12.dp))
                        Icon(Icons.Default.AutoAwesome, null, tint = preview.colors.tertiary, modifier = Modifier.size(12.dp))
                    } else {
                        Box(Modifier.height(5.dp).weight(1f).clip(CircleShape).background(preview.colors.primary))
                    }
                }
            }
            Text(choice.name, color = preview.colors.onBackground, fontWeight = FontWeight.Bold)
            Text(choice.description, color = preview.colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PreviewDot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}
