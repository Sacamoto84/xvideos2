package com.client.xvideos.common.backup

import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Переезд сохранённого XLive из `X/Live` в собственную папку `XLive`.
 *
 * Раньше «Мои» и «История» XLive лежали внутри X, чтобы ехать в резервной
 * копии с его секцией. Теперь XLive — отдельный раздел копии со своей папкой.
 * Перенос нужен дважды: при первом обращении к разделу после обновления и
 * после восстановления старого архива, который кладёт данные в `X/Live`.
 */
object XLiveLegacyFolder {

    /** Прежнее место относительно корня хранилища. */
    const val LEGACY = "X/Live"

    /** Новое место относительно корня хранилища. */
    const val CURRENT = "XLive"

    private val FOLDERS = listOf("Saved", "History")

    /**
     * Переносит `X/Live/Saved` и `X/Live/History` в `XLive` и удаляет `X/Live`.
     *
     * [XlrRestoreMode.MERGE] переносит файлы, которых в новом месте нет;
     * совпавший по имени остаётся новым. [XlrRestoreMode.REPLACE] заменяет
     * папку нового места целиком — только ту, что есть в старом месте.
     *
     * @return `true`, если старое место было и что-то перенесено.
     */
    fun move(mainRoot: File, mode: XlrRestoreMode): Boolean {
        val legacy = File(mainRoot, LEGACY)
        if (!legacy.isDirectory) return false
        var moved = false
        var failed = false
        for (name in FOLDERS) {
            val from = File(legacy, name)
            if (!from.isDirectory) continue
            val to = File(mainRoot, "$CURRENT/$name")
            if (mode == XlrRestoreMode.REPLACE) to.deleteRecursively()
            to.mkdirs()
            from.listFiles()?.forEach { file ->
                val target = File(to, file.name)
                if (target.exists()) return@forEach
                if (moveFile(file, target)) moved = true else failed = true
            }
        }
        // Файл, который не перенёсся, остаётся на старом месте: следующий
        // запуск попробует снова. Удалить его — значит потерять комнату.
        if (failed) {
            Timber.w("XLive: перенесено не всё, прежняя папка оставлена")
        } else if (!legacy.deleteRecursively()) {
            Timber.w("XLive: прежняя папка удалена не целиком")
        }
        return moved
    }

    private fun moveFile(from: File, to: File): Boolean {
        if (from.renameTo(to)) return true
        return try {
            from.copyRecursively(to, overwrite = false)
            from.deleteRecursively()
            true
        } catch (e: IOException) {
            Timber.w("XLive: файл не перенесён — %s", e.javaClass.simpleName)
            false
        }
    }
}
