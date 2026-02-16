package com.location_tracker.data.cache

import com.location_tracker.entity.LocationData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLByAppendingPathComponent
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL

/**
 * Cache persistente (em arquivo JSON) para localizações pendentes no iOS.
 *
 * Objetivo:
 * - Garantir que pontos coletados enquanto o dispositivo estiver offline
 *   não sejam perdidos ao fechar o app.
 * - Permitir que a sincronização em background (ex.: `BGAppRefreshTask` no Swift) consiga ler as
 *   pendências e tentar reenviar quando houver internet.
 *
 * Onde salva:
 * - Arquivo `pending_locations.json` dentro do diretório Documents.
 * - Se o diretório Documents não estiver disponível por algum motivo, usa o diretório temporário.
 *
 * Observações:
 * - As operações são simples: lê o arquivo inteiro, altera a lista e regrava.
 * - Em caso de erro de leitura/parse, retorna uma lista vazia (fail-safe).
 */
class JsonFileLocationCacheIos : LocationCache {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        }

    private val fileUrl: NSURL by lazy {
        val fm = NSFileManager.defaultManager
        val documents = fm.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
        val base = documents ?: NSURL.fileURLWithPath(NSTemporaryDirectory())
        base.URLByAppendingPathComponent("pending_locations.json") ?: base
    }

    override fun saveLocation(location: LocationData) {
        val current = readAllMutable()
        current.add(location)
        writeAll(current)
    }

    override fun getPendingLocations(): List<LocationData> = readAllMutable().toList()

    override fun removeLocations(locationIds: List<String>) {
        val ids = locationIds.toSet()
        val current = readAllMutable()
        current.removeAll { it.id in ids }
        writeAll(current)
    }

    override fun clearAll() {
        writeAll(emptyList())
    }

    override fun getPendingCount(): Int = readAllMutable().size

    private fun readAllMutable(): MutableList<LocationData> {
        val text = readText() ?: return mutableListOf()
        return runCatching { json.decodeFromString<List<LocationData>>(text).toMutableList() }
            .getOrDefault(mutableListOf())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readText(): String? =
        runCatching {
            NSString.stringWithContentsOfURL(fileUrl, NSUTF8StringEncoding, null) as String
        }.getOrNull()

    private fun writeAll(locations: List<LocationData>) {
        val content = json.encodeToString(locations)
        val data: NSData? = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        if (data != null) {
            runCatching { data.writeToURL(fileUrl, true) }
        }
    }
}
