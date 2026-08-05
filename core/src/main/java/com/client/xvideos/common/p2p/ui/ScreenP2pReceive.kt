package com.client.xvideos.common.p2p.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.P2pReceiveManager
import com.client.xvideos.common.p2p.ReceiveState
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

/** Экран «Приём P2P»: рекламируется и принимает один item. */
class ScreenP2pReceive : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        
        val activeController by P2pReceiveManager.controller.collectAsState()

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) P2pReceiveManager.start(context)
        }

        LaunchedEffect(Unit) {
            if (P2pPermissions.allGranted(context)) P2pReceiveManager.start(context)
            else permissionLauncher.launch(P2pPermissions.required())
        }

        val state by if (activeController != null) {
            activeController!!.state.collectAsState()
        } else {
            remember { mutableStateOf(ReceiveState.Idle) }
        }

        ScreenP2pReceiveContent(
            state = state,
            onPop = { navigator.pop() },
        )
    }
}

@Composable
private fun ScreenP2pReceiveContent(
    state: ReceiveState,
    onPop: () -> Unit,
) {

    Scaffold(modifier = Modifier.background(Theme.background)) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val s = state) {
                is ReceiveState.Idle,
                is ReceiveState.Advertising -> {
                    CircularProgressIndicator()
                    Text("Ожидание отправителя…", modifier = Modifier.padding(top = 16.dp))
                }
                is ReceiveState.Connecting -> {
                    CircularProgressIndicator()
                    Text("Подключение к: ${s.endpointName}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                }
                is ReceiveState.Receiving -> {
                    CircularProgressIndicator()
                    val pct = if (s.total > 0) (s.transferred * 100 / s.total) else 0
                    Text("Приём… $pct%", modifier = Modifier.padding(top = 16.dp))
                }
                is ReceiveState.Done -> {
                    Text("Принято ✓", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { onPop() }, modifier = Modifier.padding(top = 16.dp)) { Text("Готово") }
                }
                is ReceiveState.Error -> {
                    Text("Ошибка: ${s.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { onPop() }, modifier = Modifier.padding(top = 16.dp)) { Text("Закрыть") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewScreenP2pReceiveConnecting() {
    XvideosTheme {
        ScreenP2pReceiveContent(
            state = ReceiveState.Connecting("Pixel 6"),
            onPop = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewScreenP2pReceiveReceiving() {
    XvideosTheme {
        ScreenP2pReceiveContent(
            state = ReceiveState.Receiving(transferred = 45, total = 100),
            onPop = {}
        )
    }
}
