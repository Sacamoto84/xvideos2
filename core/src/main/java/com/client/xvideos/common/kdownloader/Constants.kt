package com.client.xvideos.common.kdownloader

/**
 * Общие константы сетевого протокола и тайм-аутов модуля загрузки [KDownloader].
 */
object Constants {
    /** HTTP-заголовок диапазона байт для докачки файлов. */
    const val RANGE = "Range"

    /** HTTP-заголовок сущности (ETag) для проверки неизменности файла при возобновлении загрузки. */
    const val ETAG = "ETag"

    /** Заголовок клиента User-Agent. */
    const val USER_AGENT = "User-Agent"

    /** Значение User-Agent по умолчанию, используемое при отсутствии кастомного заголовка. */
    const val DEFAULT_USER_AGENT = "KDownloader"

    /** Таймаут чтения сокета по умолчанию (20 секунд). */
    const val DEFAULT_READ_TIMEOUT_IN_MILLS = 20000

    /** Таймаут установки TCP-соединения по умолчанию (20 секунд). */
    const val DEFAULT_CONNECT_TIMEOUT_IN_MILLS = 20000

    /** HTTP статус 416 (Range Not Satisfiable): смещение диапазона выходит за пределы размера ресурса. */
    const val HTTP_RANGE_NOT_SATISFIABLE = 416

    /** HTTP статус 307 (Temporary Redirect): временное перенаправление с сохранением метода запроса. */
    const val HTTP_TEMPORARY_REDIRECT = 307

    /** HTTP статус 308 (Permanent Redirect): постоянное перенаправление с сохранением метода запроса. */
    const val HTTP_PERMANENT_REDIRECT = 308
}
