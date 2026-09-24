package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import com.client.xvideos.common.p2p.toggleP2pService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import com.client.xvideos.common.snackbar.SnackBar

private const val TEXT_BACKGROUND_RECEIVE = "Приём в фоне"
private const val TEXT_ENABLED = "Включён"
private const val TEXT_DISABLED = "Выключен"
private const val MSG_PERMISSIONS_REQUIRED = "Нужны разрешения для работы P2P в фоне"
private const val MSG_BG_RECEIVE_ENABLED = "Приём в фоне включен"
private const val MSG_BG_RECEIVE_DISABLED = "Приём в фоне выключен"

@Composable
internal fun P2PSettingsSection() {
    val context = LocalContext.current
    val bgReceive by Settings.p2p_background_receive.field.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            toggleP2pService(context, true)
        } else {
            Settings.p2p_background_receive.setValue(false)
            SnackBar.error(MSG_PERMISSIONS_REQUIRED)
        }
    }

    val onBgReceiveChanged: (Boolean) -> Unit = remember(context, permissionLauncher) {
        { enabled ->
            Settings.p2p_background_receive.setValue(enabled)
            if (enabled) {
                if (P2pPermissions.allGranted(context)) {
                    toggleP2pService(context, true)
                } else {
                    permissionLauncher.launch(P2pPermissions.required())
                }
            } else {
                toggleP2pService(context, false)
            }
            SnackBar.success(if (enabled) MSG_BG_RECEIVE_ENABLED else MSG_BG_RECEIVE_DISABLED)
        }
    }

    val bgReceiveSubtitle = remember(bgReceive) {
        if (bgReceive) TEXT_ENABLED else TEXT_DISABLED
    }

    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.icon_red,
            text = TEXT_BACKGROUND_RECEIVE,
            subtitle = bgReceiveSubtitle,
            value = bgReceive,
            onValueChange = onBgReceiveChanged
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun P2PSettingsSectionPreview() = SettingsPreview {
    P2PSettingsSection()
}
