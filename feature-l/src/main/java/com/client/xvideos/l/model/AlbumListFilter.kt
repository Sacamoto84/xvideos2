package com.client.xvideos.l.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import java.io.Serializable

/**
 * Модель фильтрации списка альбомов Luscious.
 *
 * `Serializable` обязателен: фильтр лежит в экране, который создаёт
 * `L_ScreenAlbumList.create`, а экраны Voyager (`Screen : Serializable`) целиком
 * уходят в saved state активити. Без этого приложение падает
 * `NotSerializableException`, когда система сохраняет состояние.
 *
 * @property display Сортировка/отображение альбомов (например, `"date_newest"`).
 * @property album_type Тип альбомов ([AlbumType]).
 * @property audienceIds Строка включенных аудиторий (например, `"+1+10+12+2+3+5+6+8+9"`).
 * @property languageIds Строка включенных языков (например, `"+1+100+101+2+3+4+5+6+7+8+9+99"`).
 * @property itemsPerPage Количество элементов на страницу (по умолчанию 30).
 * @property picture_count_rank Фильтр по числу картинок ([PictureCountRank]).
 * @property content_id Категория контента ([ContentId]).
 * @property genresPlus Включенные жанры.
 * @property genresMinus Исключенные жанры.
 * @property tagPlus Включенные теги.
 * @property tagMinus Исключенные теги.
 * @property searchQuery Строка текстового поиска.
 * @property selection Дополнительная выборка (например, `"animated"`).
 */
@Immutable
@kotlinx.serialization.Serializable
data class AlbumListFilter(
    val display: String = "date_newest",
    val album_type: AlbumType = AlbumType.Pictures, //manga pictures или все при отсуствии
    val audienceIds: String = "+1+10+12+2+3+5+6+8+9",
    val languageIds: String = "+1+100+101+2+3+4+5+6+7+8+9+99", //Все языки
    val itemsPerPage: Int = 30,
    val picture_count_rank : PictureCountRank = PictureCountRank.All,
    val content_id : ContentId = ContentId.All,
    val genresPlus : List<FilterGenre> = emptyList(),
    val genresMinus: List<FilterGenre> = emptyList(),
    val tagPlus : List<String> = emptyList(),
    val tagMinus : List<String> = emptyList(),
    val searchQuery : String = "",
    val selection : String = "",
) : Serializable {
    val hasSearchQuery: Boolean get() = searchQuery.isNotBlank()
    val hasTags: Boolean get() = tagPlus.isNotEmpty() || tagMinus.isNotEmpty()
    val hasGenres: Boolean get() = genresPlus.isNotEmpty() || genresMinus.isNotEmpty()
    val hasSelection: Boolean get() = selection.isNotBlank()
    val isAnimatedOnly: Boolean get() = selection.equals("animated", ignoreCase = true)
    val isFiltered: Boolean get() = hasSearchQuery || hasTags || hasGenres || hasSelection
    val totalFilterCount: Int
        get() = (if (hasSearchQuery) 1 else 0) +
            (if (hasSelection) 1 else 0) +
            tagPlus.size + tagMinus.size +
            genresPlus.size + genresMinus.size

    companion object {
        val DEFAULT = AlbumListFilter()
    }
}
