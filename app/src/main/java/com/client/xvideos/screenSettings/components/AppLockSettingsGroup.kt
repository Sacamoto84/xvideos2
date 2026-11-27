package com.client.xvideos.screenSettings.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.screenSettings.atom.SettingsOutlinedButton
import com.client.xvideos.screenSettings.model.AppLockSettingsGroupActions
import com.client.xvideos.screenSettings.model.AppLockSettingsGroupUiState
import com.client.xvideos.screenSettings.molecule.DuressCodeRow

/** Группа настроек безопасности, блокировки при запуске, маскировки и приватности ввода. */
@Composable
internal fun AppLockSettingsGroup(
    state: AppLockSettingsGroupUiState,
    actions: AppLockSettingsGroupActions,
    modifier: Modifier = Modifier,
) {
    val setOrChangeTrailing: @Composable () -> Unit = remember(state.enabled, actions.onSetOrChangeLock) {
        { Button(onClick = actions.onSetOrChangeLock) { Text(if (state.enabled) "Изменить" else "Задать") } }
    }
    val disableTrailing: @Composable () -> Unit = remember(actions.onDisableLock) {
        { SettingsOutlinedButton(text = "Отключить", onClick = actions.onDisableLock, color = SettingsDestructiveColor) }
    }
    val lockSubtitle = remember(state.enabled) { if (state.enabled) "Включена" else "Выключена" }
    val timeoutSubtitle = remember(state.currentTimeout) { appLockTimeoutSubtitle(state.currentTimeout) }
    val keyboardSubtitle = remember(state.keyboardIncognito) {
        if (state.keyboardIncognito) "Клавиатура не сохраняет поисковые запросы" else "Стандартный режим ввода"
    }
    val blurSubtitle = remember(state.blurRecentTasks) {
        if (state.blurRecentTasks) {
            "Превью скрыто/размыто в карусели недавних задач"
        } else {
            "Отображается обычный снимок экрана"
        }
    }

    SettingsGroup(modifier = modifier) {
        SettingsListItem(
            icon = R.drawable.key_24,
            text = "Блокировка при запуске",
            subtitle = lockSubtitle,
            trailing = setOrChangeTrailing
        )
        if (state.enabled) {
            SettingsDivider2()
            SettingsListItem(
                icon = R.drawable.key_24,
                text = "Автоблокировка",
                subtitle = timeoutSubtitle,
                onClick = actions.onShowTimeoutClick
            )
            SettingsDivider2()
            SettingsListItem(
                icon = R.drawable.key_24,
                text = "Код доступа",
                subtitle = "Отключить блокировку приложения",
                trailing = disableTrailing
            )
            SettingsDivider2()
            DuressCodeRow(
                isSet = state.duressCodeSet,
                onSetClick = actions.onSetDuressCode,
                onChangeClick = actions.onChangeDuressCode,
                onRemoveClick = actions.onRemoveDuressCode,
            )
        }
        SettingsDivider2()
        CamouflageGroup(passwordSet = state.passwordSet, onEnableRequested = actions.onEnableCamouflageRequested)
        SettingsDivider2()
        SettingsSwitchRow(
            icon = R.drawable.memory_24,
            text = "Инкогнито-клавиатура",
            subtitle = keyboardSubtitle,
            value = state.keyboardIncognito,
            onValueChange = actions.onToggleIncognito
        )
        SettingsDivider2()
        SettingsSwitchRow(
            icon = R.drawable.ic_blur_24,
            text = "Защита в диспетчере задач",
            subtitle = blurSubtitle,
            value = state.blurRecentTasks,
            onValueChange = actions.onToggleBlurRecent
        )
    }
}

private fun appLockTimeoutSubtitle(timeout: AppLockTimeout): String = when (timeout) {
    AppLockTimeout.IMMEDIATELY -> "Сразу при выходе"
    AppLockTimeout.NEVER -> "Выключена (только при перезапуске)"
    else -> "Через ${timeout.displayName.lowercase()} в фоне"
}

@Preview
@Composable
private fun AppLockSettingsGroupPreview() {
    AppLockSettingsGroup(
        state = AppLockSettingsGroupUiState(
            enabled = true,
            passwordSet = true,
            duressCodeSet = false,
            currentTimeout = AppLockTimeout.SECONDS_30,
            keyboardIncognito = false,
            blurRecentTasks = true,
        ),
        actions = AppLockSettingsGroupActions(
            onSetOrChangeLock = {},
            onDisableLock = {},
            onShowTimeoutClick = {},
            onSetDuressCode = {},
            onChangeDuressCode = {},
            onRemoveDuressCode = {},
            onEnableCamouflageRequested = {},
            onToggleIncognito = {},
            onToggleBlurRecent = {},
        ),
    )
}
