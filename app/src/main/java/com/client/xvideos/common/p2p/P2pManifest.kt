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

    /**
     * Разбирает манифест, пришедший **с чужого устройства**, и проверяет его.
     *
     * Gson не вызывает конструктор Kotlin и не смотрит на нуллабельность: у
     * класса без значений по умолчанию у всех полей объект создаётся через
     * `Unsafe`, а поля заполняются рефлексией. Неизвестное значение `type`
     * (например, из более новой версии приложения) или отсутствующий `files`
     * дают `null` в non-null поле — и падение случается позже, вдали от разбора.
     *
     * Значения по умолчанию здесь были бы хуже проверки: `type` решает, в какое
     * хранилище лягут файлы, и подстановка умолчания молча уложила бы чужой
     * бандл не туда. Поэтому битый манифест — ошибка; вызывающая сторона
     * (`P2pReceiveController`) уже оборачивает разбор в `runCatching`.
     */
    fun fromJson(json: String): P2pManifest {
        val parsed = gson.fromJson(json, P2pManifest::class.java)
            ?: error("P2P-манифест: пустой JSON")

        @Suppress("SENSELESS_COMPARISON")
        require(parsed.type != null) { "P2P-манифест: неизвестный или отсутствующий type" }

        @Suppress("SENSELESS_COMPARISON")
        require(parsed.files != null) { "P2P-манифест: отсутствует список files" }

        parsed.files.forEach { file ->
            @Suppress("SENSELESS_COMPARISON")
            require(file.name != null && file.relativePath != null) {
                "P2P-манифест: у файла нет имени или пути"
            }
        }

        return parsed
    }

    fun toBytes(manifest: P2pManifest): ByteArray = toJson(manifest).toByteArray(Charsets.UTF_8)
    fun fromBytes(bytes: ByteArray): P2pManifest = fromJson(String(bytes, Charsets.UTF_8))
}
