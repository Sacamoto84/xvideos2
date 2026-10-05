package com.client.xvideos.l.ui.screens.explorer

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.LLoginFormState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel L-раздела. Держит ссылку на singleton [SavedL], чтобы
 * корневой L-экран мог показывать общие диалоги (создание/добавление коллекции)
 * и индикатор загрузок без обращения к глобальному состоянию.
 */
@Stable
class L_ScreenExplorerSM @Inject constructor(
    val savedL: SavedL,
    private val navigationState: LNavigationState
) : ScreenModel {
    /** Текущая вкладка верхнего уровня L-раздела (раньше — статика в Companion). */
    var screenType: Int
        get() = navigationState.rootTab
        set(value) {
            navigationState.rootTab = value
        }

    /** Форма входа, которую раздел показывает, пока профиль не задан. */
    val loginForm = LLoginFormState(Settings.l_profile.field.value)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class L_ScreenExplorerModule {
    @Binds
    @IntoMap
    @ScreenModelKey(L_ScreenExplorerSM::class)
    abstract fun bindL_ScreenExplorerSM(sm: L_ScreenExplorerSM): ScreenModel
}
