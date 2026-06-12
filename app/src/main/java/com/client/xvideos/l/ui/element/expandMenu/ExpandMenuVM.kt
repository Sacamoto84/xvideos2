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
import com.client.xvideos.l.featured.share.useCaseShareFile
import com.client.xvideos.l.model.PicsDetails
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
import com.client.xvideos.l.featured.saved.lFindLikeFolder
import com.client.xvideos.l.featured.saved.readLSavedLikeMetadata
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Immutable
enum class ExpandMenuType {
    NONE,
    ALBUM,
    LIKES
}


/**
 *  val expandMenuViewModel: ExpandMenuViewModel = hiltViewModel()
 */
@HiltViewModel
class ExpandMenuViewModel @Inject constructor(
    val luscious: Luscious,
    val saved: SavedL,
    @ApplicationScope val scope: CoroutineScope,
    @ApplicationContext val context: Context
) : ViewModel() {


    @Composable
    fun ExpandMenu(type: ExpandMenuType, item: PicsDetails, idAlbum: String, isCollection: Boolean = false) {
        when (type) {
            ExpandMenuType.NONE -> {}
            ExpandMenuType.ALBUM -> ExpandMenuAlbum(item, idAlbum, isCollection)
            ExpandMenuType.LIKES -> ExpandMenuLikes(item, isCollection)
        }
    }

    ////


    @Composable
    fun ExpandMenuAlbum(item: PicsDetails, idAlbum: String, isCollection: Boolean = false) {

        val album = when(idAlbum){
            "likes" -> 0
            else -> idAlbum.toLong()
        }

        AlbumItemExpandMenu(
            item = item, onDownload = { it1 -> downloadLike(it1, album) },
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


    @Composable
    fun ExpandMenuLikes(item: PicsDetails, isCollection: Boolean = false) {
        val haptic = LocalHapticFeedback.current
        SavedLikesItemExpandMenu(
            item,
            onDelete = { it ->
                item.url_to_original?.let { url -> saved.likes.remove(url) }
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            onShare = { it -> onShareClicked(it) },
            onSaveToGallery = { it -> saveToGallery(it) },
            isCollection = isCollection,
            savedL = saved,
            onRemoveFromCollection = { it ->
                // Refresh will be handled by the collection screen
            }
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
            Timber.i("!!! share item = ${item.url_to_original} isAnimated: ${item.is_animated}")
            try {
                val file = lDownloadMediaToShareCache(item)
                if (file == null) {
                    SnackBar.error("Нет ссылки для файла")
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    useCaseShareFile(context, file)
                }
            } catch (e: Exception) {
                Timber.e(e, "L share -> ошибка при работе с файлом")
                SnackBar.error("Ошибка при попытке поделиться файлом")
            }
        }
    }

    /**
     * «В галерею»: большой файл (оригинал/видео, не превью) → /sdcard/xvideos_download.
     * Сохранённый лайк — файл берётся из папки item (metadata.mediaFileName),
     * иначе оригинал скачивается через share-кеш.
     */
    fun saveToGallery(item: PicsDetails) {
        scope.launch(Dispatchers.IO) {
            try {
                val folder = item.url_to_original?.let { lFindLikeFolder(File(AppPath.l_likes), it) }
                val localBig = folder
                    ?.let { f ->
                        readLSavedLikeMetadata(File(f, L_METADATA_FILE_NAME))
                            ?.let { File(f, it.mediaFileName) }
                    }
                    ?.takeIf { it.exists() }

                val src = localBig ?: run {
                    SnackBar.info("Сохранение в галерею…")
                    lDownloadMediaToShareCache(item)
                }
                if (src == null) {
                    SnackBar.error("Нет файла для сохранения")
                    return@launch
                }
                GallerySaver.saveLocal(context, src, src.name)
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
        val url = item.url_to_original
        val folder = url?.let { lFindLikeFolder(File(AppPath.l_likes), it) }
        val bundle = folder?.let { LExporter.export(it) }
        // Нет в Likes (или бандл битый) — экран отправки скачает item в outbox,
        // не помечая его сохранённым.
        p2pSource = if (bundle != null) P2pSendSource.Ready(bundle) else P2pSendSource.DownloadL.of(item)
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

