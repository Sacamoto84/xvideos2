package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.common.theme.Theme

@Composable
internal fun AppLockTimeoutItem(
    timeout: AppLockTimeout,
    isSelected: Boolean,
    radioColors: RadioButtonColors,
    onSelect: (AppLockTimeout) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick = remember(timeout, onSelect) { { onSelect(timeout) } }
    val label = remember(timeout) {
        when (timeout) {
            AppLockTimeout.IMMEDIATELY -> "Сразу при выходе"
            AppLockTimeout.NEVER -> "Никогда (только при перезапуске)"
            else -> timeout.displayName
        }
    }
    val itemBaseModifier = modifier.then(TIMEOUT_ITEM_FULL_MODIFIER)
    val itemTextStyle = if (isSelected) {
        Theme.L.Type.dialogBody.copy(
            color = SettingsAccentColor,
            fontWeight = FontWeight.SemiBold
        )
    } else {
        Theme.L.Type.dialogBody.copy(
            color = Theme.DialogLavande.bodyColor,
            fontWeight = FontWeight.Normal
        )
    }

    Row(
        modifier = itemBaseModifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = radioColors
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = itemTextStyle)
    }
}

@Preview
@Composable
private fun AppLockTimeoutItemPreview() {
    AppLockTimeoutItem(
        timeout = AppLockTimeout.MINUTES_1,
        isSelected = true,
        radioColors = RadioButtonDefaults.colors(),
        onSelect = {},
    )
}
