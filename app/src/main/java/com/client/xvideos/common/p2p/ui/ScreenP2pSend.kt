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
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.P2pReceiveManager
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.P2pShareController
import com.client.xvideos.common.p2p.ShareState
import com.client.xvideos.common.p2p.export.LCollectionExporter
import com.client.xvideos.common.p2p.export.RCollectionExporter
import com.client.xvideos.common.p2p.export.LExporter
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import com.client.xvideos.l.featured.saved.LDownloadProgress
import com.client.xvideos.l.featured.saved.lPersistPicsDetailsToFolder
import com.client.xvideos.l.net.Luscious
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File

private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

/** Доступ к Hilt-синглтонам из Voyager-экрана — объекта вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface P2pSendEntryPoint {
    fun luscious(): Luscious
}

/**
 * Ready — бандл уже в store. DownloadL — качаем item в outbox-зеркало
 * `outbox/L/Likes` (структура повторяет /xvideos, поэтому relativePath
 * манифеста совпадает с боевым) и экспортируем оттуда.
 */
private suspend fun prepareBundle(
    context: Context,
    source: P2pSendSource,
    progress: LDownloadProgress,
): P2pExportBundle = when (source) {
    is P2pSendSource.Ready -> source.bundle
    is P2pSendSource.DownloadL -> withContext(Dispatchers.IO) {
        val item = source.item() ?: error("Битые данные item")
        val luscious = EntryPointAccessors
            .fromApplication(context.applicationContext, P2pSendEntryPoint::class.java)
            .luscious()
        val outboxLikes = mirrorRoot(
            base = File(AppPath.p2p_outbox),
            mainRoot = File(AppPath.main),
            storeRoot = File(AppPath.l_likes),
        )
        val folder = lPersistPicsDetailsToFolder(item, outboxLikes, luscious, progress).getOrThrow()
        LExporter.export(folder) ?: error("Не удалось подготовить файлы")
    }
    is P2pSendSource.ShareCollection -> withContext(Dispatchers.IO) {
        LCollectionExporter.export(
            collectionName = source.collectionName,
            collectionRoot = File(AppPath.l_collection),
            outboxDir = File(AppPath.p2p_outbox),
        ) ?: error("Не удалось подготовить коллекцию")
    }
    is P2pSendSource.ShareCollectionR -> withContext(Dispatchers.IO) {
        RCollectionExporter.export(
            collectionName = source.collectionName,
            collectionRoot = File(AppPath.r_collection),
            outboxDir = File(AppPath.p2p_outbox),
        ) ?: error("Не удалось подготовить коллекцию")
    }
}

/**
 * Экран «Отправка P2P»: подготовка файлов (outbox при необходимости),
 * поиск устройств и передача.
 */
data class ScreenP2pSend(val source: P2pSendSource) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val activity = remember(context) { context.findActivity() }
        
        var hasPermissions by remember { mutableStateOf(P2pPermissions.allGranted(context)) }
        var showPermissionDialog by remember { mutableStateOf(!hasPermissions) }

        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
        val downloadProgress = remember { LDownloadProgress(scope) }
        val controller = remember {
            P2pShareController(
                nearby = NearbyClientImpl(context),
                scope = scope,
                myName = Build.MODEL ?: "Android",
                bundleProvider = { prepareBundle(context.applicationContext, source, downloadProgress) },
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
                        val pct by downloadProgress.percentDownload.collectAsState()
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (pct in 0f..1f) {
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                                Text("Подготовка файлов: ${(pct * 100).toInt()}%", modifier = Modifier.padding(top = 8.dp))
                            } else {
                                CircularProgressIndicator()
                                Text("Подготовка файлов…", modifier = Modifier.padding(top = 16.dp))
                            }
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
                            // Передача подтверждена — outbox-staging больше не нужен.
                            withContext(Dispatchers.IO) { AppPath.clearP2pOutbox() }
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
            LavenderDialog(
                title = "Нужны разрешения",
                onDismiss = { navigator.pop() },
                body = androidx.compose.ui.text.AnnotatedString("Для поиска устройств рядом приложению нужны разрешения на Bluetooth и Wi-Fi."),
                confirmText = "Предоставить",
                onConfirm = {
                    val perms = P2pPermissions.required()
                    Log.d("P2P", "Запрашиваем разрешения: ${perms.joinToString()}")
                    if (activity != null) {
                        activity.requestPermissions(perms, 123)
                    } else {
                        Log.e("P2P", "Activity is NULL, не можем запросить разрешения!")
                    }
                },
            )
        }
    }
}
