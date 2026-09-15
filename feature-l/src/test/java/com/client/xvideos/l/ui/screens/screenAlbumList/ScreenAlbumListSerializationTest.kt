package com.client.xvideos.l.ui.screens.screenAlbumList

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.model.OnlyContent
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

/**
 * Тот же контракт, что и у `ScreenRedFullScreenSerializationTest` в `:feature-r`.
 *
 * `Screen` у Voyager на Android — `Serializable`, а стек экранов сохраняется
 * целыми объектами (`SnapshotStateStack`, `listSaver(save = { stack.items })`).
 * Значит любое поле экрана обязано сериализоваться, иначе приложение падает
 * `NotSerializableException` в момент, когда система сохраняет состояние.
 *
 * `L_ScreenAlbumList.create` кладёт в экран `AlbumListFilter` со вложенными
 * `FilterGenre` и `OnlyContent` — проверяем всю цепочку, а не только верхний тип.
 */
class ScreenAlbumListSerializationTest {

    private fun serialize(value: Any) {
        ObjectOutputStream(ByteArrayOutputStream()).use { it.writeObject(value) }
    }

    private fun genre() = FilterGenre(
        id = "1",
        title = "Genre",
        slug = "genre",
        description = "описание",
        uploadingRules = "правила",
        posterUrl = null,
        actsAsWarning = false,
        actsAsDefault = true,
        representsUncategorized = false,
        url = "https://example/genre",
        parent = null,
        onlyAllowsModel = listOf("model"),
        onlyContent = OnlyContent(id = "2", title = "Content", url = "https://example/content"),
    )

    @Test
    fun `экран списка альбомов с фильтром переживает запись в saved state`() {
        serialize(
            L_ScreenAlbumList.create(
                filter = AlbumListFilter(
                    genresPlus = listOf(genre()),
                    genresMinus = listOf(genre()),
                    tagPlus = listOf("tag"),
                    searchQuery = "запрос",
                ),
                title = "Заголовок",
            )
        )
    }

    @Test
    fun `экран списка альбомов без фильтра переживает запись в saved state`() {
        serialize(L_ScreenAlbumList.create(filter = null))
    }

    @Test
    fun `фильтр сериализуется отдельно от экрана`() {
        serialize(AlbumListFilter(genresPlus = listOf(genre())))
    }

    @Test
    fun `SavedAlbumFilter сериализуется через Java serialization`() {
        val filter = AlbumListFilter(genresPlus = listOf(genre()), tagPlus = listOf("tag1"))
        val preset = com.client.xvideos.l.model.SavedAlbumFilter(name = "Test Preset", filter = filter)
        serialize(preset)
    }

    @Test
    fun `AlbumListFilter и SavedAlbumFilter сериализуются в JSON через LJson и восстанавливаются`() {
        val filter = AlbumListFilter(
            album_type = com.client.xvideos.l.model.enum.AlbumType.Manga,
            content_id = com.client.xvideos.l.model.enum.ContentId.Hentai,
            picture_count_rank = com.client.xvideos.l.model.enum.PictureCountRank.C50_100,
            genresPlus = listOf(genre()),
            tagPlus = listOf("tag1", "tag2"),
            selection = "animated"
        )
        val preset = com.client.xvideos.l.model.SavedAlbumFilter(name = "Manga Hentai", filter = filter)
        val json = com.client.xvideos.l.net.json.LJson.encodeToString(listOf(preset))
        val decoded = com.client.xvideos.l.net.json.LJson.decodeFromString<List<com.client.xvideos.l.model.SavedAlbumFilter>>(json)

        org.junit.Assert.assertEquals(1, decoded.size)
        org.junit.Assert.assertEquals("Manga Hentai", decoded[0].name)
        org.junit.Assert.assertEquals(com.client.xvideos.l.model.enum.AlbumType.Manga, decoded[0].filter.album_type)
        org.junit.Assert.assertEquals(com.client.xvideos.l.model.enum.ContentId.Hentai, decoded[0].filter.content_id)
        org.junit.Assert.assertEquals(com.client.xvideos.l.model.enum.PictureCountRank.C50_100, decoded[0].filter.picture_count_rank)
        org.junit.Assert.assertEquals("animated", decoded[0].filter.selection)
        org.junit.Assert.assertEquals(1, decoded[0].filter.genresPlus.size)
        org.junit.Assert.assertEquals("Genre", decoded[0].filter.genresPlus[0].title)
    }
}
