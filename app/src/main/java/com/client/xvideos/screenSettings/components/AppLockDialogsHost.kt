package com.client.xvideos.screenSettings.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.screenSettings.model.AppLockDialogsActions
import com.client.xvideos.screenSettings.model.AppLockDialogsUiState
import com.client.xvideos.screenSettings.molecule.DuressCodeDialog

/** Хост модальных диалогов и обработки кнопки «Назад» для секции блокировки приложения. */
@Composable
internal fun AppLockDialogsHost(
    state: AppLockDialogsUiState,
    actions: AppLockDialogsActions,
) {
    val isAnyDialogOpen = state.dialogMode != null ||
        state.duressDialogMode != null ||
        state.showCamouflageVerificationDialog ||
        state.showTimeoutDialog
    BackHandler(enabled = isAnyDialogOpen, onBack = actions.onDismissAllDialogs)

    state.dialogMode?.let { mode ->
        AppLockPasswordDialog(
            mode = mode,
            onDismiss = actions.onDismissDialogMode,
            onComplete = actions.onPasswordDialogComplete,
        )
    }
    state.duressDialogMode?.let { mode ->
        DuressCodeDialog(
            mode = mode,
            onDismiss = actions.onDismissDuressDialogMode,
            onComplete = actions.onDuressDialogComplete,
        )
    }
    if (state.showCamouflageVerificationDialog) {
        CamouflageVerificationDialog(
            onDismiss = actions.onDismissCamouflageVerification,
            onSuccess = actions.onDismissCamouflageVerification,
        )
    }
    if (state.showTimeoutDialog) {
        AppLockTimeoutDialog(
            currentTimeout = state.currentTimeout,
            onDismiss = actions.onDismissTimeoutDialog,
            onSelect = actions.onSelectTimeout,
        )
    }
}

@Preview
@Composable
private fun AppLockDialogsHostPreview() {
    AppLockDialogsHost(
        state = AppLockDialogsUiState(
            dialogMode = null,
            duressDialogMode = null,
            showCamouflageVerificationDialog = false,
            showTimeoutDialog = false,
            currentTimeout = AppLockTimeout.IMMEDIATELY,
        ),
        actions = AppLockDialogsActions(
            onDismissDialogMode = {},
            onPasswordDialogComplete = {},
            onDismissDuressDialogMode = {},
            onDuressDialogComplete = {},
            onDismissCamouflageVerification = {},
            onSelectTimeout = {},
            onDismissTimeoutDialog = {},
            onDismissAllDialogs = {},
        ),
    )
}
