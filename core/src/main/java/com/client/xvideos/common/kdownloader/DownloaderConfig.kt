package com.client.xvideos.common.kdownloader

/**
 * Конфигурация параметров работы загрузчика [KDownloader].
 *
 * @property databaseEnabled Флаг включения SQLite-хранилища для сохранения прогресса и возобновления загрузок.
 * @property connectTimeOut Таймаут установки соединения в миллисекундах.
 * @property readTimeOut Таймаут чтения данных из сетевого потока в миллисекундах.
 */
data class DownloaderConfig(
    var databaseEnabled: Boolean = false,
    var connectTimeOut: Int = Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS,
    var readTimeOut: Int = Constants.DEFAULT_READ_TIMEOUT_IN_MILLS
) {
    /**
     * Проверяет, используются ли стандартные значения тайм-аутов (20 секунд).
     * Введено в Batch 54 для быстрой валидации дефолтных настроек без ручных проверок.
     */
    val isDefaultTimeouts: Boolean
        get() = connectTimeOut == Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS &&
            readTimeOut == Constants.DEFAULT_READ_TIMEOUT_IN_MILLS

    companion object {
        /** Конфигурация по умолчанию: БД выключена, стандартные таймауты. */
        val DEFAULT = DownloaderConfig()
    }
}
