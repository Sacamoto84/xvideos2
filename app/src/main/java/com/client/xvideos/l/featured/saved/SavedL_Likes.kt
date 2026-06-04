package com.client.xvideos.l.featured.saved

import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.Luscious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Тонкий holder состояния для раздела «Likes» в L.
 *
 * Все операции с файловой системой и сетью вынесены в общие helper-функции
 * (`LMediaPersist`, `LCollectionFs`); этот класс держит публичный API и
 * Compose-state ([listUrl], [percentDownload]).
 */
class SavedL_Likes(
    private val luscious: Luscious,
    private val scope: CoroutineScope
) {

    val listUrl = mutableStateListOf<PicsDetails>()
    private val progress = LDownloadProgress(scope)
    val percentDownload: StateFlow<Float> = progress.percentDownload

    init {
        refresh()
    }

    fun add(item: PicsDetails) {
        Timber.i("SavedL_Likes addLikes() item:${item.url_to_original}")

        scope.launch(Dispatchers.IO) {
            val result = lPersistPicsDetailsToFolder(
                item = item,
                root = File(AppPath.l_likes),
                luscious = luscious,
                progress = progress
            )
            withContext(Dispatchers.Main) {
                result
                    .onSuccess {
                        SnackBar.success("Like")
                        refresh()
                    }
                    .onFailure {
                        Timber.e(it, "SavedL_Likes add() download error")
                        SnackBar.error("Ошибка добавления лайка")
                    }
            }
        }
    }

    fun remove(url: String) {
        Timber.i("SavedL_Likes removeLikes() url:$url")
        val root = File(AppPath.l_likes)
        val folder = lFindLikeFolder(root, url)
        val file = File(url)

        val removed = when {
            folder != null -> folder.deleteRecursively()
            lIsInside(root, file) && file.exists() -> file.delete()
            else -> false
        }

        if (removed) {
            SnackBar.info("Unlike")
        } else {
            SnackBar.error("Файл не найден: $url")
        }
        refresh()
    }

    fun refresh() {
        // Чтение каталога с разбором каждого metadata.json делаем на IO,
        // обновление Compose-state — на Main, чтобы не блокировать UI (ANR).
        scope.launch(Dispatchers.IO) {
            Timber.i("SavedL_Likes refresh()")
            val items = try {
                lReadCollectionItems(File(AppPath.l_likes))
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Likes refresh() Ошибка получения списка likes")
                SnackBar.error("Ошибка получения списка likes")
                return@launch
            }
            withContext(Dispatchers.Main) {
                listUrl.clear()
                listUrl.addAll(items)
                Timber.i("SavedL_Likes refresh() files:${items.size}")
            }
        }
    }
}
