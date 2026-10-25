package com.client.xvideos.xlive.ui.following

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.ui.saved.XLiveSavedState

/**
 * Подвкладка «Подписки»: подписки аккаунта на сайте.
 *
 * @property isSignedIn есть вход на сайт; без него — только кнопка «Войти».
 * @property list подписки той же сеткой, что «Мои»: сначала в эфире; `isChecking` — свайп вниз.
 * @property online сколько подписок в эфире.
 * @property isLoading грузится список, которого ещё нет на экране.
 * @property error список не пришёл.
 */
@Immutable
data class XLiveFollowingState(
    val isSignedIn: Boolean = false,
    val list: XLiveSavedState = XLiveSavedState(),
    val online: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)
