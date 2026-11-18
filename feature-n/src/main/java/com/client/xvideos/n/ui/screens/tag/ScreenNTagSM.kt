package com.client.xvideos.n.ui.screens.tag

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryPages
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * ScreenModel экрана галерей по выбранному тегу или автору с постраничной пагинацией.
 */
@Stable
class ScreenNTagSM @AssistedInject constructor(
    @Assisted val tag: NTag,
    private val repository: NRepository
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(tag: NTag): ScreenNTagSM
    }

    val columns = Settings.n_galleriesTab_column_current_count.field
    val pages = NGalleryPages(screenModelScope) { number ->
        repository.getGalleriesByTagPage(tag = tag, page = number, sort = NSortType.RECENT)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNTag {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenNTagSM.Factory::class)
    abstract fun bindScreenNTagSMFactory(
        factory: ScreenNTagSM.Factory
    ): ScreenModelFactory
}
