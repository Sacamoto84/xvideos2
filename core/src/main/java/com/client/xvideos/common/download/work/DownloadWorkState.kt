package com.client.xvideos.common.download.work

import androidx.compose.runtime.Immutable
import androidx.work.WorkInfo
import java.util.UUID

@Immutable
enum class DownloadStatus {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    val isFinished: Boolean get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    val isTerminal: Boolean get() = isFinished
    val isRunning: Boolean get() = this == RUNNING
    val isSuccessful: Boolean get() = this == SUCCEEDED
    val isFailed: Boolean get() = this == FAILED
    val isCancelled: Boolean get() = this == CANCELLED
    val isEnqueued: Boolean get() = this == ENQUEUED
    val isPending: Boolean get() = this == ENQUEUED
    val isActive: Boolean get() = this == ENQUEUED || this == RUNNING

    companion object {
        fun fromNameOrDefault(name: String?, default: DownloadStatus = ENQUEUED): DownloadStatus {
            if (name.isNullOrBlank()) return default
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}

@Immutable
data class DownloadWorkState(
    val workId: UUID,
    val tag: String,
    val status: DownloadStatus,
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val filePath: String? = null,
    val error: String? = null,
) {
    val isFinished: Boolean get() = status.isFinished
    val isTerminal: Boolean get() = status.isTerminal
    val isRunning: Boolean get() = status.isRunning
    val isSuccessful: Boolean get() = status.isSuccessful
    val isFailed: Boolean get() = status.isFailed
    val isCancelled: Boolean get() = status.isCancelled
    val isEnqueued: Boolean get() = status.isEnqueued
    val isActive: Boolean get() = status.isActive
    val hasError: Boolean get() = !error.isNullOrBlank()
    val hasFilePath: Boolean get() = !filePath.isNullOrBlank()
    val hasTotalBytes: Boolean get() = totalBytes > 0L
    val remainingBytes: Long get() = if (totalBytes > bytesDownloaded) totalBytes - bytesDownloaded else 0L
    val progressPercent: Int get() = (progressFraction * 100).toInt()
    val hasProgress: Boolean get() = progress > 0 || bytesDownloaded > 0L

    fun withProgress(newProgress: Int, downloaded: Long, total: Long): DownloadWorkState =
        copy(progress = newProgress, bytesDownloaded = downloaded, totalBytes = total)

    val progressFraction: Float
        get() = when {
            status == DownloadStatus.SUCCEEDED -> 1f
            totalBytes > 0L -> (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
            progress > 0 -> (progress / 100f).coerceIn(0f, 1f)
            else -> 0f
        }

    companion object {
        fun fromWorkInfo(workInfo: WorkInfo): DownloadWorkState {
            val status = when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> DownloadStatus.ENQUEUED
                WorkInfo.State.RUNNING -> DownloadStatus.RUNNING
                WorkInfo.State.SUCCEEDED -> DownloadStatus.SUCCEEDED
                WorkInfo.State.FAILED -> DownloadStatus.FAILED
                WorkInfo.State.BLOCKED -> DownloadStatus.ENQUEUED
                WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
            }

            val progressData = workInfo.progress
            val outputData = workInfo.outputData

            val progress = progressData.getInt(DownloadWorkRequest.KEY_PROGRESS, 0)
            val bytesDownloaded = progressData.getLong(DownloadWorkRequest.KEY_BYTES_DOWNLOADED, 0L)
            val totalBytes = progressData.getLong(DownloadWorkRequest.KEY_TOTAL_BYTES, -1L)

            val filePath = outputData.getString(DownloadWorkRequest.KEY_OUTPUT_FILE_PATH)
            val error = outputData.getString(DownloadWorkRequest.KEY_OUTPUT_ERROR)

            val tag = workInfo.tags.firstOrNull { it != MediaDownloadWorker.WORK_TAG_DOWNLOAD }
                ?: workInfo.id.toString()

            return DownloadWorkState(
                workId = workInfo.id,
                tag = tag,
                status = status,
                progress = progress,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                filePath = filePath,
                error = error,
            )
        }
    }
}
