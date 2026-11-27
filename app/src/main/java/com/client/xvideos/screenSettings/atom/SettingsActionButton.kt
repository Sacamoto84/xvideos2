package com.client.xvideos.screenSettings.atom

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun SettingsActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun SettingsActionButtonPreview() {
    SettingsActionButton(text = "Обновить", enabled = true, onClick = {})
}
