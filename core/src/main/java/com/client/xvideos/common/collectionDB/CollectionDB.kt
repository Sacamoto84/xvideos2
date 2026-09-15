package com.client.xvideos.common.collectionDB

import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Хранилище **вложенных** коллекций: `<path>/<коллекция>/<id>.collection`.
 *
 * Отличие от [com.client.xvideos.common.fileDB.FileDB] — уровень вложенности:
 * там плоский список файлов в одной папке, здесь папка на коллекцию и файлы
 * элементов внутри. Ревью 2026-08-16 приняло это за случайное дублирование;
 * это не так, слияние задело бы боевой путь R-коллекций ради небольшой
 * экономии кода.
 *
 * Общий контракт надёжности с `FileDB`: имя коллекции проверяется
 * ([CollectionName]), запись атомарна
 * ([com.client.xvideos.common.io.writeTextAtomically]), операции с каталогом
 * сериализованы [lock], `.tmp` от прерванной записи подчищаются на чтении.
 * Элемент, который не разобрался, молча пропускается — обрезанный JSON не
 * должен ронять весь список.
 */
class CollectionDB<T>(
    val path: String,
    private val serializer: KSerializer<T>,
    private val json: Json = AppJson
) {

    companion object {
        inline operator fun <reified T> invoke(
            path: String,
            json: Json = AppJson
        ): CollectionDB<T> = CollectionDB(path, serializer<T>(), json)
    }

    /**
     * Сериализует операции с каталогом — тот же контракт, что у
     * [com.client.xvideos.common.fileDB.FileDB]: две параллельные записи не
     * переплетаются, а чтение не видит окна между `delete()` и `renameTo()`
     * в фолбэк-ветке атомарной записи.
     */
    private val lock = Any()

    fun create(collectionName: String): Result<Boolean> {
        return try {
            val safeName = CollectionName.normalizeOrNull(collectionName)
                ?: return Result.failure(IOException("Недопустимое имя коллекции: $collectionName"))
            Timber.i("!!! Создать коллекцию  collectionCreateToDisk() collectionName:$safeName")
            synchronized(lock) {
                // Создаем директорию <userName>/block, если её нет
                val dir = File(path, safeName)
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
                }
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

            val deleted = synchronized(lock) { dir.deleteRecursively() }
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
            synchronized(lock) {
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
            if (isUnsafeItemName(itemId)) return unsafeItemName(itemId)
            Timber.i("!!! удалить лайк GIFS -> deleteItem() id:$itemId из коллекции:$safeName")

            // Папка с коллекцией
            val dir = File(path, safeName)

            // Файл-блокировка, созданный при сохранении
            val likesFile = File(dir, "$itemId.collection")

            synchronized(lock) {
                if (likesFile.exists()) {
                    // Пытаемся удалить
                    if (!likesFile.delete()) {
                        return Result.failure(IOException("Не удалось удалить файл: ${likesFile.absolutePath}"))
                    }
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
            if (isUnsafeItemName(name)) return unsafeItemName(name)
            Timber.i("!!! сохранить лайк GIFS -> likesItem() name:${name}")

            // Создаем директорию <userName>/block, если её нет
            val dir = File(path, safeName)

            synchronized(lock) {
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    if (!created) { return Result.failure(IOException("Не удалось создать директорию: ${dir.absolutePath}")) }
                }

                // Создаем файл-блокировку
                val likesFile = File(dir, "${name}.collection")

                // Атомарно: обрыв процесса посреди writeText оставлял обрезанный
                // JSON, а readAllCollections молча выбрасывает такой файл через
                // mapNotNull — элемент пропадал без следа в логах.
                likesFile.writeTextAtomically(json.encodeToString(serializer, item))
            }
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении лайка GIF")
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun readAllCollections(): Result<List<CollectionEntity<T>>> = try {
        val root = File(path)
        if (!root.exists()) return Result.success(emptyList())

        val collections: List<CollectionEntity<T>> = synchronized(lock) {
            cleanupTempFiles(root)

            root.listFiles { f -> f.isDirectory }?.map { dir ->
                val itemsInDir: List<T> = dir.listFiles { f -> f.isFile && f.extension == "collection" }
                    ?.sortedByDescending { it.lastModified() }
                    ?.mapNotNull { file ->
                        if (file.length() == 0L) return@mapNotNull null
                        try {
                            val text = file.readText(Charsets.UTF_8)
                            if (text.isBlank()) return@mapNotNull null
                            json.decodeFromString(serializer, text)
                        } catch (ex: Exception) {
                            Timber.e(ex, "!!! Не удалось проанализировать элемент коллекции: ${file.name} in ${dir.name}")
                            null
                        }
                    } ?: emptyList()
                CollectionEntity(dir.name, itemsInDir) // itemsInDir is now explicitly List<T>
            }?.sortedBy { it.collection } ?: emptyList()
        }

        Result.success(collections)
    } catch (e: Exception) {
        Timber.e(e, "Failed to read all collections from $path")
        Result.failure(e)
    }

    /**
     * Подчищает `.tmp`, оставшиеся от прерванной записи.
     *
     * На чтение они не влияют — фильтр идёт по расширению `collection`, — но
     * копятся в папке пользователя. `FileDB` делает то же самое в `refresh()`.
     */
    private fun cleanupTempFiles(root: File) {
        runCatching {
            // Суффикс общий для writeTextAtomically: tmp-имя теперь случайное
            // и «.collection.tmp» больше не образуется.
            root.listFiles { f -> f.isDirectory }?.forEach { dir ->
                dir.listFiles { f -> f.isFile && f.name.endsWith(".tmp") }
                    ?.forEach { it.delete() }
            }
        }
    }

    /**
     * Отказ для имени элемента, которым можно выйти из каталога коллекции.
     * Как и в `FileDB`: это отвергнутый вход, а не сбой ввода-вывода.
     */
    private fun <R> unsafeItemName(name: String): Result<R> {
        Timber.w("CollectionDB: имя элемента отвергнуто как путь: \"$name\"")
        return Result.failure(IllegalArgumentException("Имя элемента не может быть путём: $name"))
    }
}
