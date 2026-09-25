package com.client.xvideos.l.featured.saved

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

@Immutable
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
                    runCatchingCancellable { lRestoreSourceToFile(client, media.sourceUrl, media.target) }
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
                    runCatchingCancellable { lRestoreSourceToFile(client, preview.sourceUrl, preview.target) }
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
        if (!lIsInside(folder, mediaTarget) || !isSafeFileName(metadata.mediaFileName)) {
            invalidMetadataFiles++
            return@forEach
        }
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
    val results = mutableListOf<File>()
    val roots = arrayOf(File(AppPath.l_likes), File(AppPath.l_collection))
    for (root in roots) {
        if (!root.exists() || !root.isDirectory) continue
        root.walkTopDown().forEach { file ->
            if (file.isFile && file.name.equals(L_METADATA_FILE_NAME, ignoreCase = true)) {
                results.add(file)
            }
        }
    }
    return results
}

private fun LSavedLikeMetadata.previewRecoveryFiles(folder: File): List<LRecoveryFile> {
    val previews = mutableListOf<LRecoveryFile>()
    previewFiles
        ?.forEach { preview ->
            val target = File(folder, preview.fileName)
            if (lIsInside(folder, target) && isSafeFileName(preview.fileName)) {
                previews.add(LRecoveryFile(target, preview.sourceUrl))
            }
        }

    if (previewFileName != null && sourcePreviewUrl != null) {
        val target = File(folder, previewFileName)
        if (lIsInside(folder, target) && isSafeFileName(previewFileName)) {
            previews.add(LRecoveryFile(target, sourcePreviewUrl))
        }
    }

    return previews.distinctBy { it.target.absolutePath }
}

/**
 * Возвращает файл на место: копирует с диска, если источник локальный, и качает,
 * если это адрес.
 *
 * Разделение по схеме, а не по «нашлось локально» — и это чинит дефект. Раньше
 * локальный путь, файла по которому уже нет (обычное дело после восстановления
 * бэкапа на другом устройстве), проваливался в сетевую ветку. Ktor получал
 * строку без схемы и хоста, достраивал её до `http://localhost/...`, и наружу
 * приходило
 *
 *     CLEARTEXT communication to localhost not permitted by network security policy
 *
 * — сообщение, которое уводит в сторону: запрет открытого HTTP тут ни при чём и
 * снимать его не надо. Теперь такой случай честно говорит, что исходного файла
 * нет, и в сеть не лезет.
 */
private suspend fun lRestoreSourceToFile(client: io.ktor.client.HttpClient, source: String, target: File) {
    val isRemote = source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true)

    if (!isRemote) {
        val localFile = source.lToLocalFileOrNull()
            ?: throw IOException("исходный файл не найден: $source")
        target.parentFile?.mkdirs()
        localFile.copyTo(target, overwrite = true)
        return
    }

    lDownloadToFile(client, source, target)
}

private fun isSafeFileName(name: String): Boolean =
    !name.contains("..") && !name.contains('/') && !name.contains('\\')
