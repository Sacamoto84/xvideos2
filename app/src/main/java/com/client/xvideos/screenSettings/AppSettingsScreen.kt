package com.client.xvideos.screenSettings

import com.client.xvideos.R
import com.client.xvideos.screenSettings.section.CacheSettingsSection
import com.client.xvideos.screenSettings.section.DisplaySettingsSection
import com.client.xvideos.screenSettings.section.LSettingsSection
import com.client.xvideos.screenSettings.section.NetworkSettingsSection
import com.client.xvideos.screenSettings.section.P2PSettingsSection
import com.client.xvideos.screenSettings.section.WebServerSettingsSection
import com.client.xvideos.screenSettings.section.RSettingsSection
import com.client.xvideos.screenSettings.section.XSettingsSection
import com.client.xvideos.screenSettings.backup.BackupSettingsSection
import com.client.xvideos.common.theme.Theme

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.AppLockSettingsSection
import com.client.xvideos.screenSettings.components.EmptyStorageStats
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsTopBarColor
import com.client.xvideos.screenSettings.components.StorageStatisticsSection
import com.client.xvideos.screenSettings.components.StorageStat
import com.client.xvideos.screenSettings.components.loadStorageStats
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.getFolderSize
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.screenSettings.components.SettingsDivider2
import com.client.xvideos.ui.theme.XvideosTheme
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@Stable
class AppSettingsSM @Inject constructor(
    val savedRed: SavedRed,
    val blockRed: BlockRed,
    val downloadRed: DownloadRed,
    val savedL: SavedL
) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSettingsModule {
    @Binds
    @IntoMap
    @ScreenModelKey(AppSettingsSM::class)
    abstract fun bindAppSettingsSM(sm: AppSettingsSM): ScreenModel
}

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

        val refreshFileStats: () -> Unit = {
            scope.launch {
                refreshStorageStats()
                refreshRedSizes()
            }
        }

        AppSettingsScreenContent(
            onBack = { navigator.pop() },
            imageCacheSizeBytes = imageCacheSizeBytes,
            storageStats = storageStats,
            sizeRedTotal = sizeRedTotal,
            sizeRedDownload = sizeRedDownload,
            onClearImageCache = {
                scope.launch {
                    withContext(Dispatchers.IO) { CoilImageLoaderFactory.clearCache(context) }
                    refreshImageCacheSize()
                    SnackBar.success("Кэш картинок очищен")
                }
            },
            onClearDownload = {
                vm.downloadRed.deleteAll {
                    scope.launch {
                        refreshRedSizes()
                        SnackBar.success("Папка Download очищена")
                    }
                }
            },
            data = SettingsDataHolders(
                savedRed = vm.savedRed,
                blockRed = vm.blockRed,
                downloadRed = vm.downloadRed,
                savedL = vm.savedL
            ),
            context = context,
            onBackupDataChanged = refreshFileStats,
            onRefreshFileStats = refreshFileStats
        )
    }
}

@Composable
private fun AppSettingsScreenContent(
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
    onRefreshFileStats: () -> Unit
) {
    var currentPage by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    val scrollState = rememberScrollState()

    LaunchedEffect(currentPage) {
        scrollState.scrollTo(0)
    }

    BackHandler(enabled = currentPage != SettingsPage.Main) {
        currentPage = SettingsPage.Main
    }
    BackHandler(enabled = currentPage == SettingsPage.Main) {
        onBack()
    }

    val handleBack: () -> Unit = {
        if (currentPage != SettingsPage.Main) {
            currentPage = SettingsPage.Main
        } else {
            onBack()
        }
    }

    LaunchedEffect(currentPage) {
        if (currentPage == SettingsPage.Storage) {
            onRefreshFileStats()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .displayCutoutPadding()
                    .height(64.dp)
                    .fillMaxWidth()
                    .background(SettingsTopBarColor)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = SettingsRowTextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    currentPage.title,
                    modifier = Modifier.weight(1f),
                    color = SettingsRowTextPrimary,
                    style = Theme.L.Type.screenTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = SettingsRowTextPrimary,
                        textAlign = TextAlign.Start
                    )
                )
            }
        },
        containerColor = SettingsScreenBackground
    ) { paddingValues ->
        AppSettingsScreenBody(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState),
            currentPage = currentPage,
            onOpenPage = { currentPage = it },
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
}

