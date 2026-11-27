package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.common.theme.LavenderDialog

@Composable
internal fun AppLockTimeoutDialog(
    currentTimeout: AppLockTimeout,
    onDismiss: () -> Unit,
    onSelect: (AppLockTimeout) -> Unit
) {
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = SettingsAccentColor,
        unselectedColor = Color(0xFF938F99)
    )

    LavenderDialog(
        title = "Автоблокировка",
        onDismiss = onDismiss,
        dismissText = "Отмена",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppLockTimeout.entries.forEach { timeout ->
                    key(timeout) {
                        AppLockTimeoutItem(
                            timeout = timeout,
                            isSelected = (timeout == currentTimeout),
                            radioColors = radioColors,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun AppLockTimeoutDialogPreview() {
    AppLockTimeoutDialog(
        currentTimeout = AppLockTimeout.MINUTES_1,
        onDismiss = {},
        onSelect = {}
    )
}
