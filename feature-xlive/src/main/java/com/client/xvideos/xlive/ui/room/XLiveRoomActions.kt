package com.client.xvideos.xlive.ui.room

import androidx.compose.runtime.Immutable

/**
 * Нажатия и события экрана комнаты одним значением: по отдельности они
 * раздували сигнатуру экрана сверх порога detekt.
 *
 * @property onRetry «Повторить», когда комната не загрузилась или поток упал.
 * @property onToggleMute кнопка звука на видео.
 * @property onPlayerStarted плеер начал играть адрес потока.
 * @property onPlayerError плеер упал; аргумент — короткая причина.
 * @property onBuffering плеер ждёт данных или снова играет.
 * @property onToggleAbout развернуть или свернуть «О себе».
 * @property onToggleSaved кнопка «в мои» в шапке.
 * @property onToggleFollow «подписаться» в шапке, когда есть вход.
 * @property onSignIn «подписаться» в шапке без входа: страница входа.
 * @property onOpenTag тап по тегу темы; аргумент — тег без `#`.
 */
@Immutable
data class XLiveRoomActions(
    val onRetry: () -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onPlayerStarted: (String) -> Unit = {},
    val onPlayerError: (String) -> Unit = {},
    val onBuffering: (Boolean) -> Unit = {},
    val onToggleAbout: () -> Unit = {},
    val onToggleSaved: () -> Unit = {},
    val onToggleFollow: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onOpenTag: (String) -> Unit = {},
)
