package com.client.xvideos.common.fileDB.folder

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

data class FileStringCacheEntry(
    val key: String,
    val content: String,
    val timeCreate: Long = System.currentTimeMillis(),
    val timeCreateText: String = currentFileDbTimeText()
)

/**
 * @param ttlMs срок годности записи. `null` — бессрочно, как было у всех таблиц
 *   до появления параметра. Со сроком запись после его истечения перестаёт
 *   отдаваться из [get] и удаляется: без этого `timeCreate` писался, но никогда
 *   не читался, и лента «Топ за неделю», однажды попавшая в кеш, показывалась
 *   бы месяц спустя — обновиться ей было неоткуда.
 * @param now источник текущего времени, отдельным параметром ради тестов.
 */
class FileStringCacheTable(
    private val table: FolderTable,
    private val ttlMs: Long? = null,
    private val now: () -> Long = System::currentTimeMillis
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
        // Время берётся из [now], а не из умолчания FileStringCacheEntry: иначе
        // запись и проверка срока смотрели бы на разные часы.
        insert(FileStringCacheEntry(key = key, content = content, timeCreate = now()))
    }

    suspend fun get(key: String): FileStringCacheEntry? {
        val record = table.get(key) ?: return null
        val content = record.fields[FolderTable.FIELD_CONTENT] ?: return null
        // Записи без разборчивого времени создания достаётся 0: со сроком
        // годности это «бесконечно старая», то есть просроченная. Проверить её
        // всё равно нечем, а отдавать непроверяемое из кеша с TTL — обходить
        // сам TTL.
        val timeCreate = record.fields[FolderTable.FIELD_TIME_CREATE]?.toLongOrNull() ?: 0L
        if (isExpired(timeCreate)) {
            delete(key)
            return null
        }
        return FileStringCacheEntry(
            key = record.key,
            content = content,
            timeCreate = timeCreate,
            timeCreateText = record.fields[FolderTable.FIELD_TIME_CREATE_TEXT].orEmpty()
        )
    }

    suspend fun delete(key: String) {
        table.delete(key)
    }

    /**
     * Удаляет просроченные записи. Без [ttlMs] не делает ничего.
     *
     * [get] чистит по одной и только те, за которыми пришли, — то есть мусор от
     * лент, куда больше не заходят, так и лежал бы. Каталог обходится целиком,
     * поэтому вызывать это на старте в фоне, а не на пути к первому экрану.
     */
    suspend fun deleteExpired() {
        val cutoff = expiryCutoff() ?: return
        table.deleteOlderThan(cutoff)
    }

    /** Время, раньше которого запись считается просроченной. `null` — срока нет. */
    private fun expiryCutoff(): Long? = ttlMs?.let { now() - it }

    private fun isExpired(timeCreate: Long): Boolean {
        val cutoff = expiryCutoff() ?: return false
        // Строгое сравнение — такое же, как в FolderTable.deleteOlderThan:
        // ровно на границе запись ещё живая. Иначе чтение и уборка расходились
        // бы ровно на один миллисекундный случай.
        return timeCreate < cutoff
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
