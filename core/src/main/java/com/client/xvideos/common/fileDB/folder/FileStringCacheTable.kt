package com.client.xvideos.common.fileDB.folder

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

data class FileStringCacheEntry(
    val key: String,
    val content: String,
    val timeCreate: Long = System.currentTimeMillis(),
    val timeCreateText: String = currentFileDbTimeText()
)

class FileStringCacheTable(
    private val table: FolderTable
) {
    suspend fun insert(entry: FileStringCacheEntry) {
        table.upsert(
            key = entry.key,
            fields = mapOf(
                FolderTable.FIELD_CONTENT to entry.content.prettyJsonOrSelf(),
                FolderTable.FIELD_TIME_CREATE to entry.timeCreate.toString(),
                FolderTable.FIELD_TIME_CREATE_TEXT to entry.timeCreateText
            )
        )
    }

    suspend fun put(key: String, content: String) {
        insert(FileStringCacheEntry(key = key, content = content))
    }

    suspend fun get(key: String): FileStringCacheEntry? {
        val record = table.get(key) ?: return null
        val content = record.fields[FolderTable.FIELD_CONTENT] ?: return null
        return FileStringCacheEntry(
            key = record.key,
            content = content,
            timeCreate = record.fields[FolderTable.FIELD_TIME_CREATE]?.toLongOrNull() ?: 0L,
            timeCreateText = record.fields[FolderTable.FIELD_TIME_CREATE_TEXT].orEmpty()
        )
    }

    suspend fun delete(key: String) {
        table.delete(key)
    }

    suspend fun deleteOld(timeMs: Long) {
        table.deleteOlderThan(timeMs)
    }

    suspend fun deleteAll() {
        table.deleteAll()
    }
}

private val fileDbPrettyGson = GsonBuilder()
    .setPrettyPrinting()
    .create()

private fun String.prettyJsonOrSelf(): String {
    val trimmed = trimStart()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return this

    return runCatching {
        fileDbPrettyGson.toJson(JsonParser.parseString(this))
    }.getOrElse {
        this
    }
}
