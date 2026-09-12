package com.client.xvideos.r.common.block.useCase

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.r.model.GifsInfo
import kotlinx.serialization.encodeToString
import timber.log.Timber
import java.io.File
import java.io.IOException

//✅ Работает 04.06.2025
fun blockItem(item: GifsInfo): Result<Boolean> {
    return try {
        if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) {
            return Result.failure(IllegalArgumentException("Недопустимое имя пользователя или id для блокировки"))
        }

        Timber.i("!!! Блокировка GIFS -> useCaseBlockItem() id:${item.id} userName:${item.userName} url:${item.urls.hd}")

        val rootDir = File(AppPath.r_block)
        val blockDir = File(rootDir, item.userName)
        requireInside(rootDir, blockDir)

        if (!blockDir.exists()) {
            val created = blockDir.mkdirs()
            if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${blockDir.absolutePath}")) }
        }

        // Создаем файл-блокировку
        val blockFile = File(blockDir, "${item.id}.block")
        requireInside(blockDir, blockFile)

        // Сохраняем как JSON атомарно
        val json = AppJson.encodeToString(item)
        blockFile.writeTextAtomically(json)
        Result.success(true)
    } catch (e: Exception) {
        Timber.e(e, "Ошибка при блокировке GIF")
        Result.failure(e)
    }
}

