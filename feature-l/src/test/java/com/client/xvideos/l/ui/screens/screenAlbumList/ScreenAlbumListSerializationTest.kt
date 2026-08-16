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
        // Фильтр переживает не только экран: он же уходит в аргументы навигации
        // и в сохранённое состояние списка.
        serialize(AlbumListFilter(genresPlus = listOf(genre())))
    }
}
