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

/**
 * Блокирует переданный элемент [item]:
 * - Проверяет безопасность путей ([isUnsafeItemName], [requireInside]);
 * - Создает папку пользователя `<AppPath.r_block>/<userName>`;
 * - Атомарно сохраняет JSON сериализованного [GifsInfo] в файл `<id>.block`.
 *
 * @param item Блокируемый медиаэлемент.
 * @return [Result] с флагом успеха операции.
 */
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

/**
 * Разблокирует переданный элемент [item]:
 * - Проверяет безопасность путей;
 * - Удаляет файл `<id>.block`;
 * - Если папка автора осталась пустой, удаляет саму папку автора.
 *
 * @param item Разблокируемый медиаэлемент.
 * @return [Result] с флагом успеха операции.
 */
fun unblockItem(item: GifsInfo): Result<Boolean> {
    return try {
        if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) {
            return Result.failure(IllegalArgumentException("Недопустимое имя пользователя или id для разблокировки"))
        }

        Timber.i("!!! Разблокировка GIFS -> unblockItem() id:${item.id} userName:${item.userName}")

        val rootDir = File(AppPath.r_block)
        val blockDir = File(rootDir, item.userName)
        requireInside(rootDir, blockDir)

        val blockFile = File(blockDir, "${item.id}.block")
        requireInside(blockDir, blockFile)

        if (blockFile.exists()) {
            blockFile.delete()
        }

        if (blockDir.exists() && blockDir.isDirectory) {
            val remaining = blockDir.listFiles()
            if (remaining == null || remaining.isEmpty()) {
                blockDir.delete()
            }
        }

        Result.success(true)
    } catch (e: Exception) {
        Timber.e(e, "Ошибка при разблокировке GIF")
        Result.failure(e)
    }
}

/**
 * Возвращает дескриптор файла блокировки `<userName>/<id>.block` с проверкой безопасности путей.
 * Если путь небезопасен, возвращает null.
 */
fun getBlockFile(userName: String, id: String): File? {
    if (userName.isBlank() || id.isBlank()) return null
    if (isUnsafeItemName(userName) || isUnsafeItemName(id)) return null
    val rootDir = File(AppPath.r_block)
    val blockDir = File(rootDir, userName)
    val blockFile = File(blockDir, "$id.block")
    return try {
        requireInside(rootDir, blockDir)
        requireInside(blockDir, blockFile)
        blockFile
    } catch (e: Exception) {
        Timber.d(e, "Недопустимый путь к файлу блокировки: userName=%s, id=%s", userName, id)
        null
    }
}

/**
 * Проверяет наличие файла блокировки непосредственно на файловой системе.
 */
fun isItemBlockedOnDisk(userName: String, id: String): Boolean {
    val file = getBlockFile(userName, id) ?: return false
    return file.exists() && file.length() > 0L
}
