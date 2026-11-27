package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsScreenBackground

@Composable
internal fun BackupModeSelector(
    selected: BackupFlowScreen,
    enabled: Boolean,
    onSelected: (BackupFlowScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = SettingsAccentColor,
        activeContentColor = SettingsScreenBackground,
        activeBorderColor = SettingsAccentColor,
        inactiveContainerColor = SettingsCardColor,
        inactiveContentColor = Theme.L.textColor,
        inactiveBorderColor = SettingsDividerColor
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.then(BACKUP_SELECTOR_BASE_MODIFIER)
    ) {
        BackupFlowScreen.entries.forEachIndexed { index, item ->
            SegmentedButton(
                enabled = enabled,
                selected = selected == item,
                onClick = { onSelected(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = BackupFlowScreen.entries.size,
                    baseShape = RoundedCornerShape(8.dp)
                ),
                colors = segmentedColors,
                label = {
                    Text(
                        text = item.title,
                        color = if (selected == item) SettingsScreenBackground else SettingsRowTextPrimary,
                        style = Theme.L.Type.button
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun BackupModeSelectorPreview() {
    var mode by remember { mutableStateOf(BackupFlowScreen.CREATE) }
    BackupModeSelector(selected = mode, enabled = true, onSelected = { mode = it })
}
