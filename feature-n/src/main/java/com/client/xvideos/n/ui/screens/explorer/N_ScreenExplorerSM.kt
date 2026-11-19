package com.client.xvideos.n.ui.screens.explorer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel корневого экрана раздела nhentai.
 */
@Stable
class N_ScreenExplorerSM @Inject constructor() : ScreenModel {

    /**
     * Текущая активная вкладка раздела (0 — манга, 1 — сохранённое, 2 — поиск).
     */
    var screenType by mutableIntStateOf(0)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_ScreenExplorerModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenExplorerSM::class)
    abstract fun bindN_ScreenExplorerSM(sm: N_ScreenExplorerSM): ScreenModel
}
