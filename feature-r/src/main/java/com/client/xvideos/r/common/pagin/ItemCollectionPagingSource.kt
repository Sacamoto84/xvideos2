package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * [PagingSource] для отображения элементов сохраненной пользовательской коллекции.
 *
 * Считывает элементы из [SavedRed.collections] по имени [collection].
 *
 * @property collection Имя открытой коллекции.
 * @property savedRed Фасад локальных данных.
 */
class ItemCollectionPagingSource(val collection: String?, val savedRed: SavedRed) : PagingSource<Int, GifsInfo>() {

    /** Проверяет, задано ли непустое имя коллекции. */
    val hasCollection: Boolean get() = !collection.isNullOrBlank()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GifsInfo> {

        return try {
            Timber.d("!!! ItemCollectionPagingSource::load() collection:${collection}")

            val items = if (!collection.isNullOrBlank()) {
                savedRed.collections.collectionList.firstOrNull { it.collection == collection }?.items ?: emptyList()
            } else {
                emptyList()
            }

            LoadResult.Page( data = items.sanitizeGifsInfoList(), prevKey = null, nextKey = null )

        } catch (e: CancellationException) {
            throw e // G1
        } catch (e: Exception) {
            // G2: без SnackBar из data-слоя; ошибку отрисует UI по LoadState.
            Timber.e(e, "!!! ItemCollectionPagingSource::load() collection:$collection")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? {
        return null
    }
}
