package com.client.xvideos.common.fileDB.folder

import com.client.xvideos.common.util.toMD5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FolderRecord(
    val key: String,
    val fields: Map<String, String>
)

class FolderTable(
    dirPath: String
) {
    private val tableDir = File(dirPath)
    private val mutex = Mutex()
    private val safeKeyRegex = Regex("[A-Za-z0-9._-]{1,120}")

    suspend fun upsert(key: String, fields: Map<String, String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val rowDir = rowDir(key, createTableDir = true)
            rowDir.mkdirs()
            if (rowDir.name != key) {
                writeField(rowDir, FIELD_KEY, key)
            } else {
                File(rowDir, fieldFileName(FIELD_KEY)).delete()
            }
            fields.forEach { (field, value) ->
                writeField(rowDir, field, value)
            }
        }
    }

    suspend fun get(key: String): FolderRecord? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readRecord(rowDir(key, createTableDir = false))
        }
    }

    suspend fun all(): List<FolderRecord> = withContext(Dispatchers.IO) {
        mutex.withLock {
            tableDir.listFiles { file -> file.isDirectory }
                ?.mapNotNull { readRecord(it) }
                ?: emptyList()
        }
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            rowDir(key, createTableDir = false).deleteRecursively()
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (tableDir.exists() && !tableDir.deleteRecursively()) {
                Timber.w("FolderTable deleteAll failed: ${tableDir.absolutePath}")
            }
        }
    }

    suspend fun deleteOlderThan(timeMs: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            tableDir.listFiles { file -> file.isDirectory }
                ?.forEach { rowDir ->
                    val timeCreate = File(rowDir, fieldFileName(FIELD_TIME_CREATE))
                        .takeIf { it.exists() }
                        ?.readText(Charsets.UTF_8)
                        ?.toLongOrNull()

                    if (timeCreate != null && timeCreate < timeMs) {
                        rowDir.deleteRecursively()
                    }
                }
        }
    }

    private fun rowDir(key: String, createTableDir: Boolean): File {
        if (createTableDir) tableDir.mkdirs()
        val dirName = if (safeKeyRegex.matches(key)) key else key.toMD5()
        return File(tableDir, dirName)
    }

    private fun readRecord(rowDir: File): FolderRecord? {
        if (!rowDir.exists() || !rowDir.isDirectory) return null

        return runCatching {
            val fields = rowDir.listFiles { file -> file.isFile && !file.name.endsWith(".tmp") }
                ?.associate { file ->
                    file.name.removeSuffix(".txt") to file.readText(Charsets.UTF_8)
                }
                ?: emptyMap()

            val key = fields[FIELD_KEY] ?: rowDir.name
            FolderRecord(key = key, fields = fields)
        }.onFailure {
            Timber.e(it, "FolderTable read error: ${rowDir.absolutePath}")
        }.getOrNull()
    }

    private fun writeField(rowDir: File, field: String, value: String) {
        val target = File(rowDir, fieldFileName(field))
        val tmp = File(rowDir, "${target.name}.tmp")
        tmp.writeText(value, Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            throw IllegalStateException("Cannot replace file: ${target.absolutePath}")
        }
        if (!tmp.renameTo(target)) {
            throw IllegalStateException("Cannot commit file: ${target.absolutePath}")
        }
    }

    private fun fieldFileName(field: String): String {
        return field.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".txt"
    }

    companion object {
        const val FIELD_KEY = "key"
        const val FIELD_CONTENT = "content"
        const val FIELD_TIME_CREATE = "timeCreate"
        const val FIELD_TIME_CREATE_TEXT = "timeCreateText"
    }
}

fun currentFileDbTimeText(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}
