package com.client.xvideos.common.p2p.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Маленький диалог: выбрать способ «Поделиться» — системный chooser или P2P рядом.
 */
@Composable
fun P2pSendChooserDialog(
    onSystem: () -> Unit,
    onP2p: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поделиться") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onDismiss(); onSystem() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Системное (через приложения)")
                }
                TextButton(onClick = { onDismiss(); onP2p() }, modifier = Modifier.fillMaxWidth()) {
                    Text("P2P рядом (Nearby)")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
