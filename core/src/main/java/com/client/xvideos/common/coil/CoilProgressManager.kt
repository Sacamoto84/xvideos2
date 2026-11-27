package com.client.xvideos.common.coil

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.Snapshot

object CoilProgressManager {
    private val writeLock = Any()

    // Это mutableStateMapOf — Compose будет отслеживать изменения!
    private val _progressMap = mutableStateMapOf<String, CoilProgressItem>()

    // Публичный read-only доступ (чтобы нельзя было случайно изменить снаружи)
    val progressMap: Map<String, CoilProgressItem> = _progressMap

    /**
     * Обновление из ProgressInterceptor — с сетевых потоков OkHttp; синхронизация +
     * snapshot-обновление защищают карту от гонок между фоновыми потоками и Compose.
     */
    fun updateProgress(
        url: String,
        bytes: Long,
        total: Long,
        done: Boolean = false
    ) {
        synchronized(writeLock) {
            mutateSnapshotSafely {
                if (done) {
                    _progressMap.remove(url)
                } else {
                    _progressMap[url] = CoilProgressItem(url, bytes, total, done = false)
                }
            }
        }
    }

    fun clear(url: String) {
        synchronized(writeLock) {
            mutateSnapshotSafely { _progressMap.remove(url) }
        }
    }

    fun clearAll() {
        synchronized(writeLock) {
            mutateSnapshotSafely { _progressMap.clear() }
        }
    }

    private inline fun mutateSnapshotSafely(crossinline block: () -> Unit) {
        runCatching {
            Snapshot.withMutableSnapshot { block() }
        }.onFailure {
            block()
        }
    }
}
