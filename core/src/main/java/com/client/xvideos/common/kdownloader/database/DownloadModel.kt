package com.client.xvideos.common.kdownloader.database

/**
 * Сущность записи о загружаемом файле в локальной базе данных [AppDbHelper].
 *
 * Хранит состояние загрузки между перезапусками приложения для поддержки докачки (HTTP Resume).
 *
 * @property id Уникальный целочисленный идентификатор загрузки (хэш от URL и пути файла).
 * @property url Исходный сетевой адрес загружаемого ресурса.
 * @property eTag HTTP ETag сервера для проверки валидности частичных данных.
 * @property dirPath Целевой каталог сохранения файла на устройстве.
 * @property fileName Имя файла назначения.
 * @property totalBytes Общий размер файла в байтах (из заголовка Content-Length).
 * @property downloadedBytes Количество фактически сохраненных на диск байт.
 * @property lastModifiedAt Метка времени последнего изменения записи (Unix timestamp в мс).
 */
data class DownloadModel(
    var id: Int = 0,
    var url: String = "",
    var eTag: String = "",
    var dirPath: String = "",
    var fileName: String = "",
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var lastModifiedAt: Long = 0
) {
    /**
     * Проверяет базовую корректность модели: положительный ID и непустой URL.
     * Защита от дефектных или неинициализированных записей в БД.
     */
    val isValid: Boolean get() = id > 0 && url.isNotBlank()

    /** Истина, если сервер вернул корректный размер файла (Content-Length > 0). */
    val hasTotalBytes: Boolean get() = totalBytes > 0L

    /** Оставшееся количество байт до полного завершения скачивания. */
    val remainingBytes: Long get() = if (totalBytes > downloadedBytes) totalBytes - downloadedBytes else 0L

    /**
     * Прогресс скачивания в виде доли от 0.0f до 1.0f.
     * Автоматически ограничивается диапазоном [0..1] для предотвращения аномалий UI при скачках счетчиков.
     */
    val progressFraction: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        const val ID = "id"
        const val URL = "url"
        const val ETAG = "etag"
        const val DIR_PATH = "dir_path"
        const val FILE_NAME = "file_name"
        const val TOTAL_BYTES = "total_bytes"
        const val DOWNLOADED_BYTES = "downloaded_bytes"
        const val LAST_MODIFIED_AT = "last_modified_at"
    }
}
