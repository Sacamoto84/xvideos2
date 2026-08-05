package com.client.xvideos.common.p2p

import java.io.File

/**
 * Зеркало store-корня внутри staging-папки (inbox/outbox):
 * `base/<storeRoot относительно mainRoot>`. Благодаря зеркалу relativePath
 * манифеста, посчитанный от staging-корня, идентичен боевому.
 */
fun mirrorRoot(base: File, mainRoot: File, storeRoot: File): File =
    File(base, storeRoot.absoluteFile.normalize().relativeTo(mainRoot.absoluteFile.normalize()).path)

/**
 * Переносит ВСЁ содержимое inbox в main с сохранением структуры: rename
 * (та же ФС — мгновенно), fallback copy+delete. Существующие файлы
 * перезаписываются. После переноса inbox пересоздаётся пустым.
 */
object P2pInboxMerger {

    fun merge(inboxRoot: File, mainRoot: File) {
        if (!inboxRoot.exists()) return
        inboxRoot.walkTopDown().filter { it.isFile }.forEach { file ->
            val target = File(mainRoot, file.relativeTo(inboxRoot).path)
            target.parentFile?.mkdirs()
            if (target.exists()) target.delete()
            if (!file.renameTo(target)) {
                file.copyTo(target, overwrite = true)
                file.delete()
            }
        }
        inboxRoot.deleteRecursively()
        inboxRoot.mkdirs()
    }
}
