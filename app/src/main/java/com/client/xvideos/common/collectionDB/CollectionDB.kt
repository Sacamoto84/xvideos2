package com.client.xvideos.common.collectionDB

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.google.gson.Gson
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.reflect.Type

/**
 *
 */
class  CollectionDB<T>(val path : String, val type: Class<T>) {

    fun create(collectionName: String): Result<Boolean> {
        return try {
            Timber.i("!!! Создать коллекцию  collectionCreateToDisk() collectionName:$collectionName")
            // Создаем директорию <userName>/block, если её нет
            val dir = File("$path/$collectionName")
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
            val dir = File(path, collectionName)

            if (!dir.exists()) {
                Timber.w("Коллекция \"$collectionName\" не найдена: ${dir.absolutePath}")
                return Result.success(false)      // ничего не удаляли
            }

            val deleted = dir.deleteRecursively()
            if (!deleted) {
                throw IOException("Не удалось удалить коллекцию: ${dir.absolutePath}")
            }

            Timber.i("Удалена коллекция: $collectionName")
            Result.success(true)
        }.getOrElse { e ->
            Timber.e(e, "Ошибка при удалении коллекции $collectionName")
            Result.failure(e)
        }

    fun deleteItem(itemId: String, collectionName: String): Result<Boolean> {
        return try {
            Timber.i("!!! удалить лайк GIFS -> deleteItem() id:$itemId из коллекции:$collectionName")

            // Папка с коллекцией
            val dir = File(path, collectionName)

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
            Timber.i("!!! сохранить лайк GIFS -> likesItem() name:${name}")

            // Создаем директорию <userName>/block, если её нет
            val dir = File("$path/$collectionName")

            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
            }

            // Создаем файл-блокировку
            val likesFile = File(dir, "${name}.collection")

            // Сохраняем URL как JSON в файл
            val gson = Gson()
            val json = gson.toJson(item)
            likesFile.writeText(json, Charsets.UTF_8)
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
                    Gson().fromJson<T>(text, type)
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