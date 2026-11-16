package com.client.xvideos.n.ui.screens.explorer.tab.saved.likes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel вкладки лайков страниц манги nhentai.
 */
@Stable
class ScreenSavedNLikesSM @Inject constructor(
    private val saved: NSaved
) : ScreenModel {

    val likes: SnapshotStateList<NPicture> = saved.likes.likes

    val columns = Settings.n_likesTab_column_current_count.field

    fun removeLike(picture: NPicture) {
        saved.likes.removeLike(picture.id)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenSavedNLikesModule {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedNLikesSM::class)
    abstract fun bindScreenSavedNLikesSM(sm: ScreenSavedNLikesSM): ScreenModel
}
