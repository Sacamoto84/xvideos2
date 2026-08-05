package com.client.xvideos.common.p2p.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Маленький диалог: выбрать способ «Поделиться» — системный chooser или P2P рядом.
 */
@Composable
fun P2pSendChooserDialog(
    onSystem: () -> Unit,
    onP2p: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = "Поделиться",
        onDismiss = onDismiss,
        content = {
            TextButton(onClick = { onDismiss(); onSystem() }, modifier = Modifier.fillMaxWidth()) {
                Text("Системное (через приложения)", color = Theme.DialogLavande.dismissTextColor)
            }
            TextButton(onClick = { onDismiss(); onP2p() }, modifier = Modifier.fillMaxWidth()) {
                Text("P2P рядом (Nearby)", color = Theme.DialogLavande.dismissTextColor)
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewP2pSendChooserDialog() {
    XvideosTheme {
        P2pSendChooserDialog(
            onSystem = {},
            onP2p = {},
            onDismiss = {}
        )
    }
}
