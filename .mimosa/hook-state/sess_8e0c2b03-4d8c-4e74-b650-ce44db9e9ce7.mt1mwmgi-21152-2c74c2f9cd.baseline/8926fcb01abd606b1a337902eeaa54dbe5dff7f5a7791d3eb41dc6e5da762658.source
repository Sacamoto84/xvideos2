package com.client.xvideos.common.fileDB.folder

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Срок годности записей кеша.
 *
 * Поле `timeCreate` писалось с самого начала, но никогда не читалось, а
 * `deleteOld` не вызывался ниоткуда. То есть ответы лент RedGifs оседали на
 * диске навсегда: «Топ за неделю», однажды попавший в кеш, показывался бы и
 * месяц спустя, а сама папка росла без потолка — ключом там служит полный URL
 * вместе с номером страницы.
 *
 * Часы подставляются параметром, чтобы не спать в тесте.
 */
class FileStringCacheTableTtlTest {

    @get:Rule val tmp = TemporaryFolder()

    private var now = 1_000_000_000L

    private fun tableAt(dir: String, ttlMs: Long?) = FileStringCacheTable(
        table = FolderTable(tmp.root.resolve(dir).absolutePath),
        ttlMs = ttlMs,
        now = { now }
    )

    @Test
    fun `без срока запись живёт сколько угодно`() = runTest {
        val cache = tableAt("no-ttl", ttlMs = null)
        cache.put("k", "value")

        now += 365L * 24 * 60 * 60 * 1000

        assertEquals("value", cache.get("k")?.content)
    }

    @Test
    fun `свежая запись отдаётся`() = runTest {
        val ttl = 6L * 60 * 60 * 1000
        val cache = tableAt("fresh", ttlMs = ttl)
        cache.put("k", "value")

        now += ttl - 1

        assertEquals("value", cache.get("k")?.content)
    }

    @Test
    fun `просроченная запись не отдаётся и удаляется`() = runTest {
        val ttl = 6L * 60 * 60 * 1000
        val dir = "stale"
        val cache = tableAt(dir, ttlMs = ttl)
        cache.put("k", "value")

        // Ровно ttl — ещё свежая: граница строгая, как у FolderTable.deleteOlderThan.
        now += ttl
        assertNotNull(cache.get("k"))
        now += 1

        assertNull(cache.get("k"))
        // Не только скрыта от читателя, но и убрана с диска: иначе папка
        // продолжала бы расти теми же записями, что больше не годятся.
        assertNull(FolderTable(tmp.root.resolve(dir).absolutePath).get("k"))
    }

    @Test
    fun `deleteExpired убирает просроченные и оставляет свежие`() = runTest {
        val ttl = 6L * 60 * 60 * 1000
        val cache = tableAt("sweep", ttlMs = ttl)

        cache.put("old", "old-value")
        now += ttl + 1
        cache.put("new", "new-value")

        cache.deleteExpired()

        assertNull(cache.get("old"))
        assertEquals("new-value", cache.get("new")?.content)
    }

    @Test
    fun `без срока deleteExpired ничего не трогает`() = runTest {
        val cache = tableAt("sweep-no-ttl", ttlMs = null)
        cache.put("k", "value")

        now += 365L * 24 * 60 * 60 * 1000
        cache.deleteExpired()

        assertNotNull(cache.get("k"))
    }

    /**
     * Запись без разборчивого времени создания проверить нечем. Со сроком
     * годности она считается просроченной — отдавать непроверяемое из кеша с
     * TTL значит обходить сам TTL.
     */
    @Test
    fun `запись без времени создания считается просроченной`() = runTest {
        // Каталоги разные: чтение со сроком просроченную запись ещё и удаляет,
        // и второй проверке достался бы пустой каталог.
        FolderTable(tmp.root.resolve("no-time-ttl").absolutePath)
            .upsert("k", mapOf(FolderTable.FIELD_CONTENT to "value"))
        FolderTable(tmp.root.resolve("no-time-free").absolutePath)
            .upsert("k", mapOf(FolderTable.FIELD_CONTENT to "value"))

        assertNull(tableAt("no-time-ttl", ttlMs = 1000).get("k"))
        // А без срока — отдаётся как прежде: старые таблицы этого не заметят.
        assertEquals("value", tableAt("no-time-free", ttlMs = null).get("k")?.content)
    }
}
