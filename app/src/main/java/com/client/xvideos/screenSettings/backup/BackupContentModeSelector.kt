package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.client.xvideos.R
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.screenSettings.components.SettingsValueRow

@Composable
internal fun BackupContentModeSelector(
    title: String,
    value: XlrBackupContentMode,
    enabled: Boolean,
    description: String,
    onValueChange: (XlrBackupContentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = title,
            value = "${backupContentModeTitle(value)} • $description"
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.padding(bottom = 10.dp).then(BACKUP_SELECTOR_BASE_MODIFIER)
        ) {
            XlrBackupContentMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    enabled = enabled,
                    selected = value == mode,
                    onClick = { onValueChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = XlrBackupContentMode.entries.size
                    ),
                    label = { Text(backupContentModeTitle(mode)) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun BackupContentModeSelectorPreview() {
    var mode by remember { mutableStateOf(XlrBackupContentMode.MINI) }
    BackupContentModeSelector(
        title = "Режим L",
        value = mode,
        enabled = true,
        description = "Только метаданные",
        onValueChange = { mode = it }
    )
}
