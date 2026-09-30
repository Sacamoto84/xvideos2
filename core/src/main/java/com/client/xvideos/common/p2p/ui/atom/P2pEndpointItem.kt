package com.client.xvideos.common.p2p.ui.atom

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.p2p.P2pEndpoint

@Composable
fun P2pEndpointItem(
    endpoint: P2pEndpoint,
    onConnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleConnect = remember(endpoint.id, onConnect) { { onConnect(endpoint.id) } }
    ListItem(
        headlineContent = { Text(endpoint.name) },
        supportingContent = { Text("Нажмите, чтобы подключиться") },
        modifier = modifier.clickable(onClick = handleConnect)
    )
}

@Preview
@Composable
private fun P2pEndpointItemPreview() {
    P2pEndpointItem(
        endpoint = P2pEndpoint(id = "1", name = "Pixel 7 Pro"),
        onConnect = {}
    )
}
