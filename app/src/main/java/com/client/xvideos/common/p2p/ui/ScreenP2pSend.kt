package com.client.xvideos.common.p2p.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.P2pReceiveManager
import com.client.xvideos.common.p2p.P2pShareController
import com.client.xvideos.common.p2p.ShareState
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Экран «Отправка P2P»: поиск устройств и передача [bundle].
 */
data class ScreenP2pSend(val bundle: P2pExportBundle) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val activity = remember(context) { context.findActivity() }
        
        var hasPermissions by remember { mutableStateOf(P2pPermissions.allGranted(context)) }
        var showPermissionDialog by remember { mutableStateOf(!hasPermissions) }

        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
        val controller = remember {
            P2pShareController(
                nearby = NearbyClientImpl(context),
                scope = scope,
                myName = Build.MODEL ?: "Android",
                bundle = bundle,
            )
        }

        // Запуск поиска при наличии прав
        LaunchedEffect(hasPermissions) {
            if (hasPermissions) {
                controller.start()
            }
        }

        // Следим за жизненным циклом для обновления прав
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                hasPermissions = P2pPermissions.allGranted(context)
                if (hasPermissions) showPermissionDialog = false
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                controller.stop()
                // stopAll() гасит рекламу всего процесса — оживляем фоновый приём, если он был запущен.
                P2pReceiveManager.ensureAdvertising()
            }
        }

        val state by controller.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Отправка файлов") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (val s = state) {
                    is ShareState.Preparing -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Подготовка файлов…", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    is ShareState.Idle,
                    is ShareState.Searching -> {
                        Text("Поиск устройств рядом…", style = MaterialTheme.typography.titleMedium)
                        
                        val list = (s as? ShareState.Searching)?.endpoints.orEmpty()
                        if (list.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(list, key = { it.id }) { ep ->
                                    ListItem(
                                        headlineContent = { Text(ep.name) },
                                        supportingContent = { Text("Нажмите, чтобы подключиться") },
                                        modifier = Modifier.clickable { controller.connectTo(ep.id) }
                                    )
                                }
                            }
                        }
                    }
                    is ShareState.Connecting -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Подключение…", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    is ShareState.Sending -> {
                        val pct = if (s.total > 0) (s.transferred * 100 / s.total) else 0
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                            Text("Отправка: $pct%", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    is ShareState.Done -> {
                        // Показываем «Готово» секунду и закрываем экран сами.
                        // Уход с экрана отменяет эффект — двойного pop не будет.
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1_000)
                            navigator.pop()
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Готово ✓", style = MaterialTheme.typography.headlineMedium)
                            Button(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Вернуться")
                            }
                        }
                    }
                    is ShareState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Ошибка", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall)
                            Text(s.message, textAlign = TextAlign.Center)
                            Button(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Закрыть")
                            }
                        }
                    }
                }
            }
        }

        // Диалог запроса разрешений
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { navigator.pop() },
                title = { Text("Нужны разрешения") },
                text = { Text("Для поиска устройств рядом приложению нужны разрешения на Bluetooth и Wi-Fi.") },
                confirmButton = {
                    Button(onClick = {
                        val perms = P2pPermissions.required()
                        Log.d("P2P", "Запрашиваем разрешения: ${perms.joinToString()}")
                        if (activity != null) {
                            activity.requestPermissions(perms, 123)
                        } else {
                            Log.e("P2P", "Activity is NULL, не можем запросить разрешения!")
                        }
                    }) {
                        Text("Предоставить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { navigator.pop() }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}
