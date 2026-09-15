package com.client.xvideos.common.fileDB

import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.util.replaceWith
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/**
 * val nichesDb = FileDB<NichesInfo>(AppPath.niches_red, "niches", NichesInfo.serializer())
 *
 * Все публичные методы синхронные и потокобезопасны: операции с каталогом
 * сериализованы через [lock], а запись файлов атомарна (temp + rename), чтобы
 * обрыв процесса посреди записи не оставлял обрезанный JSON. Публикация
 * результата [refresh] в [list] идёт вне [lock], но упорядочена по номеру
 * загрузки — устаревший результат не ляжет поверх свежего.
 *
 * Хранит **плоский** список: файлы `<имя>.<extension>` в одной папке. Для
 * вложенного случая — папки-коллекции, внутри каждой файлы элементов — есть
 * соседний [com.client.xvideos.common.collectionDB.CollectionDB]. Это не
 * дубликат: разный уровень вложенности и разный жизненный цикл элементов.
 * Держите надёжность обоих в одном состоянии — атомарная запись, лок операций,
 * уборка `.tmp`.
 */
class FileDB<T>(
    val dirPath: String,
    val extension: String,
    private val serializer: KSerializer<T>,
    private val json: Json = AppJson
) {

    companion object {
        inline operator fun <reified T> invoke(
            dirPath: String,
            extension: String,
            json: Json = AppJson
        ): FileDB<T> = FileDB(dirPath, extension, serializer<T>(), json)
    }

    val list = mutableStateListOf<T>()

    /** Сериализует операции с каталогом: два параллельных refresh() не переплетаются. */
    private val lock = Any()

    /**
     * Номер загрузки и последний опубликованный номер.
     *
     * Публикация в [list] стоит за пределами [lock] намеренно: `replaceWith`
     * берёт снапшот-лок Compose, и захват `lock -> snapshotLock` встретился бы
     * с обратным порядком у кода, который зовёт FileDB из-под снапшота.
     * Порядок вместо этого восстанавливается по номеру: результат более старой
     * загрузки не может лечь поверх более новой.
     */
    private val loadSeq = AtomicLong(0)
    private val publishLock = Any()
    private var publishedSeq = 0L

    fun insert(nameFile: String, value: T): Result<Boolean> {
        if (isUnsafeItemName(nameFile)) return unsafeName(nameFile)
        return try {
            synchronized(lock) {
                val dir = File(dirPath)
                if (!dir.exists()) {
                    if (!dir.mkdirs()) { throw IOException("Не удалось создать директорию: ${dir.absolutePath}") }
                }

                val file = File(dirPath, "${nameFile}.${extension}")

                val jsonString = json.encodeToString(serializer, value)
                file.writeTextAtomically(jsonString)
            }

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! eee Ошибка при сохранении файла $nameFile")
            Result.failure(e)
        }
    }

    fun update(nameFile: String, value: T): Result<Boolean> {
        if (isUnsafeItemName(nameFile)) return unsafeName(nameFile)
        return try {
            synchronized(lock) {
                val file = File(dirPath, "$nameFile.$extension")
                if (!file.exists()) {
                    return Result.failure(FileNotFoundException("File not found: ${file.absolutePath}"))
                }

                val jsonString = json.encodeToString(serializer, value)
                file.writeTextAtomically(jsonString)
            }

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! FileDB update error $nameFile")
            Result.failure(e)
        }
    }

    fun delete(name: String): Result<Boolean> {
        if (isUnsafeItemName(name)) return unsafeName(name)
        return try {
            synchronized(lock) {
                val file = File(dirPath, "$name.$extension")
                if (file.exists()) {
                    if (!file.delete()) {
                        return Result.failure(IOException("!!! Не удалось удалить файл: ${file.absolutePath}"))
                    }
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при удалении $name")
            Result.failure(e)
        }
    }


    fun read(nameFile: String): Result<T> {
        if (isUnsafeItemName(nameFile)) return unsafeName(nameFile)
        return try {
            synchronized(lock) {
                val file = File(dirPath, "$nameFile.$extension")
                if (!file.exists() || file.length() == 0L) {
                    return Result.failure(FileNotFoundException("!!! Файл не найден или пуст: ${file.absolutePath}"))
                }
                val jsonString = file.readText(Charsets.UTF_8)
                val obj = json.decodeFromString(serializer, jsonString)
                Result.success(obj)
            }
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при чтении файла $nameFile")
            Result.failure(e)
        }
    }

    fun refresh(): Result<Boolean> {
        return try {
            // Номер берётся под тем же локом, что и чтение каталога, поэтому
            // порядок номеров совпадает с порядком загрузок. Пара, а не
            // присваивание внешней val изнутри лямбды: так не приходится
            // полагаться на definite-assignment сквозь inline-функцию.
            val (seq, loaded) = synchronized(lock) {
                val dir = File(dirPath)
                if (!dir.exists() || !dir.isDirectory) {
                    return Result.failure(IOException("!!! Директория не существует: $dirPath"))
                }

                cleanupTempFiles(dir)

                val files = dir.listFiles { file -> file.extension == extension }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

                loadSeq.incrementAndGet() to files.mapNotNull { file ->
                    if (file.length() == 0L) return@mapNotNull null
                    try {
                        val jsonString = file.readText(Charsets.UTF_8)
                        if (jsonString.isBlank()) return@mapNotNull null
                        json.decodeFromString(serializer, jsonString)
                    } catch (e: Exception) {
                        Timber.e(e, "!!! FileDB refresh Ошибка при чтении файла $dirPath ${file.name}")
                        null
                    }
                }
            }

            synchronized(publishLock) {
                if (seq > publishedSeq) {
                    publishedSeq = seq
                    list.replaceWith(loaded)
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при обновлении списка из директории $dirPath")
            Result.failure(e)
        }
    }

    /** Подчищает временные файлы, оставшиеся от прерванной записи. */
    private fun cleanupTempFiles(dir: File) {
        runCatching {
            // Суффикс общий для writeTextAtomically, а не «расширение.tmp»:
            // tmp-имя теперь случайное и целевое расширение в него не входит.
            dir.listFiles { file -> file.name.endsWith(".tmp") }
                ?.forEach { it.delete() }
        }
    }

    /**
     * Отказ для имени, которым можно выйти из своего каталога. Отдельная
     * функция, а не общий catch: это не сбой ввода-вывода, а отвергнутый вход.
     */
    private fun <R> unsafeName(name: String): Result<R> {
        Timber.w("FileDB: имя элемента отвергнуто как путь: \"$name\"")
        return Result.failure(IllegalArgumentException("Имя элемента не может быть путём: $name"))
    }

}


