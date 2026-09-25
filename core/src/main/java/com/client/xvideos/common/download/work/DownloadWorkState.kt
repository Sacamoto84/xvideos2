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
    CANCELLED,
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
    val isFinished: Boolean
        get() = when (status) {
            DownloadStatus.SUCCEEDED, DownloadStatus.FAILED, DownloadStatus.CANCELLED -> true
            DownloadStatus.ENQUEUED, DownloadStatus.RUNNING -> false
        }

    val isRunning: Boolean get() = status == DownloadStatus.RUNNING
    val isSuccessful: Boolean get() = status == DownloadStatus.SUCCEEDED
    val isFailed: Boolean get() = status == DownloadStatus.FAILED
    val isCancelled: Boolean get() = status == DownloadStatus.CANCELLED

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
