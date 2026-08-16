package com.client.xvideos.common.fileDB

import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.util.replaceWith
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * val nichesDb = FileDB<NichesInfo>(AppPath.niches_red, "niches", object : TypeToken<NichesInfo>() {}.type)
 *
 * Все публичные методы синхронные и потокобезопасны: операции с каталогом
 * сериализованы через [lock], а запись файлов атомарна (temp + rename), чтобы
 * обрыв процесса посреди записи не оставлял обрезанный JSON.
 */
class FileDB<T>(val dirPath: String, val extension: String, private val clazz: Class<T> ) {

    val list = mutableStateListOf<T>()

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** Сериализует операции с каталогом: два параллельных refresh() не переплетаются. */
    private val lock = Any()

    /** Расширение временного файла, в который пишем перед атомарным переименованием. */
    private val tempExtension = "$extension.tmp"

    fun insert(nameFile: String, value: T): Result<Boolean> {
        return try {
            synchronized(lock) {
                val dir = File(dirPath)
                if (!dir.exists()) {
                    if (!dir.mkdirs()) { throw IOException("Не удалось создать директорию: ${dir.absolutePath}") }
                }

                val file = File(dirPath, "${nameFile}.${extension}")

                gson.toJson(value).also { json ->
                    require(json != "null") { "Сериализация вернула null" }
                    file.writeTextAtomically(json)
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! eee Ошибка при сохранении файла $nameFile")
            Result.failure(e)
        }
    }

    fun update(nameFile: String, value: T): Result<Boolean> {
        return try {
            synchronized(lock) {
                val file = File(dirPath, "$nameFile.$extension")
                if (!file.exists()) {
                    return Result.failure(FileNotFoundException("File not found: ${file.absolutePath}"))
                }

                gson.toJson(value).also { json ->
                    require(json != "null") { "Serialization returned null" }
                    file.writeTextAtomically(json)
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! FileDB update error $nameFile")
            Result.failure(e)
        }
    }

    fun delete(name: String): Result<Boolean> {
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
        return try {
            val file = File(dirPath, "$nameFile.$extension")
            if (!file.exists()) {
                return Result.failure(FileNotFoundException("!!! Файл не найден: ${file.absolutePath}"))
            }
            val json = file.readText(Charsets.UTF_8)
            val obj = gson.fromJson(json, clazz)
                ?: return Result.failure(NullPointerException("!!! Десериализация вернула null"))
            Result.success(obj)
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при чтении файла $nameFile")
            Result.failure(e)
        }
    }

    fun refresh(): Result<Boolean> {
        return try {
            val loaded = synchronized(lock) {
                val dir = File(dirPath)
                if (!dir.exists() || !dir.isDirectory) {
                    return Result.failure(IOException("!!! Директория не существует: $dirPath"))
                }

                cleanupTempFiles(dir)

                val files = dir.listFiles { file -> file.extension == extension } ?: emptyArray()

                files.mapNotNull { file ->
                    try {
                        val json = file.readText(Charsets.UTF_8)
                        gson.fromJson(json, clazz)
                    } catch (e: Exception) {
                        Timber.e(e, "!!! FileDB refresh Ошибка при чтении файла $dirPath ${file.name}")
                        null
                    }
                }
            }

            list.replaceWith(loaded)

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "!!! Ошибка при обновлении списка из директории $dirPath")
            Result.failure(e)
        }
    }

    /** Подчищает временные файлы, оставшиеся от прерванной записи. */
    private fun cleanupTempFiles(dir: File) {
        runCatching {
            dir.listFiles { file -> file.name.endsWith(".$tempExtension") }
                ?.forEach { it.delete() }
        }
    }

}


