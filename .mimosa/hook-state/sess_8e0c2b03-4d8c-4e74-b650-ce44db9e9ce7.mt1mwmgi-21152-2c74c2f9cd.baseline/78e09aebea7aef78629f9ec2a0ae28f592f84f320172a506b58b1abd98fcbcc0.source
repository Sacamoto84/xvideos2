package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import com.client.xvideos.common.p2p.toggleP2pService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import com.client.xvideos.common.snackbar.SnackBar
import kotlinx.coroutines.launch

@Composable
internal fun P2PSettingsSection() {
    val context = LocalContext.current
    val bgReceive = Settings.p2p_background_receive.field.collectAsStateWithLifecycle().value
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            toggleP2pService(context, true)
        } else {
            Settings.p2p_background_receive.setValue(false)
            SnackBar.error("Нужны разрешения для работы P2P в фоне")
        }
    }

    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.icon_red,
            text = "Приём в фоне",
            subtitle = if (bgReceive) "Включён" else "Выключен",
            value = bgReceive,
            onValueChange = { enabled ->
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
                SnackBar.success(if (enabled) "Приём в фоне включен" else "Приём в фоне выключен")
            }
        )
    }
}
