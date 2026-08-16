package com.client.xvideos.common.collectionDB

import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.io.writeTextAtomically
import com.google.gson.Gson
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 *
 */
class  CollectionDB<T>(val path : String, val type: Class<T>) {

    // Gson потокобезопасен и дорог в конструировании: раньше экземпляр
    // создавался на каждый insert и на каждый файл в readAllCollections.
    private val gson = Gson()

    fun create(collectionName: String): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! Создать коллекцию  collectionCreateToDisk() collectionName:$safeName")
            // Создаем директорию <userName>/block, если её нет
            val dir = File(path, safeName)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
            }
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании коллекции $collectionName")
            Result.failure(e)
        }
    }


    fun deleteCollection(collectionName: String): Result<Boolean> =
        runCatching {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: throw IOException("Недопустимое имя коллекции: $collectionName")
            val dir = File(path, safeName)

            if (!dir.exists()) {
                Timber.w("Коллекция \"$safeName\" не найдена: ${dir.absolutePath}")
                return Result.success(false)      // ничего не удаляли
            }

            val deleted = dir.deleteRecursively()
            if (!deleted) {
                throw IOException("Не удалось удалить коллекцию: ${dir.absolutePath}")
            }

            Timber.i("Удалена коллекция: $safeName")
            Result.success(true)
        }.getOrElse { e ->
            Timber.e(e, "Ошибка при удалении коллекции $collectionName")
            Result.failure(e)
        }

    fun renameCollection(oldName: String, newName: String): Result<Boolean> =
        runCatching {
            val trimmed = CollectionName.normalizeOrNull(newName)
                ?: throw IOException("Недопустимое имя коллекции: $newName")
            val safeOldName = CollectionName.normalizeOrNull(oldName)
                ?: throw IOException("Недопустимое имя коллекции: $oldName")
            if (safeOldName == trimmed) {
                return Result.success(true)
            }
            val oldDir = File(path, safeOldName)
            val newDir = File(path, trimmed)
            if (!oldDir.exists()) {
                Timber.w("Коллекция \"$safeOldName\" не найдена: ${oldDir.absolutePath}")
                return Result.success(false)
            }
            if (newDir.exists()) {
                throw IOException("Коллекция \"$trimmed\" уже существует")
            }
            if (!oldDir.renameTo(newDir)) {
                throw IOException("Не удалось переименовать коллекцию: ${oldDir.absolutePath}")
            }
            Timber.i("Переименована коллекция: $safeOldName -> $trimmed")
            Result.success(true)
        }.getOrElse { e ->
            Timber.e(e, "Ошибка при переименовании коллекции $oldName -> $newName")
            Result.failure(e)
        }

    fun deleteItem(itemId: String, collectionName: String): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! удалить лайк GIFS -> deleteItem() id:$itemId из коллекции:$safeName")

            // Папка с коллекцией
            val dir = File(path, safeName)

            // Файл-блокировка, созданный при сохранении
            val likesFile = File(dir, "$itemId.collection")

            if (likesFile.exists()) {
                // Пытаемся удалить
                if (!likesFile.delete()) {
                    return Result.failure(IOException("Не удалось удалить файл: ${likesFile.absolutePath}"))
                }
            }

            /*  ──────────────────────────────────────────────────────────────
                При желании можно убрать пустую директорию коллекции:
                if (dir.isDirectory && dir.list()?.isEmpty() == true) dir.delete()
               ────────────────────────────────────────────────────────────── */

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении лайка GIF")
            Result.failure(e)
        }
    }

    fun insert(name: String, collectionName: String, item: T): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! сохранить лайк GIFS -> likesItem() name:${name}")

            // Создаем директорию <userName>/block, если её нет
            val dir = File(path, safeName)

            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
            }

            // Создаем файл-блокировку
            val likesFile = File(dir, "${name}.collection")

            // Атомарно: обрыв процесса посреди writeText оставлял обрезанный
            // JSON, а readAllCollections молча выбрасывает такой файл через
            // mapNotNull — элемент пропадал без следа в логах.
            likesFile.writeTextAtomically(gson.toJson(item))
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении лайка GIF")
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun readAllCollections(): Result<List<CollectionEntity<T>>> = try {
        val root = File(path)
        if (!root.exists()) throw IOException("Каталог коллекций не найден: ${root.absolutePath}")

        val collections: List<CollectionEntity<T>> = root.listFiles { f -> f.isDirectory }?.map { dir ->
            val itemsInDir: List<T> = dir.listFiles { f -> f.isFile && f.extension == "collection" }?.mapNotNull { file ->
                try {
                    val text = file.readText(Charsets.UTF_8)
                    gson.fromJson<T>(text, type)
                } catch (ex: Exception) {
                    Timber.e(ex, "!!! Не удалось проанализировать элемент коллекции: ${file.name} in ${dir.name}")
                    null
                }
            } ?: emptyList()
            CollectionEntity(dir.name, itemsInDir) // itemsInDir is now explicitly List<T>
        }?.sortedBy { it.collection } ?: emptyList()

        Result.success(collections)
    } catch (e: Exception) {
        Timber.e(e, "Failed to read all collections from $path")
        Result.failure(e)
    }



}
