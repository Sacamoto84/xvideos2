package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.saved.SavedRed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

@Stable
class ScreenSavedNichesSM @Inject constructor(
    val savedRed: SavedRed
) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedNichesSM::class)
    abstract fun bindScreenRedSavedNichesScreenModel(screenModel: ScreenSavedNichesSM): ScreenModel
}
