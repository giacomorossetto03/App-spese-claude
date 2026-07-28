package com.personal.spese.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Metadati pubblicati dalla CI accanto all'APK (`version.json`). */
@Serializable
data class ReleaseVersion(
    val versionCode: Int,
    val versionName: String,
    val notes: String? = null
)

/** Info su un aggiornamento disponibile. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String?,
    val apkUrl: String
)

/**
 * Controllo aggiornamenti via GitHub Releases (tag `latest`, marcato come "latest release").
 * Nessun token: si usano i download diretti `releases/latest/download/<asset>`.
 * L'app resta offline per i dati: la rete si tocca solo qui, su richiesta o all'avvio.
 */
class UpdateService(
    owner: String = "giacomorossetto03",
    repo: String = "App-spese-claude"
) {
    private val base = "https://github.com/$owner/$repo/releases/latest/download"
    private val versionUrl = "$base/version.json"
    val apkUrl = "$base/app-release.apk"

    private val json = Json { ignoreUnknownKeys = true }

    /** Info di aggiornamento se la release ha versionCode > [currentCode], altrimenti null. */
    suspend fun check(currentCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        val text = readText(versionUrl) ?: return@withContext null
        val remote = runCatching { json.decodeFromString(ReleaseVersion.serializer(), text) }
            .getOrNull() ?: return@withContext null
        if (remote.versionCode > currentCode) {
            UpdateInfo(remote.versionCode, remote.versionName, remote.notes, apkUrl)
        } else null
    }

    /** Scarica l'APK in [dest]. Ritorna true se completato. */
    suspend fun download(url: String, dest: File): Boolean = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        val conn = openFollowing(url) ?: return@withContext false
        try {
            if (conn.responseCode !in 200..299) return@withContext false
            conn.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun readText(url: String): String? {
        val conn = openFollowing(url) ?: return null
        return try {
            if (conn.responseCode !in 200..299) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** Apre una connessione seguendo manualmente i redirect (i download GitHub cambiano host). */
    private fun openFollowing(startUrl: String, maxRedirects: Int = 5): HttpURLConnection? {
        var current = startUrl
        repeat(maxRedirects) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/octet-stream, application/json, */*")
            }
            when (conn.responseCode) {
                301, 302, 303, 307, 308 -> {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) return null
                    current = loc
                }
                else -> return conn
            }
        }
        return null
    }
}
