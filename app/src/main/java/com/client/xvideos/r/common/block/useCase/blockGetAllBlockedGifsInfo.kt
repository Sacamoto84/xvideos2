package com.client.xvideos.r.common.block.useCase

import com.client.xvideos.common.AppPath
import com.client.xvideos.r.model.GifsInfo
import timber.log.Timber
import java.io.File

/**
 * Сканирует все директории пользователей в `AppPath.block_red` и собирает
 * все объекты GifsInfo, восстановленные из .block файлов.
 *
 * @return Список всех GifsInfo, считанных из .block файлов во всех пользовательских директориях.
 */
fun blockGetAllBlockedGifsInfo(): List<GifsInfo> {
    val rootDir = File(AppPath.r_block)

    if (!rootDir.exists() || !rootDir.isDirectory) {
        Timber.w("Директория кэша не найдена: ${rootDir.absolutePath}")
        return emptyList()
    }

    return rootDir.listFiles { file -> file.isDirectory }
        ?.flatMap { userDir ->
            blockGetGifsInfoByUserName(userDir.name)
        } ?: emptyList()
}
