package com.client.xvideos.common.p2p.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pShareController
import com.client.xvideos.common.p2p.ShareState
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Bottom sheet «Поиск телефонов рядом» для отправки [bundle].
 * Требует уже выданных разрешений (проверяет вызывающий) — здесь только дискавери и отправка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2pDeviceSearchSheet(
    bundle: P2pExportBundle,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val controller = remember {
        P2pShareController(
            nearby = NearbyClientImpl(context),
            scope = scope,
            myName = Build.MODEL ?: "Android",
            bundle = bundle,
        )
    }

    LaunchedEffect(Unit) { controller.start() }
    DisposableEffect(Unit) { onDispose { controller.stop() } }

    val state by controller.state.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val s = state) {
                is ShareState.Idle,
                is ShareState.Searching -> {
                    Text("Телефоны рядом", style = MaterialTheme.typography.titleMedium)
                    val list = (s as? ShareState.Searching)?.endpoints.orEmpty()
                    if (list.isEmpty()) {
                        Text("Поиск…")
                        CircularProgressIndicator()
                    } else {
                        LazyColumn {
                            items(list, key = { it.id }) { ep ->
                                Text(
                                    ep.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { controller.connectTo(ep.id) }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
                is ShareState.Connecting -> Text("Соединение… код: ${s.authDigits ?: "…"}")
                is ShareState.Sending -> {
                    val pct = if (s.total > 0) (s.transferred * 100 / s.total) else 0
                    Text("Отправка… $pct%")
                    CircularProgressIndicator()
                }
                is ShareState.Done -> Text("Готово ✓")
                is ShareState.Error -> Text("Ошибка: ${s.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
