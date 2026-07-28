package com.personal.spese.feature.update

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.spese.core.update.UpdateInstaller
import java.io.File

/**
 * Elemento globale (in AppRoot): mostra l'avviso di aggiornamento, gestisce permesso di
 * installazione, download e avvio dell'installer. Usa la stessa istanza di [UpdateViewModel]
 * del bottone manuale in Impostazioni, così esito e dialog sono coerenti da ogni schermata.
 */
@Composable
fun UpdateGate(vm: UpdateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // APK pronto → lancia l'installer di sistema.
    LaunchedEffect(state.readyApk) {
        state.readyApk?.let { path ->
            UpdateInstaller.install(context, File(path))
            vm.consumeReadyApk()
        }
    }

    // Messaggi (esito controllo manuale, errori).
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }

    val info = state.available
    if (info != null && !state.dismissed) {
        AlertDialog(
            onDismissRequest = { if (!state.downloading) vm.dismiss() },
            title = { Text("Aggiornamento disponibile") },
            text = {
                Column {
                    Text("Nuova versione ${info.versionName} (attuale ${vm.currentVersionName}).")
                    if (!info.notes.isNullOrBlank()) {
                        Text(
                            info.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (state.downloading) {
                        Text(
                            "Scaricamento in corso…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.downloading,
                    onClick = {
                        if (UpdateInstaller.canInstall(context)) {
                            vm.downloadAndInstall()
                        } else {
                            UpdateInstaller.requestPermission(context)
                            Toast.makeText(
                                context,
                                "Consenti l'installazione da questa app, poi premi di nuovo.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) { Text(if (state.downloading) "Scaricamento…" else "Scarica e installa") }
            },
            dismissButton = {
                TextButton(enabled = !state.downloading, onClick = { vm.dismiss() }) {
                    Text("Più tardi")
                }
            }
        )
    }
}
