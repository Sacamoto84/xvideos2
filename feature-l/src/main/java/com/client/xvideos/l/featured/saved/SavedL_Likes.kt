package com.client.xvideos.l.featured.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.Luscious
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 *
 * @param luscious Ссылка на сервис API Luscious.
 * @param scope Область корутин для выполнения фоновых дисковых операций.
 */
@Stable
class SavedL_Likes(
    private val luscious: Luscious,
    private val scope: CoroutineScope
) {

    /** Реактивный список локально сохраненных понравившихся элементов. */
    val listUrl = mutableStateListOf<PicsDetails>()
    private val progress = LDownloadProgress(scope)
    /** Поток совокупного процента скачивания новых лайков. */
    val percentDownload: StateFlow<Float> = progress.percentDownload
    private var mutationJob: Job? = null

    init {
        refresh()
    }

    /**
     * Скачивает и сохраняет элемент медиа [item] в локальное хранилище лайков (`AppPath.l_likes`).
     *
     * @param item Элемент с медиафайлом и метаданными.
     */
    fun add(item: PicsDetails) {
        if (item.url_to_original.isNullOrBlank() && item.url_to_video.isNullOrBlank()) {
            Timber.w("SavedL_Likes: отклонён элемент без URL")
            SnackBar.error("Недопустимый URL для сохранения")
            return
        }
        Timber.i("SavedL_Likes addLikes() item:${item.url_to_original}")

        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            val result = lPersistPicsDetailsToFolder(
                item = item,
                root = File(AppPath.l_likes),
                luscious = luscious,
                progress = progress
            )
            withContext(Dispatchers.Main) {
                result
                    .onSuccess {
                        SnackBar.success("Добавлено в лайки")
                        refresh()
                    }
                    .onFailure {
                        Timber.e(it, "SavedL_Likes add() download error")
                        SnackBar.error("Ошибка добавления лайка")
                    }
            }
        }
    }

    /**
     * Удаляет сохраненный лайк по его локальному пути или сетевому URL [url].
     *
     * @param url Идентификатор пути к файлу или исходного URL.
     */
    fun remove(url: String) {
        if (url.isBlank()) return
        Timber.i("SavedL_Likes removeLikes() url:$url")

        // Вызов приходит из onDelete в composable, то есть с main-потока, а
        // deleteRecursively() по папке с медиа — это полноценный обход каталога.
        // Уносим на IO, как это уже сделано в add() и refresh().
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            val root = File(AppPath.l_likes)
            val folder = lFindLikeFolder(root, url)
            val file = File(url)

            val removed = when {
                folder != null -> folder.deleteRecursively()
                lIsInside(root, file) && file.exists() -> file.delete()
                else -> false
            }

            if (removed) {
                SnackBar.info("Удалено из лайков")
            } else {
                SnackBar.error("Файл не найден: $url")
            }
            refresh()
        }
    }

    private var refreshJob: Job? = null

    /**
     * Перечитывает список всех сохраненных лайков из каталога `AppPath.l_likes` на диске.
     */
    fun refresh() {
        // Чтение каталога с разбором каждого metadata.json делаем на IO,
        // обновление Compose-state — на Main, чтобы не блокировать UI (ANR).
        // Отменяем предыдущий незавершённый скан при повторном вызове,
        // исключая гонки устаревших результатов.
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            Timber.i("SavedL_Likes refresh()")
            val items = try {
                lReadCollectionItems(File(AppPath.l_likes))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Likes refresh() Ошибка получения списка likes")
                SnackBar.error("Ошибка получения списка likes")
                return@launch
            }
            withContext(Dispatchers.Main) {
                listUrl.replaceWith(items)
                Timber.i("SavedL_Likes refresh() files:${items.size}")
            }
        }
    }
}
