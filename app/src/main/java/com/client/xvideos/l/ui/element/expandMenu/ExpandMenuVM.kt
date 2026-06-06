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
            onShare = { it1 -> share(it1) },
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

}

