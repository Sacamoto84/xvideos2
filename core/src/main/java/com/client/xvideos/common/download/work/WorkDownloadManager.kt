package com.client.xvideos.common.download.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Централизованный менеджер фоновых загрузок на базе WorkManager.
 */
@Singleton
class WorkDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    /**
     * Поставить задачу загрузки в очередь.
     * Возвращает уникальный [UUID] задачи WorkManager.
     */
    fun enqueue(
        request: DownloadWorkRequest,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
    ): UUID {
        Timber.i("WorkDownloadManager: Постановка задачи в очередь: id=${request.id}, url=${request.url}")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(request.networkType)
            .setRequiresCharging(request.requiresCharging)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(request.toWorkData())
            .addTag(MediaDownloadWorker.WORK_TAG_DOWNLOAD)
            .addTag(request.tag)
            .build()

        workManager.enqueueUniqueWork(
            request.id,
            existingWorkPolicy,
            workRequest
        )

        return workRequest.id
    }

    /** Отменить загрузку по UUID задачи WorkManager. */
    fun cancel(workId: UUID) {
        Timber.i("WorkDownloadManager: Отмена загрузки $workId")
        workManager.cancelWorkById(workId)
    }

    /** Отменить загрузку по уникальному ID или тегу. */
    fun cancelByTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        Timber.i("WorkDownloadManager: Отмена загрузок по тегу $trimmed")
        workManager.cancelAllWorkByTag(trimmed)
        workManager.cancelUniqueWork(trimmed)
    }

    /** Отменить все текущие и запланированные загрузки. */
    fun cancelAll() {
        Timber.i("WorkDownloadManager: Отмена всех загрузок")
        workManager.cancelAllWorkByTag(MediaDownloadWorker.WORK_TAG_DOWNLOAD)
    }

    /** Наблюдать за состоянием конкретной загрузки по её UUID. */
    fun observeDownload(workId: UUID): Flow<DownloadWorkState?> {
        return workManager.getWorkInfoByIdFlow(workId)
            .map { info -> info?.let { DownloadWorkState.fromWorkInfo(it) } }
    }

    /** Наблюдать за состоянием загрузок с заданным тегом. */
    fun observeDownloadsByTag(tag: String): Flow<List<DownloadWorkState>> {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return workManager.getWorkInfosByTagFlow(trimmed)
            .map { list -> if (list.isEmpty()) emptyList() else list.map { DownloadWorkState.fromWorkInfo(it) } }
    }

    /** Наблюдать за всеми активными и недавними загрузками. */
    fun observeAllDownloads(): Flow<List<DownloadWorkState>> {
        return workManager.getWorkInfosByTagFlow(MediaDownloadWorker.WORK_TAG_DOWNLOAD)
            .map { list -> if (list.isEmpty()) emptyList() else list.map { DownloadWorkState.fromWorkInfo(it) } }
    }
}
