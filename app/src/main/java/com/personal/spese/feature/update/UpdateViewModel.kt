package com.personal.spese.feature.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.BuildConfig
import com.personal.spese.core.update.UpdateInfo
import com.personal.spese.core.update.UpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val available: UpdateInfo? = null,   // versione più recente trovata sul repo
    val readyApk: String? = null,        // percorso APK scaricato, pronto per l'installazione
    val message: String? = null,         // esito da mostrare (es. "sei aggiornato", errori)
    val dismissed: Boolean = false       // avviso chiuso dall'utente per questa sessione
)

/**
 * Controllo/scarico degli aggiornamenti. Istanza unica ospitata in AppRoot: fa il check
 * automatico all'avvio e serve anche il bottone manuale in Impostazioni.
 */
class UpdateViewModel(
    private val service: UpdateService,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state = _state.asStateFlow()

    val currentVersionName: String = BuildConfig.VERSION_NAME
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE

    init {
        check(manual = false) // controllo automatico all'avvio
    }

    fun check(manual: Boolean) {
        if (_state.value.checking || _state.value.downloading) return
        _state.update { it.copy(checking = true, message = null) }
        viewModelScope.launch {
            val info = runCatching { service.check(currentVersionCode) }.getOrNull()
            _state.update {
                when {
                    info != null -> it.copy(checking = false, available = info, dismissed = false)
                    manual -> it.copy(checking = false, message = "Sei già aggiornato (v$currentVersionName).")
                    else -> it.copy(checking = false)
                }
            }
        }
    }

    fun downloadAndInstall() {
        val info = _state.value.available ?: return
        if (_state.value.downloading) return
        _state.update { it.copy(downloading = true, message = null) }
        viewModelScope.launch {
            val dest = File(appContext.cacheDir, "updates/spese-${info.versionCode}.apk")
            val ok = runCatching { service.download(info.apkUrl, dest) }.getOrDefault(false)
            _state.update {
                if (ok) it.copy(downloading = false, readyApk = dest.absolutePath)
                else it.copy(downloading = false, message = "Download non riuscito. Riprova.")
            }
        }
    }

    fun consumeReadyApk() = _state.update { it.copy(readyApk = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun dismiss() = _state.update { it.copy(available = null, dismissed = true) }
}
