package com.client.xvideos.l.ui.element.expandMenu

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.featured.share.lDownloadMediaToShareCache
import com.client.xvideos.common.share.useCaseShareFile
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.albumIdOrNull
import com.client.xvideos.l.model.asLAlbumIdOrNull
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.net.Luscious
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.LExporter
import com.client.xvideos.l.featured.saved.L_METADATA_FILE_NAME
import com.client.xvideos.l.featured.saved.LSavedLikeMetadata
import com.client.xvideos.l.featured.saved.lFindLikeFolder
import com.client.xvideos.l.featured.saved.lFindSavedItemFolder
import com.client.xvideos.l.featured.saved.lP2pSendSource
import com.client.xvideos.l.featured.saved.readLSavedLikeMetadata
import com.client.xvideos.l.featured.saved.writeLSavedLikeMetadata
import java.io.File
import com.client.xvideos.l.model.extractAnchorId
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.repository.toLUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@Immutable
enum class ExpandMenuType {
    NONE,
    ALBUM,
    LIKES,
    SERVER_LIKES
}


/**
 *  val expandMenuViewModel: ExpandMenuViewModel = hiltViewModel()
 */
@HiltViewModel
class ExpandMenuViewModel @Inject constructor(
    val luscious: Luscious,
    val saved: SavedL,
    val serverFavorites: LusciousServerFavoritesRepository,
    @ApplicationScope val scope: CoroutineScope,
    @ApplicationContext val context: Context
) : ViewModel() {

    /**
     * Картинки, для которых лайк или снятие лайка уже в пути. Повторное нажатие
     * до ответа ничего не шлёт: раньше второй запрос на снятие отвечал ошибкой,
     * и за «Лайк удалён» следом шло «Не удалось удалить лайк».
     */
    private val serverActionsInFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun likeOnServer(item: PicsDetails) = launchServerAction(item) { pictureId ->
        serverFavorites.likePicture(pictureId)
            .onSuccess {
                SnackBar.success("Лайк добавлен на сервере")
            }
            .onFailure { e ->
                Timber.e(e, "Failed to like picture on server")
                SnackBar.error("Не удалось поставить лайк: ${e.toLUserMessage()}")
            }
    }

    fun unlikeOnServer(item: PicsDetails, onSuccess: (() -> Unit)? = null) = launchServerAction(item) { pictureId ->
        serverFavorites.unlikePicture(pictureId)
            .onSuccess {
                SnackBar.info("Лайк удалён на сервере")
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke()
                }
            }
            .onFailure { e ->
                Timber.e(e, "Failed to unlike picture on server")
                SnackBar.error("Не удалось удалить лайк: ${e.toLUserMessage()}")
            }
    }

    /** Выполняет [action] с id картинки на сервере, если для неё ещё нет запроса в пути. */
    private fun launchServerAction(item: PicsDetails, action: suspend (pictureId: String) -> Unit) {
        val knownId = item.extractAnchorId()?.takeIf { it.isNotBlank() }
        val key = knownId ?: item.sourceUrl()
        if (!serverActionsInFlight.add(key)) return

        scope.launch(Dispatchers.IO) {
            try {
                val pictureId = knownId ?: resolvePictureId(item) ?: return@launch
                action(pictureId)
            } finally {
                serverActionsInFlight.remove(key)
            }
        }
    }

    private fun PicsDetails.sourceUrl(): String = url_to_original ?: url_to_video ?: lDownloadUrl().orEmpty()

    /**
     * id картинки, которого нет в самом объекте (старый локальный лайк): ищет
     * его в папке сохранённого элемента, затем на сервере. `null` — не нашёлся,
     * причина пользователю уже показана.
     */
    private suspend fun resolvePictureId(item: PicsDetails): String? {
        val (folder, localPictureId) = findLocalFolderAndPictureId(item)
        if (!localPictureId.isNullOrBlank()) return localPictureId

        val metadata = folder?.let { readLSavedLikeMetadata(File(it, L_METADATA_FILE_NAME)) }
        val albumId = metadata?.albumId.asLAlbumIdOrNull() ?: item.albumIdOrNull

        val slugCandidate = metadata?.sourceMediaUrl?.takeIf { it.isNotBlank() }
            ?: metadata?.sourcePreviewUrl?.takeIf { it.isNotBlank() }
            ?: folder?.name
            ?: item.sourceUrl()

        if (albumId.isNullOrBlank() || slugCandidate.isBlank()) {
            SnackBar.error("ID картинки не найден")
            return null
        }

        SnackBar.info("Поиск ID на сервере…")

        val resolvedId = serverFavorites.resolvePictureId(albumId, slugCandidate).getOrElse { error ->
            // С причиной: обрыв сети и поиск, остановленный на пределе страниц,
            // раньше выглядели одинаково — «не найден».
            SnackBar.error("ID картинки не найден на сервере: ${error.toLUserMessage()}")
            return null
        }
        if (resolvedId.isBlank()) {
            SnackBar.error("ID картинки не найден на сервере")
            return null
        }

        cacheResolvedPictureId(folder, metadata, resolvedId)
        return resolvedId
    }

    private fun findLocalFolder(url: String?): File? =
        url?.takeIf { it.isNotBlank() }?.let {
            lFindSavedItemFolder(File(AppPath.l_likes), File(AppPath.l_collection), it)
        }

    private fun findLocalFolderAndPictureId(item: PicsDetails): Pair<File?, String?> {
        val folder = findLocalFolder(item.sourceUrl())
        val metadata = folder?.let { readLSavedLikeMetadata(File(it, L_METADATA_FILE_NAME)) }
        val metaPictureId = metadata?.pictureId?.takeIf { it.isNotBlank() }
            ?: metadata?.picture?.id?.takeIf { it.isNotBlank() }
            ?: metadata?.picture?.extractAnchorId()
        return folder to metaPictureId
    }

    private fun cacheResolvedPictureId(folder: File?, metadata: LSavedLikeMetadata?, resolvedId: String) {
        if (folder != null && metadata != null) {
            runCatching {
                val updated = metadata.copy(
                    pictureId = resolvedId,
                    picture = metadata.picture.copy(id = resolvedId)
                )
                writeLSavedLikeMetadata(File(folder, L_METADATA_FILE_NAME), updated)
            }
        }
    }

    ///////
    fun downloadLike(item: PicsDetails, idAlbum: Long) {
        saved.likes.add(item.copy(album = idAlbum.toString()))
    }

    fun share(item: PicsDetails) {
        // Скачиваем и пишем файл на IO (потоково, без буферизации всего файла
        // в RAM), а системный share показываем на Main.
        scope.launch(Dispatchers.IO) {
            Timber.d("share item = ${item.url_to_original} isAnimated: ${item.is_animated}")
            try {
                val file = lDownloadMediaToShareCache(item)
                if (file == null) {
                    SnackBar.error("Нет ссылки для файла")
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    useCaseShareFile(context, file)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "L share -> ошибка при работе с файлом")
                SnackBar.error("Ошибка при попытке поделиться файлом")
            }
        }
    }

    /**
     * «В галерею»: большой файл (оригинал/видео, не превью) → общая галерея.
     * Сохранённый лайк — файл берётся из папки item (metadata.mediaFileName),
     * иначе оригинал скачивается через share-кеш.
     */
    fun saveToGallery(item: PicsDetails) {
        scope.launch(Dispatchers.IO) {
            try {
                // И в лайках, и в коллекциях: раньше картинка из коллекции
                // качалась заново, хотя файл лежит на устройстве.
                val folder = findLocalFolder(item.sourceUrl())
                val localBig = folder
                    ?.let { f ->
                        readLSavedLikeMetadata(File(f, L_METADATA_FILE_NAME))
                            ?.let { File(f, it.mediaFileName) }
                    }
                    ?.takeIf { it.exists() && it.length() > 0L }

                val src = localBig ?: run {
                    SnackBar.info("Сохранение в галерею…")
                    lDownloadMediaToShareCache(item)
                }
                if (src == null) {
                    SnackBar.error("Нет файла для сохранения")
                    return@launch
                }
                GallerySaver.saveLocal(context, src, src.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "L saveToGallery -> ошибка")
                SnackBar.error("Ошибка сохранения в галерею")
            }
        }
    }

    // ---- P2P share ----

    var p2pChooserItem by mutableStateOf<PicsDetails?>(null)
        private set
    var p2pSource by mutableStateOf<P2pSendSource?>(null)
        private set

    fun onShareClicked(item: PicsDetails) { p2pChooserItem = item }
    fun dismissChooser() { p2pChooserItem = null }
    fun dismissP2p() { p2pSource = null }

    fun startP2p(item: PicsDetails) {
        scope.launch(Dispatchers.IO) {
            val url = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl()
            val folder = url?.let { lFindLikeFolder(File(AppPath.l_likes), it) }
            val bundle = folder?.let { LExporter.export(it) }
            // Нет в Likes (или бандл битый) — экран отправки скачает item в outbox,
            // не помечая его сохранённым.
            val source = if (bundle != null) P2pSendSource.Ready(bundle) else lP2pSendSource(item)
            withContext(Dispatchers.Main) {
                p2pSource = source
            }
        }
    }

}

