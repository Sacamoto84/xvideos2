package com.client.xvideos.common.fileDB.folder

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FolderTableTtlOrphanTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun deleteOlderThan_evicts_orphans_without_timestamp_and_preserves_fresh() = runTest {
        val tableDir = tmp.newFolder("ttl_test")
        val table = FolderTable(tableDir.absolutePath)

        // 1. Свежая запись (1000)
        table.upsert("fresh", mapOf(
            FolderTable.FIELD_CONTENT to "fresh-content",
            FolderTable.FIELD_TIME_CREATE to "1000"
        ))

        // 2. Устаревшая запись (400)
        table.upsert("stale", mapOf(
            FolderTable.FIELD_CONTENT to "stale-content",
            FolderTable.FIELD_TIME_CREATE to "400"
        ))

        // 3. Запись без timeCreate (симулируем повреждённую или устаревшую запись)
        table.upsert("orphan", mapOf(
            FolderTable.FIELD_CONTENT to "orphan-content"
        ))

        // Выполняем очистку старше 500
        table.deleteOlderThan(500L)

        // fresh остался
        assertNotNull(table.get("fresh"))
        assertEquals("fresh-content", table.get("fresh")?.fields?.get(FolderTable.FIELD_CONTENT))

        // stale удалён
        assertNull(table.get("stale"))

        // orphan без timeCreate удалён
        assertNull(table.get("orphan"))
    }
}
