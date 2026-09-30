package com.client.xvideos.l.ui.screens.explorer.tab.saved.likes

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

enum class AllImagGif {
    ALL, IMAGE, GIF
}

@Stable
class ScreenSavedLLikesSM @Inject constructor(
    val savedL: SavedL
) : ScreenModel {

    val host = LazyRowPictureDetailsHost("l_likes")

    /**
     * Выбор типа отображаемого контента
     */
    private var selectorFilter = AllImagGif.ALL

    val original = savedL.likes.listUrl

    init {
        filterSelect(selectorFilter)
    }

    fun filterSelect(item: AllImagGif) {
        when (item) {
            AllImagGif.ALL   -> { selectAll() }
            AllImagGif.IMAGE -> { selectImage() }
            AllImagGif.GIF   -> { selectGif() }
        }
    }

    fun filterSelect(index: Int) {
        when (index) {
            0 -> {
                selectorFilter = AllImagGif.ALL
                selectAll()
            }
            1 -> {
                selectorFilter = AllImagGif.IMAGE
                selectImage()
            }
            2 -> {
                selectorFilter = AllImagGif.GIF
                selectGif()
            }
        }
    }

    fun delete(item: PicsDetails) {
        val url = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl() ?: return
        savedL.likes.remove(url)
    }

    fun selectGif() {
        host.replaceFilteredPictures(original.filter { it.is_animated })
    }

    fun selectImage() {
        host.replaceFilteredPictures(original.filter { !it.is_animated })
    }

    fun selectAll() {
        host.replaceFilteredPictures(original)
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedLikes {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedLLikesSM::class)
    abstract fun bindScreenLSavedLikesScreenModel(hiltListScreenModel: ScreenSavedLLikesSM): ScreenModel
}
