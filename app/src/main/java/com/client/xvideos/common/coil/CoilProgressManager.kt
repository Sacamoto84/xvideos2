package com.client.xvideos.common.coil

import androidx.compose.runtime.mutableStateMapOf

object CoilProgressManager {
    // Это mutableStateMapOf — Compose будет отслеживать изменения!
    private val _progressMap = mutableStateMapOf<String, CoilProgressItem>()

    // Публичный read-only доступ (чтобы нельзя было случайно изменить снаружи)
    val progressMap: Map<String, CoilProgressItem> = _progressMap

    // Обновление из ProgressInterceptor (вызывается в фоновом потоке — безопасно)
    fun updateProgress(
        url: String,
        bytes: Long,
        total: Long,
        done: Boolean = false
    ) {
        // SnapshotStateMap thread-safe для чтения/записи
        _progressMap[url] = CoilProgressItem(url, bytes, total, done)
    }

    // Опционально: очистка
    fun clear(url: String) { _progressMap.remove(url) }

    fun clearAll() {
        _progressMap.clear()
    }
}
