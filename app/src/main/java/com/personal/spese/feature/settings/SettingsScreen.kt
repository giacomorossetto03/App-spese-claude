package com.personal.spese.feature.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.spese.core.model.ThemeMode
import com.personal.spese.di.appContainer
import com.personal.spese.di.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onOpenCategories: () -> Unit, onOpenRecurring: () -> Unit) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory { SettingsViewModel(container.settings) })
    val theme by vm.themeMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val jsonText = container.backupManager.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(jsonText.toByteArray()) }
                }
            }.onSuccess { toast(context, "Backup esportato") }
                .onFailure { toast(context, "Errore export: ${it.message}") }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("File non leggibile")
                container.backupManager.importJson(text)
            }.onSuccess { toast(context, "Backup importato") }
                .onFailure { toast(context, "Errore import: ${it.message}") }
        }
    }

    Column(Modifier.padding(vertical = 12.dp)) {
        SectionLabel("Tema")
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = theme == mode, onClick = { vm.setTheme(mode) })
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = theme == mode, onClick = null)
                    Text(mode.label(), Modifier.padding(start = 12.dp))
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingRow(label = "Gestione categorie", onClick = onOpenCategories)
        SettingRow(label = "Spese ricorrenti", onClick = onOpenRecurring)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionLabel("Backup")
        SettingRow(
            label = "Esporta dati (JSON)",
            subtitle = "Salva un backup di tutti i dati",
            onClick = { exportLauncher.launch("spese-backup.json") }
        )
        SettingRow(
            label = "Importa backup",
            subtitle = "Sostituisce i dati attuali con quelli del file",
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        )
        SettingRow(
            label = "Reset dati",
            subtitle = "Cancella tutto e ripristina le categorie predefinite",
            onClick = { confirmReset = true }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Azzerare tutti i dati?") },
            text = { Text("Verranno eliminate spese, rate e ricorrenti. Le categorie tornano a quelle predefinite. Operazione non reversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        runCatching { container.backupManager.resetToDefaults() }
                            .onSuccess { toast(context, "Dati azzerati") }
                            .onFailure { toast(context, "Errore reset: ${it.message}") }
                    }
                }) { Text("Azzera") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annulla") } }
        )
    }
}

private fun toast(context: Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingRow(
    label: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (enabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.LIGHT -> "Chiaro"
    ThemeMode.DARK -> "Scuro"
    ThemeMode.SYSTEM -> "Sistema"
}
