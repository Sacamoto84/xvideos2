package com.client.xvideos.common.download.work

import androidx.work.Data
import androidx.work.NetworkType

import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.json.AppJsonCompact

/**
 * Описание задачи загрузки медиа-файла через WorkManager.
 *
 * @property id Уникальный строковый идентификатор загрузки (например, ID видео/гифки).
 * @property url Прямая ссылка для скачивания.
 * @property destDir Целевая папка для сохранения.
 * @property fileName Имя сохраняемого файла (например, "123.mp4").
 * @property title Заголовок для уведомления в шторке.
 * @property tag Дополнительный тег для группировки в WorkManager (по умолчанию равен id).
 * @property metaContent Опциональный JSON с метаданными, сохраняемый рядом в .info файл.
 * @property metaFileName Имя файла метаданных (например, "123.info").
 * @property headers Кастомные HTTP заголовки (Referer, User-Agent и т.д.).
 * @property networkType Требуемый тип сети (по умолчанию [NetworkType.CONNECTED]).
 * @property requiresCharging Требуется ли подключение к зарядному устройству.
 */
data class DownloadWorkRequest(
    val id: String,
    val url: String,
    val destDir: String,
    val fileName: String,
    val title: String,
    val tag: String = id,
    val metaContent: String? = null,
    val metaFileName: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val networkType: NetworkType = NetworkType.CONNECTED,
    val requiresCharging: Boolean = false,
) {
    init {
        require(!isUnsafeItemName(fileName)) { "Небезопасное имя файла: $fileName" }
        metaFileName?.let {
            require(!isUnsafeItemName(it)) { "Небезопасное имя файла метаданных: $it" }
        }
    }

    fun toWorkData(): Data {
        val builder = Data.Builder()
            .putString(KEY_ID, id)
            .putString(KEY_URL, url)
            .putString(KEY_DEST_DIR, destDir)
            .putString(KEY_FILE_NAME, fileName)
            .putString(KEY_TITLE, title)
            .putString(KEY_TAG, tag)

        metaContent?.let { builder.putString(KEY_META_CONTENT, it) }
        metaFileName?.let { builder.putString(KEY_META_FILE_NAME, it) }

        if (headers.isNotEmpty()) {
            val serializedHeaders = AppJsonCompact.encodeToString(headers)
            builder.putString(KEY_HEADERS, serializedHeaders)
        }

        return builder.build()
    }

    companion object {
        const val KEY_ID = "download_id"
        const val KEY_URL = "download_url"
        const val KEY_DEST_DIR = "download_dest_dir"
        const val KEY_FILE_NAME = "download_file_name"
        const val KEY_TITLE = "download_title"
        const val KEY_TAG = "download_tag"
        const val KEY_META_CONTENT = "download_meta_content"
        const val KEY_META_FILE_NAME = "download_meta_file_name"
        const val KEY_HEADERS = "download_headers"

        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_OUTPUT_FILE_PATH = "file_path"
        const val KEY_OUTPUT_ERROR = "error_message"

        fun parseHeaders(headersString: String?): Map<String, String> {
            if (headersString.isNullOrBlank()) return emptyMap()
            val trimmed = headersString.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val parsed = runCatching {
                    AppJsonCompact.decodeFromString<Map<String, String>>(trimmed)
                }.getOrNull()
                if (parsed != null) return parsed
            }
            return trimmed.split(";")
                .mapNotNull { entry ->
                    val split = entry.split("=", limit = 2)
                    if (split.size == 2) split[0].trim() to split[1].trim() else null
                }
                .toMap()
        }
    }
}
