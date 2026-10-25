package com.client.xvideos.xlive.ui.following

import androidx.compose.runtime.Immutable

/**
 * Действия подвкладки «Подписки», кроме сетки — у неё действия «Моих».
 *
 * @property onSignIn «Войти»: страница входа сайта.
 * @property onSignOut «Выйти» — после подтверждения.
 * @property onRetry «Повторить», когда список не пришёл.
 */
@Immutable
data class XLiveFollowingActions(
    val onSignIn: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onRetry: () -> Unit = {},
)
