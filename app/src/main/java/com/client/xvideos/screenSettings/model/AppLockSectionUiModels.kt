package com.client.xvideos.screenSettings.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.screenSettings.components.AppLockDialogMode

/** Состояние модальных диалогов секции блокировки приложения. */
@Immutable
internal data class AppLockDialogsUiState(
    val dialogMode: AppLockDialogMode?,
    val duressDialogMode: DuressDialogMode?,
    val showCamouflageVerificationDialog: Boolean,
    val showTimeoutDialog: Boolean,
    val currentTimeout: AppLockTimeout,
)

/** Обработчики событий модальных диалогов секции блокировки приложения. */
@Immutable
internal data class AppLockDialogsActions(
    val onDismissDialogMode: () -> Unit,
    val onPasswordDialogComplete: () -> Unit,
    val onDismissDuressDialogMode: () -> Unit,
    val onDuressDialogComplete: () -> Unit,
    val onDismissCamouflageVerification: () -> Unit,
    val onSelectTimeout: (AppLockTimeout) -> Unit,
    val onDismissTimeoutDialog: () -> Unit,
    val onDismissAllDialogs: () -> Unit,
)

/** Отображаемое состояние переключателей и пунктов группы блокировки приложения. */
@Immutable
internal data class AppLockSettingsGroupUiState(
    val enabled: Boolean,
    val passwordSet: Boolean,
    val duressCodeSet: Boolean,
    val currentTimeout: AppLockTimeout,
    val keyboardIncognito: Boolean,
    val blurRecentTasks: Boolean,
)

/** Пользовательские действия в группе настроек блокировки приложения. */
@Immutable
internal data class AppLockSettingsGroupActions(
    val onSetOrChangeLock: () -> Unit,
    val onDisableLock: () -> Unit,
    val onShowTimeoutClick: () -> Unit,
    val onSetDuressCode: () -> Unit,
    val onChangeDuressCode: () -> Unit,
    val onRemoveDuressCode: () -> Unit,
    val onEnableCamouflageRequested: () -> Unit,
    val onToggleIncognito: (Boolean) -> Unit,
    val onToggleBlurRecent: (Boolean) -> Unit,
)
