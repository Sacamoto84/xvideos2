package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.AppPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class LDownloadRecoveryReport(
    val totalMetadataFiles: Int = 0,
    val incompleteItems: Int = 0,
    val downloadedMedia: Int = 0,
    val downloadedPreview: Int = 0,
    val invalidMetadataFiles: Int = 0,
    val failedMedia: Int = 0,
    val failedPreview: Int = 0,
    val skippedNoMediaUrl: Int = 0,
    val skippedNoPreviewUrl: Int = 0
)

private data class LRecoveryFile(
    val target: File,
    val sourceUrl: String?
)

private data class LRecoveryCandidate(
    val media: LRecoveryFile?,
    val previews: List<LRecoveryFile>
)

internal suspend fun lRecoverIncompleteSavedMedia(
    onEvent: (String) -> Unit = {}
): LDownloadRecoveryReport = withContext(Dispatchers.IO) {
    val scan = scanLIncompleteSavedMedia()
    var report = scan.first
    onEvent("L: metadata ${report.totalMetadataFiles}, требуют докачки ${report.incompleteItems}")
    val client = lCreateMediaClient()

    try {
        scan.second.forEach { candidate ->
            candidate.media?.let { media ->
                if (media.sourceUrl.isNullOrBlank()) {
                    report = report.copy(skippedNoMediaUrl = report.skippedNoMediaUrl + 1)
                    onEvent("L: нет URL для media ${media.target.name}")
                } else {
                    onEvent("L: скачиваю media ${media.target.name}")
                    runCatching { lRestoreSourceToFile(client, media.sourceUrl, media.target) }
                        .onSuccess {
                            report = report.copy(downloadedMedia = report.downloadedMedia + 1)
                            onEvent("L: media готово ${media.target.name}")
                        }
                        .onFailure {
                            report = report.copy(failedMedia = report.failedMedia + 1)
                            onEvent("L: media не скачан ${media.target.name}: ${it.message ?: it::class.java.simpleName}")
                            Timber.e(it, "L recovery media failed: ${media.target.absolutePath}")
                        }
                }
            }

            candidate.previews.forEach { preview ->
                if (preview.sourceUrl.isNullOrBlank()) {
                    report = report.copy(skippedNoPreviewUrl = report.skippedNoPreviewUrl + 1)
                    onEvent("L: нет URL для preview ${preview.target.name}")
                } else {
                    onEvent("L: скачиваю preview ${preview.target.name}")
                    runCatching { lRestoreSourceToFile(client, preview.sourceUrl, preview.target) }
                        .onSuccess {
                            report = report.copy(downloadedPreview = report.downloadedPreview + 1)
                            onEvent("L: preview готово ${preview.target.name}")
                        }
                        .onFailure {
                            report = report.copy(failedPreview = report.failedPreview + 1)
                            onEvent("L: preview не скачан ${preview.target.name}: ${it.message ?: it::class.java.simpleName}")
                            Timber.e(it, "L recovery preview failed: ${preview.target.absolutePath}")
                        }
                }
            }
        }
    } finally {
        client.close()
    }

    report
}

private fun scanLIncompleteSavedMedia(): Pair<LDownloadRecoveryReport, List<LRecoveryCandidate>> {
    val metadataFiles = lRecoveryMetadataFiles()
    var invalidMetadataFiles = 0
    val candidates = mutableListOf<LRecoveryCandidate>()

    metadataFiles.forEach { metadataFile ->
        val metadata = readLSavedLikeMetadata(metadataFile)
        if (metadata == null) {
            invalidMetadataFiles++
            return@forEach
        }

        val folder = metadataFile.parentFile ?: run {
            invalidMetadataFiles++
            return@forEach
        }

        val mediaTarget = File(folder, metadata.mediaFileName)
        val missingMedia = if (!mediaTarget.exists() || mediaTarget.length() == 0L) {
            LRecoveryFile(mediaTarget, metadata.sourceMediaUrl)
        } else {
            null
        }

        val missingPreviews = metadata.previewRecoveryFiles(folder)
            .filter { !it.target.exists() || it.target.length() == 0L }

        if (missingMedia != null || missingPreviews.isNotEmpty()) {
            candidates.add(LRecoveryCandidate(missingMedia, missingPreviews))
        }
    }

    return LDownloadRecoveryReport(
        totalMetadataFiles = metadataFiles.size,
        incompleteItems = candidates.size,
        invalidMetadataFiles = invalidMetadataFiles
    ) to candidates
}

private fun lRecoveryMetadataFiles(): List<File> {
    return listOf(File(AppPath.l_likes), File(AppPath.l_collection))
        .flatMap { root ->
            if (!root.exists() || !root.isDirectory) {
                emptyList()
            } else {
                root.walkTopDown()
                    .filter { it.isFile && it.name.equals(L_METADATA_FILE_NAME, ignoreCase = true) }
                    .toList()
            }
        }
}

private fun LSavedLikeMetadata.previewRecoveryFiles(folder: File): List<LRecoveryFile> {
    val previews = mutableListOf<LRecoveryFile>()
    previewFiles
        ?.forEach { preview ->
            previews.add(LRecoveryFile(File(folder, preview.fileName), preview.sourceUrl))
        }

    if (previewFileName != null && sourcePreviewUrl != null) {
        previews.add(LRecoveryFile(File(folder, previewFileName), sourcePreviewUrl))
    }

    return previews.distinctBy { it.target.absolutePath }
}

private suspend fun lRestoreSourceToFile(client: io.ktor.client.HttpClient, source: String, target: File) {
    source.lToLocalFileOrNull()?.let { localFile ->
        target.parentFile?.mkdirs()
        localFile.copyTo(target, overwrite = true)
        return
    }
    lDownloadToFile(client, source, target)
}
