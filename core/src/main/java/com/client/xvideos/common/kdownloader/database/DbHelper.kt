package com.client.xvideos.common.kdownloader.database

/**
 * Интерфейс персистентного хранилища состояния задач [com.client.xvideos.common.kdownloader.KDownloader].
 *
 * Все методы выполняются в корутинном контексте (suspend) для гарантированного отсутствия
 * блокировок вызывающих потоков.
 */
interface DbHelper {

    /**
     * Поиск записи загрузки по её идентификатору.
     *
     * @param id Уникальный целочисленный идентификатор загрузки.
     * @return [DownloadModel] с сохраненными параметрами или null, если запись отсутствует.
     */
    suspend fun find(id: Int): DownloadModel?

    /**
     * Вставка новой записи загрузки в хранилище.
     *
     * @param model Сущность создаваемой загрузки.
     */
    suspend fun insert(model: DownloadModel)

    /**
     * Полное обновление метаданных существующей загрузки (URL, ETag, пути, прогресс).
     *
     * @param model Обновленная модель загрузки.
     */
    suspend fun update(model: DownloadModel)

    /**
     * Оптимизированное частичное обновление прогресса (только байты и временная метка).
     * Используется во время активного скачивания для снижения нагрузки на БД.
     *
     * @param id Идентификатор загрузки.
     * @param downloadedBytes Текущее количество скачанных байт.
     * @param lastModifiedAt Время последнего обновления (Unix timestamp).
     */
    suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long)

    /**
     * Удаление записи о загрузке из базы данных.
     *
     * @param id Идентификатор удаляемой загрузки.
     */
    suspend fun remove(id: Int)

    /**
     * Получение списка устаревших/незавершенных записей, не обновлявшихся более указанного числа дней.
     *
     * @param days Количество дней неактивности.
     * @return Список устаревших моделей для очистки.
     */
    suspend fun getUnwantedModels(days: Int): List<DownloadModel>?

    /**
     * Полная очистка таблицы загрузок (удаление всех записей).
     */
    suspend fun empty()

}