@Composable
private fun AppSettingsScreenBody(
    modifier: Modifier = Modifier,
    currentPage: SettingsPage = SettingsPage.Main,
    onOpenPage: (SettingsPage) -> Unit = {},
    imageCacheSizeBytes: Long,
    storageStats: List<StorageStat>,
    sizeRedTotal: Long,
    sizeRedDownload: Long,
    onClearImageCache: () -> Unit,
    onClearDownload: () -> Unit,
    data: SettingsDataHolders,
    context: Context,
    onBackupDataChanged: () -> Unit
) {
    Column(
        modifier = modifier
            .background(SettingsScreenBackground)
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        if (currentPage == SettingsPage.Main) {
            SettingsSectionTitle("Основное")

            SettingsGroup {
                SettingsPage.primaryPages.forEachIndexed { index, page ->
                    if (index > 0) { SettingsDivider2() }
                    SettingsNavigationRow(
                        page = page,
                        onClick = { onOpenPage(page) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingsSectionTitle("Разделы")
            SettingsGroup {
                SettingsPage.contentPages.forEachIndexed { index, page ->
                    if (index > 0) { SettingsDivider2() }
                    SettingsNavigationRow(
                        page = page,
                        onClick = { onOpenPage(page) }
                    )
                }
            }
        } else {
            SettingsDetailPage(
                params = SettingsDetailParams(
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
            )
        }
    }
}

@Immutable
private data class SettingsDetailParams(
    val currentPage: SettingsPage,
    val imageCacheSizeBytes: Long,
    val storageStats: List<StorageStat>,
    val sizeRedTotal: Long,
    val sizeRedDownload: Long,
    val onClearImageCache: () -> Unit,
    val onClearDownload: () -> Unit,
    val data: SettingsDataHolders,
    val context: Context,
    val onBackupDataChanged: () -> Unit
)

@Composable
private fun SettingsDetailPage(params: SettingsDetailParams) {
    val ramCachePercent = Settings.image_cache_ram_percent.field.collectAsStateWithLifecycle().value
    val diskCacheEnabled = Settings.image_cache_disk_enabled.field.collectAsStateWithLifecycle().value
    val diskCacheSizeMb = Settings.image_cache_disk_size_mb.field.collectAsStateWithLifecycle().value
    val lLogin = Settings.l_login.field.collectAsStateWithLifecycle().value

    val isNichesCacheDownloading = params.data.savedRed?.nichesCache?.isDownloading ?: false
    val nichesCacheProgress = params.data.savedRed?.nichesCache?.progress ?: 0f
    val nichesCacheSize = params.data.savedRed?.nichesCache?.list?.size ?: 0
    val nichesCacheLastModifiedHour = params.data.savedRed?.nichesCache?.lastModifiedHour ?: 0L

    Spacer(Modifier.height(4.dp))
    when (params.currentPage) {
        SettingsPage.Main -> Unit
        SettingsPage.Privacy -> AppLockSettingsSection()
        SettingsPage.Display -> DisplaySettingsSection()
        SettingsPage.Network -> NetworkSettingsSection()
        SettingsPage.Cache -> CacheSettingsSection(
            ramCachePercent = ramCachePercent,
            diskCacheEnabled = diskCacheEnabled,
            diskCacheSizeMb = diskCacheSizeMb,
            imageCacheSizeBytes = params.imageCacheSizeBytes,
            onClearImageCache = params.onClearImageCache,
            context = params.context
        )
        SettingsPage.L -> LSettingsSection(lLogin = lLogin)
        SettingsPage.Red -> RSettingsSection(
            sizeRedTotal = params.sizeRedTotal,
            sizeRedDownload = params.sizeRedDownload,
            onClearDownload = params.onClearDownload,
            savedRed = params.data.savedRed,
            downloadRed = params.data.downloadRed,
            isNichesCacheDownloading = isNichesCacheDownloading,
            nichesCacheProgress = nichesCacheProgress,
            nichesCacheSize = nichesCacheSize,
            nichesCacheLastModifiedHour = nichesCacheLastModifiedHour
        )
        SettingsPage.X -> XSettingsSection()
        SettingsPage.Storage -> StorageStatisticsSection(params.storageStats)
        SettingsPage.Backup -> BackupSettingsSection(
            context = params.context,
            data = params.data,
            onDataChanged = params.onBackupDataChanged
        )
        SettingsPage.P2P -> P2PSettingsSection()
        SettingsPage.WebServer -> WebServerSettingsSection()
    }
}

internal enum class SettingsPage(
    val title: String,
    @DrawableRes val icon: Int,
    val subtitle: String
) {
    Main(
        title = "Настройки",
        icon = R.drawable.memory_24,
        subtitle = ""
    ),
    Privacy(
        title = "Приватность",
        icon = R.drawable.key_24,
        subtitle = "Пароль и блокировка приложения"
    ),
    Display(
        title = "Отображение",
        icon = R.drawable.crop_free,
        subtitle = "Вырез экрана и отступы"
    ),
    Network(
        title = "Сеть и DNS",
        icon = R.drawable.ic_dns_24,
        subtitle = "DNS-over-HTTPS, IPv4/IPv6"
    ),
    WebServer(
        title = "Просмотр на ПК",
        icon = R.drawable.hard_drive_2_24,
        subtitle = "Локальный Web-сервер по Wi-Fi"
    ),
    Cache(
        title = "Кэш",
        icon = R.drawable.hard_disk_24,
        subtitle = "RAM, изображения и очистка"
    ),
    L(
        title = "L",
        icon = R.drawable.icon_luscious,
        subtitle = "Профиль, миниатюры и колонки"
    ),
    Red(
        title = "R",
        icon = R.drawable.icon_red,
        subtitle = "Размеры папок, Downloads и Niches cache"
    ),
    X(
        title = "X",
        icon = R.drawable.icon_xvideos_white,
        subtitle = "Отображение и фильтры X"
    ),
    Storage(
        title = "Статистика",
        icon = R.drawable.hard_drive_2_24,
        subtitle = "Статистика по X, L и R"
    ),
    Backup(
        title = "Backup",
        icon = R.drawable.hard_drive_2_24,
        subtitle = "X, L, R в ZIP"
    ),
    P2P(
        title = "P2P",
        icon = R.drawable.icon_red, // Replace with appropriate icon if available
        subtitle = "Передача файлов рядом"
    );

    companion object {
        val primaryPages: List<SettingsPage> = listOf(Privacy, Display, Network, WebServer, Cache, Storage, Backup, P2P)
        val contentPages: List<SettingsPage> = listOf(X, L, Red)
        val detailPages: List<SettingsPage>
            get() = primaryPages + contentPages
    }
}

@Composable
private fun SettingsNavigationRow(
    page: SettingsPage,
    onClick: () -> Unit
) {
    SettingsListItem(
        icon = page.icon,
        text = page.title,
        subtitle = page.subtitle,
        onClick = onClick
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsNavigationRowPreview() = SettingsPreview {
    SettingsNavigationRow(
        page = SettingsPage.Display,
        onClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F,
    device = "spec:width=1080px,height=23400px,dpi=440"
)
@Composable
private fun AppSettingsScreenPreview() {
    val context = LocalContext.current
    Settings.init(context.getSharedPreferences("preview_prefs", 0))
    XvideosTheme {
        Column {
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .fillMaxWidth()
                    .background(SettingsTopBarColor)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = SettingsRowTextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Настройки",
                    modifier = Modifier.weight(1f),
                    color = SettingsRowTextPrimary,
                    style = Theme.L.Type.screenTitle.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = SettingsRowTextPrimary,
                        textAlign = TextAlign.Start
                    )
                )
            }
            AppSettingsScreenBody(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                imageCacheSizeBytes = 128_000_000L,
                storageStats = EmptyStorageStats,
                sizeRedTotal = 512_000_000L,
                sizeRedDownload = 64_000_000L,
                onClearImageCache = {},
                onClearDownload = {},
                data = SettingsDataHolders(),
                context = context.applicationContext,
                onBackupDataChanged = {}
            )
        }
    }
}
