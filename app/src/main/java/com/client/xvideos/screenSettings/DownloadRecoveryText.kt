package com.client.xvideos.screenSettings

import com.client.xvideos.l.featured.saved.LDownloadRecoveryReport
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport

/**
 * Формирует компактный статус процесса восстановления загрузок RedGifs для отображения в плашке UI.
 *
 * @param report Результат сканирования/восстановления [RedDownloadRecoveryReport].
 * @param isWorking Выполняется ли сейчас фоновое сканирование.
 * @return Человекочитаемая строка статуса для пользователя.
 */
internal fun redDownloadRecoveryText(
    report: RedDownloadRecoveryReport?,
    isWorking: Boolean
): String {
    if (isWorking) return "Сканирование .info и запуск недостающих загрузок"
    if (report == null) return "Если после backup есть только .info, скачает недостающие mp4/jpg"
    if (report.incompleteItems == 0) return "Все файлы на месте: ${report.totalInfoFiles} info"
    return "Найдено ${report.incompleteItems} из ${report.totalInfoFiles} • видео ${report.queuedVideo} • превью ${report.queuedPreview}"
}

/**
 * Формирует детальный многострочный отчет восстановления RedGifs для вывода в консоль/лог диалога.
 *
 * @param report Итоговый отчет восстановления RedGifs.
 * @return Форматированный текст с разделением по строкам.
 */
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

/**
 * Формирует детальный многострочный отчет восстановления Luscious для вывода в консоль/лог диалога.
 *
 * @param report Итоговый отчет восстановления Luscious.
 * @return Форматированный текст с разделением по строкам.
 */
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

/**
 * Определяет, требуется ли автовосстановление медиа Luscious при восстановлении бэкапа по набору выбранных путей [selectedPaths].
 */
internal fun shouldAutoRecoverL(selectedPaths: Set<String>): Boolean {
    return selectedPaths.any { path ->
        path == "L" || path == "L/Likes" || path.startsWith("L/Likes/") ||
                path == "L/Collection" || path.startsWith("L/Collection/")
    }
}

/**
 * Определяет, требуется ли автовосстановление медиа RedGifs при восстановлении бэкапа по набору выбранных путей [selectedPaths].
 */
internal fun shouldAutoRecoverRedDownload(selectedPaths: Set<String>): Boolean {
    return selectedPaths.any { path ->
        path == "R" || path == "R/Download" || path.startsWith("R/Download/")
    }
}
