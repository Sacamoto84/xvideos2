package com.client.xvideos.l.ui.element.expandMenu

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModel
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.featured.share.lDownloadMediaToShareCache
import com.client.xvideos.common.share.useCaseShareFile
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.net.Luscious
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.LExporter
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.l.featured.saved.L_METADATA_FILE_NAME
import com.client.xvideos.l.featured.saved.LSavedLikeMetadata
import com.client.xvideos.l.featured.saved.lFindLikeFolder
import com.client.xvideos.l.featured.saved.lP2pSendSource
import com.client.xvideos.l.featured.saved.readLSavedLikeMetadata
import com.client.xvideos.l.featured.saved.writeLSavedLikeMetadata
import java.io.File
import com.client.xvideos.l.model.extractAnchorId
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import kotlinx.coroutines.withContext
import timber.log.Timber
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


    @Composable
    fun ExpandMenu(
        type: ExpandMenuType,
        item: PicsDetails,
        idAlbum: String,
        isCollection: Boolean = false,
        host: LazyRowPictureDetailsHost? = null
    ) {
        when (type) {
            ExpandMenuType.NONE -> {}
            ExpandMenuType.ALBUM -> ExpandMenuAlbum(item, idAlbum, isCollection)
            ExpandMenuType.LIKES -> ExpandMenuLikes(item, isCollection)
            ExpandMenuType.SERVER_LIKES -> ExpandMenuServerLikes(item, idAlbum, host)
        }
    }

    ////


    @Composable
    fun ExpandMenuAlbum(item: PicsDetails, idAlbum: String, isCollection: Boolean = false) {

        // Сюда прилетает и albumName (L_FullScreenImage), а он бывает нечисловым:
        // "l_likes" у лайков, имя папки у коллекций. toLong() на таком падал бы
        // прямо в композиции. Нечисловой источник = псевдоальбом, id 0.
        val album = idAlbum.toLongOrNull() ?: 0L

        AlbumItemExpandMenu(
            item = item,
            onDownload = { it1 -> downloadLike(it1, album) },
            onServerLike = { it1 -> likeOnServer(it1) },
            onShare = { it1 -> onShareClicked(it1) },
            onSaveToGallery = { it1 -> saveToGallery(it1) },
            isCollection = isCollection,
            savedL = saved,
            onRemoveFromCollection = { it ->
                // Refresh will be handled by the collection screen
            },
            idAlbum = idAlbum
        )
    }

    fun likeOnServer(item: PicsDetails) {
        val anchorId = item.extractAnchorId()
        if (!anchorId.isNullOrBlank()) {
            sendServerLike(anchorId)
            return
        }

        resolveAndPerformServerAction(item) { resolvedId ->
            sendServerLike(resolvedId)
        }
    }

    fun unlikeOnServer(item: PicsDetails, onSuccess: (() -> Unit)? = null) {
        val anchorId = item.extractAnchorId()
        if (!anchorId.isNullOrBlank()) {
            sendServerUnlike(anchorId, onSuccess)
            return
        }

        resolveAndPerformServerAction(item) { resolvedId ->
            sendServerUnlike(resolvedId, onSuccess)
        }
    }

    private fun resolveAndPerformServerAction(
        item: PicsDetails,
        onResolved: (String) -> Unit
    ) {
        // Если ID не найден в объекте (например, старый локальный лайк),
        // пробуем найти его в папке сохранённого элемента или разрешить через сервер
        scope.launch(Dispatchers.IO) {
            val (folder, localPictureId) = findLocalFolderAndPictureId(item)
            if (!localPictureId.isNullOrBlank()) {
                onResolved(localPictureId)
                return@launch
            }

            val metadata = folder?.let { readLSavedLikeMetadata(File(it, L_METADATA_FILE_NAME)) }
            val albumId = metadata?.albumId?.takeIf { it.isNotBlank() && it != "null" }
                ?: item.album?.takeIf { it.isNotBlank() && it != "null" }

            val targetUrl = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl().orEmpty()
            val slugCandidate = metadata?.sourceMediaUrl?.takeIf { it.isNotBlank() }
                ?: metadata?.sourcePreviewUrl?.takeIf { it.isNotBlank() }
                ?: folder?.name
                ?: targetUrl

            if (albumId.isNullOrBlank() || slugCandidate.isBlank()) {
                withContext(Dispatchers.Main) {
                    SnackBar.error("ID картинки не найден")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                SnackBar.info("Поиск ID на сервере…")
            }

            val resolvedId = serverFavorites.resolvePictureId(albumId, slugCandidate).getOrNull()
            if (resolvedId.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    SnackBar.error("ID картинки не найден на сервере")
                }
                return@launch
            }

            cacheResolvedPictureId(folder, metadata, resolvedId)
            onResolved(resolvedId)
        }
    }

    private fun findLocalFolderAndPictureId(item: PicsDetails): Pair<File?, String?> {
        val targetUrl = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl().orEmpty()
        val folder = targetUrl.takeIf { it.isNotBlank() }?.let {
            lFindLikeFolder(File(AppPath.l_likes), it)
                ?: lFindLikeFolder(File(AppPath.l_collection), it)
        }
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

    private fun sendServerLike(anchorId: String) {
        scope.launch {
            serverFavorites.likePicture(anchorId)
                .onSuccess {
                    SnackBar.success("Лайк добавлен на сервере")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to like picture on server")
                    SnackBar.error(e.message ?: "Не удалось поставить лайк")
                }
        }
    }

    private fun sendServerUnlike(anchorId: String, onSuccess: (() -> Unit)? = null) {
        scope.launch {
            serverFavorites.unlikePicture(anchorId)
                .onSuccess {
                    SnackBar.info("Лайк удалён на сервере")
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke()
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to unlike picture on server")
                    SnackBar.error(e.message ?: "Не удалось удалить лайк")
                }
        }
    }

    @Composable
    fun ExpandMenuLikes(item: PicsDetails, isCollection: Boolean = false) {
        val haptic = LocalHapticFeedback.current
        SavedLikesItemExpandMenu(
            item,
            onDelete = {
                val url = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl()
                url?.let { saved.likes.remove(it) }
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            onServerLike = { it1 -> likeOnServer(it1) },
            onShare = { it -> onShareClicked(it) },
            onSaveToGallery = { it -> saveToGallery(it) },
            isCollection = isCollection,
            savedL = saved,
            onRemoveFromCollection = { it ->
                // Refresh will be handled by the collection screen
            }
        )
    }

    @Composable
    fun ExpandMenuServerLikes(
        item: PicsDetails,
        idAlbum: String = "",
        host: LazyRowPictureDetailsHost? = null
    ) {
        ServerLikesItemExpandMenu(
            item = item,
            onDownload = { it1 ->
                val album = idAlbum.toLongOrNull() ?: it1.album?.toLongOrNull() ?: 0L
                downloadLike(it1, album)
            },
            onServerUnlike = { it1 ->
                unlikeOnServer(it1) {
                    host?.removePicture(it1)
                }
            },
            onShare = { it1 -> onShareClicked(it1) },
            onSaveToGallery = { it1 -> saveToGallery(it1) },
            savedL = saved,
            idAlbum = idAlbum
        )
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
                val targetUrl = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl()
                val folder = targetUrl?.let { lFindLikeFolder(File(AppPath.l_likes), it) }
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

    /**
     * Хост диалога P2P-шаринга. Должен компоноваться РОВНО ОДИН РАЗ на контейнер
     * (список/экран), не внутри per-item элементов — state общий на ViewModel,
     * каждый экземпляр хоста показал бы свой диалог.
     */
    @Composable
    fun P2pShareHost() {
        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.current
        p2pChooserItem?.let { item ->
            P2pSendChooserDialog(
                onSystem = { share(item) },
                onP2p = { startP2p(item) },
                onDismiss = { dismissChooser() },
            )
        }
        p2pSource?.let { source ->
            // Навигация — side effect, нельзя звать прямо из композиции:
            // рекомпозиции дублировали бы push.
            androidx.compose.runtime.LaunchedEffect(source) {
                navigator?.push(ScreenP2pSend(source))
                dismissP2p()
            }
        }
    }

}

