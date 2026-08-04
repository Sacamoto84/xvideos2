package com.client.xvideos.common.settings.ui


import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.client.xvideos.l.featured.saved.LDownloadRecoveryReport
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport

internal fun redDownloadRecoveryText(
    report: RedDownloadRecoveryReport?,
    isWorking: Boolean
): String {
    if (isWorking) return "Сканирование .info и запуск недостающих загрузок"
    if (report == null) return "Если после backup есть только .info, скачает недостающие mp4/jpg"
    if (report.incompleteItems == 0) return "Все файлы на месте: ${report.totalInfoFiles} info"
    return "Найдено ${report.incompleteItems} из ${report.totalInfoFiles} • видео ${report.queuedVideo} • превью ${report.queuedPreview}"
}

internal fun redDownloadRecoveryConsoleText(report: RedDownloadRecoveryReport): String {
    return listOf(
        "---------",
        "R итог",
        "Info: всего ${report.totalInfoFiles}, неполных ${report.incompleteItems}",
        "Скачано/очередь: видео ${report.queuedVideo}, preview ${report.queuedPreview}",
        "Пропущено: нет video URL ${report.skippedNoVideoUrl}, нет preview URL ${report.skippedNoPreviewUrl}",
        "Ошибки: битых info ${report.invalidInfoFiles}"
    ).joinToString("\n")
}

internal fun lDownloadRecoveryConsoleText(report: LDownloadRecoveryReport): String {
    return listOf(
        "---------",
        "L итог",
        "Metadata: всего ${report.totalMetadataFiles}, неполных ${report.incompleteItems}",
        "Скачано: media ${report.downloadedMedia}, preview ${report.downloadedPreview}",
        "Пропущено: нет media URL ${report.skippedNoMediaUrl}, нет preview URL ${report.skippedNoPreviewUrl}",
        "Ошибки: media ${report.failedMedia}, preview ${report.failedPreview}, битых metadata ${report.invalidMetadataFiles}"
    ).joinToString("\n")
}

internal fun shouldAutoRecoverL(selectedPaths: Set<String>): Boolean {
    return selectedPaths.any { path ->
        path == "L" || path == "L/Likes" || path.startsWith("L/Likes/") ||
                path == "L/Collection" || path.startsWith("L/Collection/")
    }
}

internal fun shouldAutoRecoverRedDownload(selectedPaths: Set<String>): Boolean {
    return selectedPaths.any { path ->
        path == "R" || path == "R/Download" || path.startsWith("R/Download/")
    }
}
