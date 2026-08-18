package com.client.xvideos.common.p2p

import com.client.xvideos.common.io.normalizeRelativePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Описание одного файла бандла.
 *
 * @param name имя файла (для UI/логов).
 * @param relativePath путь относительно корня store (через '/'), задаёт, куда положить файл у получателя.
 * @param payloadId id Nearby-payload'а; одинаков на обоих телефонах, по нему получатель сопоставляет байты.
 * @param size размер в байтах.
 */
@Serializable
data class P2pManifestFile(
    val name: String,
    val relativePath: String,
    val payloadId: Long,
    val size: Long,
)

/**
 * Control-сообщение, которое отправитель шлёт BYTES-payload'ом после всех файлов.
 *
 * @param type источник (определяет store у получателя).
 * @param metadataFileName имя файла-метаданных среди [files] (`metadata.json` / `<id>.info`), или null.
 * @param files список файлов бандла.
 */
@Serializable
data class P2pManifest(
    val type: P2pType,
    val metadataFileName: String?,
    val files: List<P2pManifestFile>,
)

/** Сериализация манифеста для передачи BYTES-payload'ом. */
object P2pManifestCodec {

    /*
     * kotlinx, а не Gson. Манифест приходит с чужого устройства, а Gson не
     * вызывает конструкторы Kotlin и не смотрит на нуллабельность: объект
     * создаётся через Unsafe, поля заполняются рефлексией, и отсутствующее
     * поле остаётся null в non-null типе — падение случалось позже, вдали от
     * разбора. Раньше это компенсировалось руками: три require и
     * @Suppress("SENSELESS_COMPARISON") на каждое поле. Теперь проверку делает
     * сам разбор.
     *
     * ignoreUnknownKeys: бандл из более новой версии приложения может нести
     * поля, которых мы не знаем. Лишнее поле — не повод отказаться от приёма;
     * неизвестный `type` по-прежнему отказ, потому что он решает, в какое
     * хранилище лягут файлы.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(manifest: P2pManifest): String = json.encodeToString(manifest)

    /**
     * Разбирает манифест, пришедший **с чужого устройства**, и проверяет пути.
     *
     * Структуру проверяет kotlinx (отсутствующее поле, неизвестный `type` —
     * `SerializationException`). Остаётся проверка, которую библиотека сделать
     * не может: `relativePath` напрямую задаёт, куда ляжет файл, и без
     * нормализации пир кладёт `../../shared_prefs/...`. Вызывающая сторона
     * (`P2pReceiveController`) оборачивает разбор в `runCatching`.
     */
    fun fromJson(raw: String): P2pManifest {
        val parsed = json.decodeFromString<P2pManifest>(raw)
        parsed.files.forEach { file -> normalizeRelativePath(file.relativePath) }
        return parsed
    }

    fun toBytes(manifest: P2pManifest): ByteArray = toJson(manifest).toByteArray(Charsets.UTF_8)
    fun fromBytes(bytes: ByteArray): P2pManifest = fromJson(String(bytes, Charsets.UTF_8))
}
