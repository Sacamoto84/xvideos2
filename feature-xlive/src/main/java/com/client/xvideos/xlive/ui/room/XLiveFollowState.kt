package com.client.xvideos.xlive.ui.room

import androidx.compose.runtime.Immutable

/**
 * Кнопка «подписаться» в шапке комнаты.
 *
 * @property isSignedIn есть вход на сайт; без него кнопка ведёт ко входу.
 * @property following аккаунт подписан на комнату.
 * @property isBusy запрос подписки или отписки в пути — второе нажатие не в счёт.
 */
@Immutable
data class XLiveFollowState(
    val isSignedIn: Boolean = false,
    val following: Boolean = false,
    val isBusy: Boolean = false,
)
