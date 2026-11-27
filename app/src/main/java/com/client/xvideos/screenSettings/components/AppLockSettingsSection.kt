package com.client.xvideos.screenSettings.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.model.AppLockDialogsActions
import com.client.xvideos.screenSettings.model.AppLockDialogsUiState
import com.client.xvideos.screenSettings.model.AppLockSettingsGroupActions
import com.client.xvideos.screenSettings.model.AppLockSettingsGroupUiState
import com.client.xvideos.screenSettings.model.DuressDialogMode

@Composable
fun AppLockSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val appLockEnabled by Settings.app_lock_enabled.field.collectAsStateWithLifecycle()
    val timeoutSeconds by Settings.app_lock_timeout_seconds.field.collectAsStateWithLifecycle()
    val keyboardIncognito by Settings.keyboard_incognito_enabled.field.collectAsStateWithLifecycle()
    val blurRecentTasks by Settings.blur_recent_tasks.field.collectAsStateWithLifecycle()
    var passwordSet by remember { mutableStateOf(AppLockRepository.isPasswordSet(context)) }
    var duressCodeSet by remember { mutableStateOf(AppLockRepository.isDuressCodeSet(context)) }
    var dialogMode by remember { mutableStateOf<AppLockDialogMode?>(null) }
    var duressDialogMode by remember { mutableStateOf<DuressDialogMode?>(null) }
    val enabled = appLockEnabled && passwordSet
    var showCamouflageVerificationDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appLockEnabled, passwordSet) {
        syncAppLockPreferenceConsistency(context, appLockEnabled, passwordSet)
    }

    val currentTimeout = remember(timeoutSeconds) { AppLockTimeout.fromSeconds(timeoutSeconds) }
    val dialogActions = remember(context) {
        AppLockDialogsActions(
            onDismissDialogMode = { dialogMode = null },
            onPasswordDialogComplete = {
                passwordSet = AppLockRepository.isPasswordSet(context)
                duressCodeSet = AppLockRepository.isDuressCodeSet(context)
                dialogMode = null
            },
            onDismissDuressDialogMode = { duressDialogMode = null },
            onDuressDialogComplete = {
                duressCodeSet = AppLockRepository.isDuressCodeSet(context)
                duressDialogMode = null
            },
            onDismissCamouflageVerification = { showCamouflageVerificationDialog = false },
            onSelectTimeout = { timeout ->
                Settings.app_lock_timeout_seconds.setValue(timeout.seconds)
                showTimeoutDialog = false
            },
            onDismissTimeoutDialog = { showTimeoutDialog = false },
            onDismissAllDialogs = {
                dialogMode = null
                duressDialogMode = null
                showCamouflageVerificationDialog = false
                showTimeoutDialog = false
            },
        )
    }
    val groupActions = remember(enabled) {
        AppLockSettingsGroupActions(
            onSetOrChangeLock = { dialogMode = if (enabled) AppLockDialogMode.CHANGE else AppLockDialogMode.SET },
            onDisableLock = { dialogMode = AppLockDialogMode.DISABLE },
            onShowTimeoutClick = { showTimeoutDialog = true },
            onSetDuressCode = { duressDialogMode = DuressDialogMode.SET },
            onChangeDuressCode = { duressDialogMode = DuressDialogMode.CHANGE },
            onRemoveDuressCode = { duressDialogMode = DuressDialogMode.REMOVE },
            onEnableCamouflageRequested = { showCamouflageVerificationDialog = true },
            onToggleIncognito = { Settings.keyboard_incognito_enabled.setValue(it) },
            onToggleBlurRecent = { Settings.blur_recent_tasks.setValue(it) },
        )
    }

    AppLockDialogsHost(
        state = AppLockDialogsUiState(
            dialogMode = dialogMode,
            duressDialogMode = duressDialogMode,
            showCamouflageVerificationDialog = showCamouflageVerificationDialog,
            showTimeoutDialog = showTimeoutDialog,
            currentTimeout = currentTimeout,
        ),
        actions = dialogActions,
    )

    AppLockSettingsGroup(
        state = AppLockSettingsGroupUiState(
            enabled = enabled,
            passwordSet = passwordSet,
            duressCodeSet = duressCodeSet,
            currentTimeout = currentTimeout,
            keyboardIncognito = keyboardIncognito,
            blurRecentTasks = blurRecentTasks,
        ),
        actions = groupActions,
        modifier = modifier,
    )
}

private fun syncAppLockPreferenceConsistency(
    context: Context,
    appLockEnabled: Boolean,
    passwordSet: Boolean,
) {
    if (appLockEnabled && !passwordSet) {
        Settings.app_lock_enabled.setValue(false)
    }
    if (!passwordSet && Settings.camouflage_calculator_enabled.field.value) {
        Settings.camouflage_calculator_enabled.setValue(false)
        LauncherAliasManager.setCalculatorAliasEnabled(context, false)
    }
}

@Preview
@Composable
private fun AppLockSettingsSectionPreview() {
    AppLockSettingsSection()
}
