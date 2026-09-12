package com.client.xvideos.r.common.block.useCase

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.r.model.GifsInfo
import timber.log.Timber
import java.io.File

fun blockGetGifsInfoByUserName(userName: String): List<GifsInfo> {
    if (isUnsafeItemName(userName)) return emptyList()

    val rootDir = File(AppPath.r_block)
    val blockDir = File(rootDir, userName)
    runCatching { requireInside(rootDir, blockDir) }.onFailure { return emptyList() }

    if (!blockDir.exists() || !blockDir.isDirectory) {
        Timber.w("Директория блокировок не найдена: ${blockDir.absolutePath}")
        return emptyList()
    }

    val blockedGifs = mutableListOf<GifsInfo>()

    blockDir.listFiles { file ->
        file.isFile && file.name.endsWith(".block")
    }?.forEach { file ->
        try {
            val json = file.readText(Charsets.UTF_8)
            val gifInfo = AppJson.decodeFromString<GifsInfo>(json)
            blockedGifs.add(gifInfo)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка чтения файла блокировки: ${file.name}")
        }
    }

    return blockedGifs
}
