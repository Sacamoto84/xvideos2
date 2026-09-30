package com.client.xvideos.screenSettings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.getFolderSize
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.screenSettings.components.EmptyStorageStats
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.StorageStat
import com.client.xvideos.screenSettings.components.loadStorageStats
import com.client.xvideos.screenSettings.molecule.AppSettingsScreenBody
import com.client.xvideos.screenSettings.molecule.SettingsDetailParams
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object AppSettingsScreen : Screen {

    private fun readResolve(): Any = AppSettingsScreen

    override val key: ScreenKey = "AppSettingsScreen"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current.applicationContext
        val scope = rememberCoroutineScope()
        val vm: AppSettingsSM = getScreenModel()

        var imageCacheSizeBytes by remember { mutableLongStateOf(0L) }
        var storageStats by remember { mutableStateOf(EmptyStorageStats) }
        var sizeRedTotal by remember { mutableLongStateOf(0L) }
        var sizeRedDownload by remember { mutableLongStateOf(0L) }

        suspend fun refreshImageCacheSize() {
            imageCacheSizeBytes = withContext(Dispatchers.IO) {
                CoilImageLoaderFactory.imageDiskCacheSizeBytes(context)
            }
        }

        suspend fun refreshStorageStats() {
            storageStats = withContext(Dispatchers.IO) { loadStorageStats() }
        }

        suspend fun refreshRedSizes() {
            sizeRedTotal = withContext(Dispatchers.IO) { getFolderSize(File(AppPath.main, "R")) }
            sizeRedDownload = withContext(Dispatchers.IO) { getFolderSize(File(AppPath.r_cache_download)) }
        }

        LaunchedEffect(Unit) {
            refreshImageCacheSize()
            refreshStorageStats()
            refreshRedSizes()
        }

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }

        val refreshFileStats: () -> Unit = remember(scope) {
            {
                scope.launch {
                    refreshStorageStats()
                    refreshRedSizes()
                }
            }
        }

        val onClearImageCache: () -> Unit = remember(scope, context) {
            {
                scope.launch {
                    withContext(Dispatchers.IO) { CoilImageLoaderFactory.clearCache(context) }
                    refreshImageCacheSize()
                    SnackBar.success("Кэш картинок очищен")
                }
            }
        }

        val onClearDownload: () -> Unit = remember(vm, scope) {
            {
                vm.downloadRed.deleteAll {
                    scope.launch {
                        refreshRedSizes()
                        SnackBar.success("Папка Download очищена")
                    }
                }
            }
        }

        val data = remember(vm) {
            SettingsDataHolders(
                savedRed = vm.savedRed,
                blockRed = vm.blockRed,
                downloadRed = vm.downloadRed,
                savedL = vm.savedL
            )
        }

        AppSettingsScreenContent(
            onBack = onBack,
            imageCacheSizeBytes = imageCacheSizeBytes,
            storageStats = storageStats,
            sizeRedTotal = sizeRedTotal,
            sizeRedDownload = sizeRedDownload,
            onClearImageCache = onClearImageCache,
            onClearDownload = onClearDownload,
            data = data,
            context = context,
            onBackupDataChanged = refreshFileStats,
            onRefreshFileStats = refreshFileStats
        )
    }
}

@Composable
internal fun AppSettingsScreenContent(
    onBack: () -> Unit,
    imageCacheSizeBytes: Long,
    storageStats: List<StorageStat>,
    sizeRedTotal: Long,
    sizeRedDownload: Long,
    onClearImageCache: () -> Unit,
    onClearDownload: () -> Unit,
    data: SettingsDataHolders,
    context: Context,
    onBackupDataChanged: () -> Unit,
    onRefreshFileStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    val scrollState = rememberScrollState()

    LaunchedEffect(currentPage) {
        scrollState.scrollTo(0)
    }

    val onMainBack: () -> Unit = remember { { currentPage = SettingsPage.Main } }
    BackHandler(enabled = currentPage != SettingsPage.Main, onBack = onMainBack)
    BackHandler(enabled = currentPage == SettingsPage.Main, onBack = onBack)

    LaunchedEffect(currentPage) {
        if (currentPage == SettingsPage.Storage) {
            onRefreshFileStats()
        }
    }

    val onOpenPage: (SettingsPage) -> Unit = remember { { page -> currentPage = page } }
    val topCutout = getTopInsetDp()

    val detailParams = remember(
        currentPage,
        imageCacheSizeBytes,
        storageStats,
        sizeRedTotal,
        sizeRedDownload,
        onClearImageCache,
        onClearDownload,
        data,
        context,
        onBackupDataChanged
    ) {
        SettingsDetailParams(
            currentPage = currentPage,
            imageCacheSizeBytes = imageCacheSizeBytes,
            storageStats = storageStats,
            sizeRedTotal = sizeRedTotal,
            sizeRedDownload = sizeRedDownload,
            onClearImageCache = onClearImageCache,
            onClearDownload = onClearDownload,
            data = data,
            context = context,
            onBackupDataChanged = onBackupDataChanged
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = SettingsScreenBackground
    ) { paddingValues ->
        AppSettingsScreenBody(
            params = detailParams,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .verticalScroll(scrollState),
            topCutout = topCutout,
            onOpenPage = onOpenPage
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF1B1B1F,
    device = "spec:width=1080px,height=23400px,dpi=440"
)
@Composable
private fun AppSettingsScreenPreview() {
    val context = LocalContext.current
    Settings.init(context.getSharedPreferences("preview_prefs", 0))
    XvideosTheme {
        AppSettingsScreenBody(
            params = SettingsDetailParams(
                currentPage = SettingsPage.Main,
                imageCacheSizeBytes = 128_000_000L,
                storageStats = EmptyStorageStats,
                sizeRedTotal = 512_000_000L,
                sizeRedDownload = 64_000_000L,
                onClearImageCache = {},
                onClearDownload = {},
                data = SettingsDataHolders(),
                context = context.applicationContext,
                onBackupDataChanged = {}
            ),
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}
