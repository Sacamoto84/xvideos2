package com.client.xvideos.r.common.block.useCase

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import timber.log.Timber
import java.io.File

/**
 * Возвращает список идентификаторов заблокированных гифок конкретного пользователя [userName].
 *
 * @param userName Имя автора.
 * @return Список id гифок (имен файлов без расширения `.block`).
 */
fun blockGetGifsByUserNameAsListString(userName: String): List<String> {
    if (isUnsafeItemName(userName)) return emptyList()

    val rootDir = File(AppPath.r_block)
    val blockDir = File(rootDir, userName)
    runCatching { requireInside(rootDir, blockDir) }.onFailure { return emptyList() }

    if (!blockDir.exists() || !blockDir.isDirectory) {
        Timber.w("Директория блокировок не найдена: ${blockDir.absolutePath}")
        return emptyList()
    }

    return blockDir.listFiles { file ->
        file.isFile && file.name.endsWith(".block")
    }?.map { file ->
        file.name.removeSuffix(".block")
    } ?: emptyList()
}
