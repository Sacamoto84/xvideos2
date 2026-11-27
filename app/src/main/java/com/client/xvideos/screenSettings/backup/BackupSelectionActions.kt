package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.screenSettings.components.SettingsAccentColor

@Composable
internal fun BackupSelectionActions(
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 72.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 8.dp
                )
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            enabled = enabled,
            onClick = onSelectAll
        ) {
            Text("Все разделы")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            enabled = enabled,
            onClick = onSelectNone,
            colors = ButtonDefaults.textButtonColors(contentColor = SettingsAccentColor)
        ) {
            Text("Снять")
        }
    }
}

@Preview
@Composable
private fun BackupSelectionActionsPreview() {
    BackupSelectionActions(enabled = true, onSelectAll = {}, onSelectNone = {})
}
