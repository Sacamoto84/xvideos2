package com.client.xvideos.common.coil

import androidx.compose.runtime.mutableStateMapOf

object CoilProgressManager {
    // Это mutableStateMapOf — Compose будет отслеживать изменения!
    private val _progressMap = mutableStateMapOf<String, CoilProgressItem>()

    // Публичный read-only доступ (чтобы нельзя было случайно изменить снаружи)
    val progressMap: Map<String, CoilProgressItem> = _progressMap

    /**
     * Обновление из ProgressInterceptor — с сетевых потоков OkHttp; SnapshotStateMap
     * допускает запись из любого потока.
     *
     * Законченная загрузка ([done]: тело дочитано или закрыто недочитанным) из карты
     * убирается: прогресс нужен, только пока картинка грузится. Раньше записи не
     * удалял никто, и карта росла на каждую картинку из сети до конца процесса
     * (проход 206, T1).
     */
    fun updateProgress(
        url: String,
        bytes: Long,
        total: Long,
        done: Boolean = false
    ) {
        if (done) {
            _progressMap.remove(url)
        } else {
            _progressMap[url] = CoilProgressItem(url, bytes, total, done = false)
        }
    }

    // Опционально: очистка
    fun clear(url: String) { _progressMap.remove(url) }

    fun clearAll() {
        _progressMap.clear()
    }
}
