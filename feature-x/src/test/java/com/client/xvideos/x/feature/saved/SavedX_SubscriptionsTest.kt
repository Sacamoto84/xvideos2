package com.client.xvideos.x.feature.saved

import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ProfileType
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.model.toSubscriptionItem
import com.client.xvideos.x.model.withImagesFrom
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedX_SubscriptionsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `cleanSlug корректно очищает любые URL-префиксы и слеши`() {
        val testCases = listOf(
            "/models/joy-sky" to "joy-sky",
            "models/joy-sky/" to "joy-sky",
            "/channels/dart_oficial" to "dart_oficial",
            "channels/dart_oficial/" to "dart_oficial",
            "/profiles/sweet-girl" to "sweet-girl",
            "/model-channel/" to "model-channel",
            "   /models/some-name/   " to "some-name",
        )

        for ((input, expected) in testCases) {
            val item = XSubscriptionItem(slug = input)
            assertEquals("Ошибка для input: $input", expected, item.cleanSlug)
        }
    }

    @Test
    fun `toSubscriptionItem корректно конвертирует ChannelHeaderModel`() {
        val header = ChannelHeaderModel(
            slug = "/models/joy-sky",
            name = "Joy Sky",
            avatarUrl = "https://img.com/avatar.jpg",
            bannerUrl = "https://img.com/banner.jpg",
            subscribers = "45,2 к",
            totalViews = "12 М",
            videoCount = 180,
            profileType = ProfileType.MODEL,
        )

        val sub = header.toSubscriptionItem()

        assertEquals("joy-sky", sub.cleanSlug)
        assertEquals("Joy Sky", sub.name)
        assertEquals("Joy Sky", sub.displayName)
        assertEquals("https://img.com/avatar.jpg", sub.avatarUrl)
        assertEquals("https://img.com/banner.jpg", sub.bannerUrl)
        assertEquals("45,2 к", sub.subscribers)
        assertEquals("12 М", sub.totalViews)
        assertEquals(180, sub.videoCount)
        assertTrue(sub.isModel)
    }

    @Test
    fun `withImagesFrom берёт картинки свежей шапки и не трогает остальные поля`() {
        val saved = XSubscriptionItem(
            slug = "joy-sky",
            name = "Joy Sky",
            avatarUrl = "https://img.com/avatar.jpg?secure=old,1",
            bannerUrl = "https://img.com/banner.jpg?secure=old,1",
            isModel = true,
            subscribers = "45,2 к",
            totalViews = "12 М",
            videoCount = 180,
            dateAdded = 123456789L,
        )
        val header = ChannelHeaderModel(
            slug = "joy-sky",
            name = "Другое имя",
            avatarUrl = "https://img.com/avatar.jpg?secure=new,2",
            bannerUrl = "https://img.com/banner.jpg?secure=new,2",
            subscribers = "50 к",
            videoCount = 200,
            profileType = ProfileType.MODEL,
        )

        val updated = saved.withImagesFrom(header)

        assertEquals(
            saved.copy(
                avatarUrl = "https://img.com/avatar.jpg?secure=new,2",
                bannerUrl = "https://img.com/banner.jpg?secure=new,2",
            ),
            updated,
        )
    }

    @Test
    fun `withImagesFrom без картинок в шапке оставляет запись как есть`() {
        val saved = XSubscriptionItem(slug = "joy-sky", avatarUrl = "https://img.com/a.jpg?secure=old,1")
        // Так выглядит шапка, когда страница не загрузилась: только запасные поля.
        val header = ChannelHeaderModel(slug = "joy-sky", name = "joy-sky")

        assertNull(saved.withImagesFrom(header))
    }

    @Test
    fun `withImagesFrom с теми же адресами возвращает null`() {
        val saved = XSubscriptionItem(slug = "joy-sky", avatarUrl = "https://img.com/a.jpg", bannerUrl = "")
        val header = ChannelHeaderModel(slug = "joy-sky", avatarUrl = "https://img.com/a.jpg")

        assertNull(saved.withImagesFrom(header))
    }

    @Test
    fun `withImagesFrom без аватара на странице переходит на баннер`() {
        val saved = XSubscriptionItem(slug = "joy-sky", avatarUrl = "https://img.com/a.jpg?secure=old,1")
        val header = ChannelHeaderModel(slug = "joy-sky", bannerUrl = "https://img.com/b.jpg")

        val updated = saved.withImagesFrom(header)

        assertEquals("", updated?.avatarUrl)
        assertEquals("https://img.com/b.jpg", updated?.bannerUrl)
    }

    @Test
    fun `XSubscriptionItem успешно сериализуется и десериализуется в JSON`() {
        val item = XSubscriptionItem(
            slug = "dart_oficial",
            name = "Dart Oficial",
            avatarUrl = "https://img.com/dart.jpg",
            bannerUrl = "https://img.com/banner.jpg",
            isModel = false,
            subscribers = "100 к",
            totalViews = "50 М",
            videoCount = 42,
            dateAdded = 123456789L,
        )

        val encoded = json.encodeToString(XSubscriptionItem.serializer(), item)
        val decoded = json.decodeFromString(XSubscriptionItem.serializer(), encoded)

        assertEquals(item, decoded)
    }

    @Test
    fun `round-robin объединение результатов чередует авторов и исключает дубликаты`() {
        val videosCreator1 = listOf(
            ItemsX(id = 1L, title = "A1"),
            ItemsX(id = 2L, title = "A2"),
            ItemsX(id = 3L, title = "A3"),
        )
        val videosCreator2 = listOf(
            ItemsX(id = 10L, title = "B1"),
            ItemsX(id = 2L, title = "A2-duplicate"), // дубликат id 2
            ItemsX(id = 20L, title = "B2"),
        )

        val results = listOf(videosCreator1, videosCreator2)
        val seenIds = HashSet<Long>()
        val combined = ArrayList<ItemsX>()

        val maxVideos = results.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxVideos) {
            for (creatorVideos in results) {
                if (i < creatorVideos.size) {
                    val video = creatorVideos[i]
                    if (seenIds.add(video.id)) {
                        combined.add(video)
                    }
                }
            }
        }

        // Порядок: A1 (1), B1 (10), A2 (2), дубликат 2 пропущен, A3 (3), B2 (20)
        assertEquals(5, combined.size)
        assertEquals(1L, combined[0].id)
        assertEquals(10L, combined[1].id)
        assertEquals(2L, combined[2].id)
        assertEquals(3L, combined[3].id)
        assertEquals(20L, combined[4].id)
    }
}
