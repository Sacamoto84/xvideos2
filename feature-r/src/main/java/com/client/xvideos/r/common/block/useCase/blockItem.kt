package com.client.xvideos.r.common.block.useCase

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.r.model.GifsInfo
import kotlinx.io.IOException
import kotlinx.serialization.encodeToString
import timber.log.Timber
import java.io.File

//✅ Работает 04.06.2025
fun blockItem(item: GifsInfo): Result<Boolean> {
    return try {
        Timber.i("!!! Блокировка GIFS -> useCaseBlockItem() id:${item.id} userName:${item.userName} url:${item.urls.hd}")

        // Создаем директорию <userName>/block, если её нет
        val blockDir = File(AppPath.r_block, item.userName)

        if (!blockDir.exists()) {
            val created = blockDir.mkdirs()
            if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${blockDir.absolutePath}")) }
        }

        // Создаем файл-блокировку
        val blockFile = File(blockDir, "${item.id}.block")

        // Сохраем URL как JSON в файл
        val json = AppJson.encodeToString(item)
        blockFile.writeText(json, Charsets.UTF_8)
        Result.success(true)
    } catch (e: Exception) {
        Timber.e(e, "Ошибка при блокировке GIF")
        Result.failure(e)
    }
}

