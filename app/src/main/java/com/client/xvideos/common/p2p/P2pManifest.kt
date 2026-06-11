package com.client.xvideos.common.p2p

import com.google.gson.Gson

/**
 * Описание одного файла бандла.
 *
 * @param name имя файла (для UI/логов).
 * @param relativePath путь относительно корня store (через '/'), задаёт, куда положить файл у получателя.
 * @param payloadId id Nearby-payload'а; одинаков на обоих телефонах, по нему получатель сопоставляет байты.
 * @param size размер в байтах.
 */
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
data class P2pManifest(
    val type: P2pType,
    val metadataFileName: String?,
    val files: List<P2pManifestFile>,
)

/** Сериализация манифеста для передачи BYTES-payload'ом. */
object P2pManifestCodec {
    private val gson = Gson()

    fun toJson(manifest: P2pManifest): String = gson.toJson(manifest)
    fun fromJson(json: String): P2pManifest = gson.fromJson(json, P2pManifest::class.java)
    fun toBytes(manifest: P2pManifest): ByteArray = toJson(manifest).toByteArray(Charsets.UTF_8)
    fun fromBytes(bytes: ByteArray): P2pManifest = fromJson(String(bytes, Charsets.UTF_8))
}
