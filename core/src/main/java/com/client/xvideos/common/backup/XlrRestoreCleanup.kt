package com.client.xvideos.common.backup

import timber.log.Timber
import java.io.File

/**
 * Уборка прежней копии раздела, отодвинутой на время восстановления: что из неё
 * переезжает в восстановленный раздел, а что удаляется вместе с ней.
 *
 * Зовётся из [XlrBackupManager] дважды: сразу после переноса всех путей и при
 * следующем запуске, если процесс убили посреди уборки.
 */
internal object XlrRestoreCleanup {

    /** Суффиксы временных файлов загрузчиков: KDownloader, сохранение L, WorkManager. */
    private val PARTIAL_DOWNLOAD_SUFFIXES = listOf(".temp", ".part", ".tmp")

    /**
     * Уборка одного пути после переноса: из прежней копии забирается то, что
     * положено по режиму [mode], сама копия удаляется.
     *
     * @return `false`, если при объединении прежние данные перенесены не все.
     * Копия тогда остаётся на диске, а с ней и журнал — уборку повторит
     * следующий запуск. Удалить её здесь значило бы потерять записи, которые
     * пользователь просил сохранить.
     */
    fun finishPath(root: File, target: File, aside: File, mode: XlrRestoreMode): Boolean {
        if (!aside.exists()) return true
        val moved = runCatching {
            when (mode) {
                XlrRestoreMode.MERGE -> keepLocalContent(aside, target)
                XlrRestoreMode.REPLACE -> {
                    val relativeRoot = target.canonicalFile.relativeTo(root).invariantSeparatorsPath
                    carryOverDownloadedMedia(aside, target, relativeRoot)
                }
            }
        }.onFailure { Timber.e(it, "XlrBackupManager: перенос прежних файлов не удался") }
        if (mode == XlrRestoreMode.MERGE && moved.isFailure) return false
        aside.deleteRecursively()
        return true
    }

    /**
     * Возвращает из прежней копии [aside] в восстановленный [target] всё, чего
     * в архиве не было: записи, сохранённые после бэкапа, скачанные медиа,
     * пустые папки. Данные лежат по файлу на запись, поэтому объединение папок
     * и есть объединение списков. Файл, пришедший из архива, не заменяется.
     */
    private fun keepLocalContent(aside: File, target: File) {
        aside.walkTopDown().toList().forEach { old ->
            val restored = File(target, old.relativeTo(aside).path)
            when {
                old.isDirectory -> restored.mkdirs()
                isPartialDownloadName(old.name) || restored.exists() -> Unit
                else -> {
                    restored.parentFile?.mkdirs()
                    if (!old.renameTo(restored)) old.copyTo(restored, overwrite = false)
                }
            }
        }
    }

    /**
     * Переносит из прежней копии раздела [aside] в восстановленный [target]
     * уже скачанные медиа.
     *
     * Бэкап в режиме MINI несёт только метаданные. Раздел заменялся целиком, и
     * скачанные файлы удалялись вместе с прежней копией — после восстановления
     * всё качалось заново. Переезжают только файлы, которых MINI-бэкап не несёт,
     * и только для элементов, чьи метаданные есть в восстановленном разделе:
     * элементы, которых в бэкапе нет, удаляются, как и раньше. Файл, пришедший
     * из бэкапа, важнее скачанного и не заменяется.
     *
     * @param relativeRoot Путь [target] относительно корня данных, например `R`.
     */
    private fun carryOverDownloadedMedia(aside: File, target: File, relativeRoot: String) {
        aside.walkTopDown().filter { it.isFile }.toList().forEach { old ->
            val relative = old.relativeTo(aside).invariantSeparatorsPath
            val entryName = "$relativeRoot/$relative"
            if (!isMiniOmittedMedia(entryName) || isPartialDownloadName(old.name)) return@forEach
            val restored = File(target, relative)
            val restoredDir = restored.parentFile ?: return@forEach
            if (restored.exists() || !hasRestoredItemMetadata(restoredDir, old, entryName)) return@forEach
            if (!old.renameTo(restored)) old.copyTo(restored, overwrite = false)
        }
    }

    /** Файл, который полный бэкап несёт, а MINI — нет: медиа элемента без его метаданных. */
    private fun isMiniOmittedMedia(entryName: String): Boolean =
        XlrBackupManager.shouldIncludeBackupEntry(entryName, XlrBackupOptions.FULL) &&
            !XlrBackupManager.shouldIncludeBackupEntry(entryName, XlrBackupOptions.MINI)

    /** Остаток прерванной загрузки: переносить его незачем. */
    private fun isPartialDownloadName(name: String): Boolean =
        PARTIAL_DOWNLOAD_SUFFIXES.any { name.endsWith(it, ignoreCase = true) }

    /**
     * Есть ли в восстановленной папке метаданные элемента, которому принадлежит
     * [old]: в загрузках R это `<имя>.info` рядом, в L — `metadata.json` папки.
     */
    private fun hasRestoredItemMetadata(restoredDir: File, old: File, entryName: String): Boolean =
        if (with(XlrBackupManager) { entryName.isInsideBackupPath(R_DOWNLOAD_PATH) }) {
            File(restoredDir, "${old.nameWithoutExtension}.info").isFile
        } else {
            File(restoredDir, XlrBackupManager.L_METADATA_FILE_NAME).isFile
        }
}
