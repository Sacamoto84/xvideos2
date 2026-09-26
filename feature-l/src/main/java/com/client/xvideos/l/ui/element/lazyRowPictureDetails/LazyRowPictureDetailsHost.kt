package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.l.model.PicsDetails

/**
 * Хост состояния для списков и сеток картинок/медиа в альбомах и коллекциях Luscious.
 *
 * Инкапсулирует состояние скролла (StaggeredGrid и LazyList), выбранную картинку,
 * фильтрованный список элементов и обработку удаления элементов.
 *
 * @property albumName Название отображаемого альбома или псевдоальбома.
 * @property idAlbum Строковый идентификатор альбома (по умолчанию пустой).
 */
@OptIn(ExperimentalFoundationApi::class)
class LazyRowPictureDetailsHost(
    val albumName: String,
    val idAlbum: String = ""
) {

    /** Окно упреждающего кэширования для плавного скролла списка. */
    val dpCacheWindow = LazyLayoutCacheWindow(ahead = 150.dp, behind = 100.dp)

    /** Состояние скролла для ступенчатой сетки (staggered grid). */
    @OptIn(ExperimentalFoundationApi::class)
    val state = LazyStaggeredGridState()

    /** Альтернативное состояние скролла для линейного списка с кастомным окном кэширования. */
    val state1 = LazyListState(cacheWindow = dpCacheWindow)

    /**
     * Количество отображаемых столбцов в сетке.
     */
    var columns by mutableIntStateOf(3)

    /** Текущее выбранное пользователем изображение для детального просмотра или полноэкранного режима. */
    var selectedImage by mutableStateOf<PicsDetails?>(null)

    /** Список отфильтрованных картинок для текущего отображения. */
    var filteredPic = mutableStateListOf<PicsDetails>()

    /** Текущий поисковый запрос для локальной фильтрации элементов. */
    var collectionSearchQuery by mutableStateOf("")

    /** Обратный вызов, срабатывающий при успешном удалении элемента из списка. */
    var onItemRemoved: ((PicsDetails) -> Unit)? = null

    /**
     * Удаляет картинку [picture] из локального списка [filteredPic] и сбрасывает выбор, если она была активна.
     *
     * @param picture Удаляемый элемент [PicsDetails].
     * @return `true`, если элемент был найден и удален.
     */
    fun removePicture(picture: PicsDetails): Boolean {
        val targetKey = picture.selectionKey()
        val index = filteredPic.indexOfFirst {
            (!it.id.isNullOrBlank() && it.id == picture.id) ||
                it.selectionKey() == targetKey
        }
        if (index >= 0) {
            val removed = filteredPic.removeAt(index)
            if (selectedImage?.selectionKey() == targetKey) {
                selectedImage = null
            }
            onItemRemoved?.invoke(removed)
            return true
        }
        return false
    }

    /**
     * Заменяет текущий список отфильтрованных картинок [filteredPic] на [items],
     * избегая лишних обновлений, если элементы идентичны.
     *
     * @param items Новый список картинок.
     */
    fun replaceFilteredPictures(items: List<PicsDetails>) {
        if (filteredPic.hasSameItems(items)) return

        filteredPic.replaceWith(items)
    }

}

/**
 * Генерирует детерминированный строковый ключ идентичности картинки [PicsDetails]
 * для сопоставления при выделении и удалении.
 */
fun PicsDetails.selectionKey(): String {
    return url_to_original
        ?: url_to_video
        ?: thumbnails?.firstOrNull { !it.url.isNullOrBlank() }?.url
        ?: "${album.orEmpty()}-$width-$height-${is_animated}"
}

/**
 * Сравнивает два списка [PicsDetails] по последовательности ключей [selectionKey].
 */
private fun List<PicsDetails>.hasSameItems(items: List<PicsDetails>): Boolean {
    if (size != items.size) return false
    return indices.all { index -> this[index].selectionKey() == items[index].selectionKey() }
}
