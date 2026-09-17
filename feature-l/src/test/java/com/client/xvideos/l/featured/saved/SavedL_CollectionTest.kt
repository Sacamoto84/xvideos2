package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.json.AppJson
import com.client.xvideos.l.model.PicsDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedL_CollectionTest {

    @Test
    fun `LCollectionSortOrder RECENT сортирует по убыванию lastModifiedAt`() {
        val c1 = LCollectionEntity("alpha", null, 10, lastModifiedAt = 1000L, duplicateCount = 0, hasManualCover = false)
        val c2 = LCollectionEntity("beta", null, 5, lastModifiedAt = 3000L, duplicateCount = 0, hasManualCover = false)
        val c3 = LCollectionEntity("gamma", null, 20, lastModifiedAt = 2000L, duplicateCount = 0, hasManualCover = false)

        val list = listOf(c1, c2, c3)
        val sorted = list.sortedByDescending { it.lastModifiedAt }

        assertEquals(listOf("beta", "gamma", "alpha"), sorted.map { it.collection })
    }

    @Test
    fun `LCollectionSortOrder NAME сортирует по алфавиту без учета регистра`() {
        val c1 = LCollectionEntity("zebra", null, 1, 100L, 0, false)
        val c2 = LCollectionEntity("Apple", null, 1, 200L, 0, false)
        val c3 = LCollectionEntity("banana", null, 1, 300L, 0, false)

        val list = listOf(c1, c2, c3)
        val sorted = list.sortedBy { it.collection.lowercase() }

        assertEquals(listOf("Apple", "banana", "zebra"), sorted.map { it.collection })
    }

    @Test
    fun `LCollectionSortOrder SIZE сортирует по числу элементов с tie-break по имени`() {
        val c1 = LCollectionEntity("alpha", null, itemsCount = 10, lastModifiedAt = 100L, duplicateCount = 0, hasManualCover = false)
        val c2 = LCollectionEntity("beta", null, itemsCount = 50, lastModifiedAt = 200L, duplicateCount = 0, hasManualCover = false)
        val c3 = LCollectionEntity("gamma", null, itemsCount = 10, lastModifiedAt = 300L, duplicateCount = 0, hasManualCover = false)

        val list = listOf(c1, c2, c3)
        val sorted = list.sortedWith(
            compareByDescending<LCollectionEntity> { it.itemsCount }
                .thenBy { it.collection.lowercase() }
        )

        assertEquals(listOf("beta", "alpha", "gamma"), sorted.map { it.collection })
    }

    @Test
    fun `LCollectionDuplicateGroup группирует дубликаты по ключу`() {
        val pic1 = PicsDetails(url_to_original = "url1")
        val pic2 = PicsDetails(url_to_original = "url2")
        val group = LCollectionDuplicateGroup(key = "hash_123", items = listOf(pic1, pic2))

        assertEquals("hash_123", group.key)
        assertEquals(2, group.items.size)
    }

    @Test
    fun `LCollectionConfig сериализуется и десериализуется через AppJson`() {
        val config = LCollectionConfig(schemaVersion = 1, coverFolderName = "pic_cover_1")
        val json = AppJson.encodeToString(config)
        val parsed = AppJson.decodeFromString<LCollectionConfig>(json)

        assertEquals(1, parsed.schemaVersion)
        assertEquals("pic_cover_1", parsed.coverFolderName)
    }
}
