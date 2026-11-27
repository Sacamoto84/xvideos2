package com.client.xvideos.screenSettings.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar

@Composable
internal fun CamouflageGroup(
    passwordSet: Boolean,
    onEnableRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
    val camouflageSubtitle = remember(passwordSet, isCamouflage) {
        when {
            !passwordSet -> "Сначала задайте код доступа"
            isCamouflage -> "Иконка «Калькулятор», секретный вход по PIN + «=»"
            else -> "Выключена (стандартная иконка приложения)"
        }
    }
    val onToggleCamouflage: (Boolean) -> Unit = remember(passwordSet, onEnableRequested, context) {
        { enable ->
            if (passwordSet) {
                if (enable) {
                    onEnableRequested()
                } else {
                    Settings.camouflage_calculator_enabled.setValue(false)
                    LauncherAliasManager.setCalculatorAliasEnabled(context, false)
                    SnackBar.info("Маскировка под калькулятор отключена")
                }
            }
        }
    }

    SettingsSwitchRow(
        modifier = modifier,
        icon = R.drawable.ic_launcher_calculator,
        text = "Маскировка под калькулятор",
        subtitle = camouflageSubtitle,
        value = isCamouflage && passwordSet,
        enabled = passwordSet,
        onValueChange = onToggleCamouflage
    )
}

@Preview
@Composable
private fun CamouflageGroupPreview() {
    CamouflageGroup(passwordSet = true, onEnableRequested = {})
}
