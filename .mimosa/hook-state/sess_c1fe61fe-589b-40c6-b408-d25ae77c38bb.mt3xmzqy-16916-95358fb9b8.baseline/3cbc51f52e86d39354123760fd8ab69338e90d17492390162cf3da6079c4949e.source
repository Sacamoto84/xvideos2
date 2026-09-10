package com.client.xvideos.l.ui.screens.screenAlbum

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.featured.saved.LAlbumExporter
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.AlbumDetails
import java.io.File
import com.client.xvideos.l.featured.share.lDownloadMediaToShareCache
import com.client.xvideos.common.share.useCaseShareFile
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.AlbumInfo
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ScreenLAlbumSM @AssistedInject constructor(
    @Assisted val idAlbum: Long,
    val luscious: Luscious,
    val saved: SavedL,
    @ApplicationScope val scope: CoroutineScope,
    @ApplicationContext val context: Context
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(idAlbum: Long): ScreenLAlbumSM
    }

    val host = LazyRowPictureDetailsHost(idAlbum.toString(), idAlbum.toString())

    val albumInfo = MutableStateFlow<AlbumInfo?>(
        luscious.getAlbum(idAlbum, requestScope = screenModelScope)
    )

    /**
     * Показ только анимированных картинок
     */
    var showOnlyAnimated by mutableStateOf(false)

    /**
     * Сохранить альбом
     */
    fun saveAlbum() {
        scope.launch {
            albumInfo.value?.let { saved.albums.add(it.albumInfo.value) }
        }
    }

    /** Источник для экрана P2P-отправки альбома; экран наблюдает и пушит ScreenP2pSend. */
    var p2pAlbumSource by mutableStateOf<P2pSendSource?>(null)
        private set

    fun dismissP2pAlbum() { p2pAlbumSource = null }

    /**
     * Поделиться альбомом по P2P: бандл — только файл метаданных `<id>.album`
     * (из l_albums, если альбом сохранён, иначе пишется в outbox-зеркало).
     */
    fun shareAlbumP2p(album: AlbumDetails) {
        scope.launch(Dispatchers.IO) {
            val outboxAlbumRoot = mirrorRoot(
                base = File(AppPath.p2p_outbox),
                mainRoot = File(AppPath.main),
                storeRoot = File(AppPath.l_albums),
            )
            val bundle = LAlbumExporter.export(album, File(AppPath.l_albums), outboxAlbumRoot)
            if (bundle == null) {
                SnackBar.error("Не удалось подготовить альбом для P2P")
                return@launch
            }
            withContext(Dispatchers.Main) {
                p2pAlbumSource = P2pSendSource.Ready(bundle)
            }
        }
    }

    fun downloadLike(item: PicsDetails) {
        saved.likes.add(item.copy(album = idAlbum.toString()))
    }

    init {
        Timber.e("!!! ScreenLAlbumSM init")
    }

    override fun onDispose() {
        super.onDispose()
        Timber.e("!!! ScreenLAlbumSM onDispose")
    }


    fun share(item: PicsDetails) {
        // Скачивание/запись файла — на IO (потоково), системный share — на Main.
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "L share -> ошибка при работе с файлом")
                SnackBar.error("Ошибка при попытке поделиться файлом")
            }
        }
    }

    fun retryFailedAlbumPages() {
        screenModelScope.launch {
            albumInfo.value?.albumPicsDetails?.retryFailedPages()
        }
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbum {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLAlbumSM.Factory::class)
    abstract fun bindHiltProfilesScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenLAlbumSM.Factory
    ): ScreenModelFactory

}
