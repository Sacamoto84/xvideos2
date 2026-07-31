package com.client.xvideos.common.settings.ui

import com.client.xvideos.R
import com.client.xvideos.common.settings.ui.section.CacheSettingsSection
import com.client.xvideos.common.settings.ui.section.DisplaySettingsSection
import com.client.xvideos.common.settings.ui.section.LSettingsSection
import com.client.xvideos.common.settings.ui.section.P2PSettingsSection
import com.client.xvideos.common.settings.ui.section.RSettingsSection
import com.client.xvideos.common.settings.ui.section.XSettingsSection
import com.client.xvideos.common.settings.ui.backup.BackupSettingsSection
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.settings.ui.components.AppLockSettingsSection
import com.client.xvideos.common.settings.ui.components.EmptyStorageStats
import com.client.xvideos.common.settings.ui.components.SettingsAccentColor
import com.client.xvideos.common.settings.ui.components.SettingsDivider
import com.client.xvideos.common.settings.ui.components.SettingsGroup
import com.client.xvideos.common.settings.ui.components.SettingsListItem
import com.client.xvideos.common.settings.ui.components.SettingsRowTextPrimary
import com.client.xvideos.common.settings.ui.components.SettingsScreenBackground
import com.client.xvideos.common.settings.ui.components.SettingsSectionTitle
import com.client.xvideos.common.settings.ui.components.SettingsTopBarColor
import com.client.xvideos.common.settings.ui.components.StorageStatisticsSection
import com.client.xvideos.common.settings.ui.components.StorageStat
import com.client.xvideos.common.settings.ui.components.loadStorageStats
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.getFolderSize
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
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

class AppSettingsSM @Inject constructor(
    val savedRed: SavedRed,
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

    override val key: ScreenKey = uniqueScreenKey

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
                scope.launch {
                    withContext(Dispatchers.IO) { File(AppPath.r_cache_download).deleteRecursively() }
                    refreshRedSizes()
                    SnackBar.success("Папка Download очищена")
                }
            },
            savedRed = vm.savedRed,
            downloadRed = vm.downloadRed,
            savedL = vm.savedL,
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
    savedRed: SavedRed?,
    downloadRed: DownloadRed?,
    savedL: SavedL?,
    context: Context,
    onBackupDataChanged: () -> Unit,
    onRefreshFileStats: () -> Unit
) {
    var currentPage by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    val closeCurrentPage = {
        if (currentPage == SettingsPage.Main) {
            onBack()
        } else {
            currentPage = SettingsPage.Main
        }
    }

    BackHandler(enabled = currentPage != SettingsPage.Main) {
        currentPage = SettingsPage.Main
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
                    .height(52.dp)
                    .fillMaxWidth()
                    .background(SettingsTopBarColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = closeCurrentPage) {
                    Icon(
                        painterResource(R.drawable.exo_ic_chevron_left),
                        contentDescription = null,
                        tint = SettingsAccentColor
                    )
                }
                Text(
                    currentPage.title,
                    modifier = Modifier.weight(1f),
                    color = SettingsRowTextPrimary,
                    style = Theme.L.Type.rowTitle.copy(
                        color = SettingsRowTextPrimary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.width(48.dp))
            }
        },
        containerColor = SettingsScreenBackground
    ) { paddingValues ->
        AppSettingsScreenBody(
            modifier = Modifier.padding(paddingValues),
            currentPage = currentPage,
            onOpenPage = { currentPage = it },
            imageCacheSizeBytes = imageCacheSizeBytes,
            storageStats = storageStats,
            sizeRedTotal = sizeRedTotal,
            sizeRedDownload = sizeRedDownload,
            onClearImageCache = onClearImageCache,
            onClearDownload = onClearDownload,
            savedRed = savedRed,
            downloadRed = downloadRed,
            savedL = savedL,
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
    savedRed: SavedRed?,
    downloadRed: DownloadRed?,
    savedL: SavedL?,
    context: Context,
    onBackupDataChanged: () -> Unit
) {
    val ramCachePercent = Settings.image_cache_ram_percent.field.collectAsStateWithLifecycle().value
    val diskCacheEnabled = Settings.image_cache_disk_enabled.field.collectAsStateWithLifecycle().value
    val diskCacheSizeMb = Settings.image_cache_disk_size_mb.field.collectAsStateWithLifecycle().value
    val l_login = Settings.l_login.field.collectAsStateWithLifecycle().value

    val isNichesCacheDownloading = savedRed?.nichesCache?.isDownloading ?: false
    val nichesCacheProgress = savedRed?.nichesCache?.progress ?: 0f
    val nichesCacheSize = savedRed?.nichesCache?.size ?: 0
    val nichesCacheLastModifiedHour = savedRed?.nichesCache?.lastModifiedHour ?: 0L

    Column(
        modifier = modifier
            .background(SettingsScreenBackground)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        if (currentPage == SettingsPage.Main) {
            SettingsSectionTitle("Основное")
            SettingsGroup {
                SettingsPage.primaryPages.forEachIndexed { index, page ->
                    if (index > 0) SettingsDivider()
                    SettingsNavigationRow(
                        page = page,
                        onClick = { onOpenPage(page) }
                    )
                }
            }
            SettingsSectionTitle("Разделы")
            SettingsGroup {
                SettingsPage.contentPages.forEachIndexed { index, page ->
                    if (index > 0) SettingsDivider()
                    SettingsNavigationRow(
                        page = page,
                        onClick = { onOpenPage(page) }
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            when (currentPage) {
                SettingsPage.Main -> Unit
                SettingsPage.Privacy -> AppLockSettingsSection()
                SettingsPage.Display -> DisplaySettingsSection()
                SettingsPage.Cache -> CacheSettingsSection(
                    ramCachePercent = ramCachePercent,
                    diskCacheEnabled = diskCacheEnabled,
                    diskCacheSizeMb = diskCacheSizeMb,
                    imageCacheSizeBytes = imageCacheSizeBytes,
                    onClearImageCache = onClearImageCache,
                    context = context
                )
                SettingsPage.L -> LSettingsSection(lLogin = l_login)
                SettingsPage.Red -> RSettingsSection(
                    sizeRedTotal = sizeRedTotal,
                    sizeRedDownload = sizeRedDownload,
                    onClearDownload = onClearDownload,
                    savedRed = savedRed,
                    downloadRed = downloadRed,
                    isNichesCacheDownloading = isNichesCacheDownloading,
                    nichesCacheProgress = nichesCacheProgress,
                    nichesCacheSize = nichesCacheSize,
                    nichesCacheLastModifiedHour = nichesCacheLastModifiedHour
                )
                SettingsPage.X -> XSettingsSection()
                SettingsPage.Storage -> StorageStatisticsSection(storageStats)
                SettingsPage.Backup -> BackupSettingsSection(
                    context = context,
                    downloadRed = downloadRed,
                    savedL = savedL,
                    onDataChanged = onBackupDataChanged
                )
                SettingsPage.P2P -> P2PSettingsSection()
            }
        }
    }
}

private enum class SettingsPage(
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
        icon = R.drawable.icon_red,
        subtitle = "Пароль и блокировка приложения"
    ),
    Display(
        title = "Отображение",
        icon = R.drawable.crop_free,
        subtitle = "Вырез экрана и отступы"
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
        val primaryPages: List<SettingsPage> = listOf(Privacy, Display, Cache, Storage, Backup, P2P)
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
        onClick = onClick,
        trailing = {
            Icon(
                painter = painterResource(R.drawable.exo_ic_chevron_right),
                contentDescription = null,
                tint = SettingsAccentColor
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535,
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
                    .height(52.dp)
                    .fillMaxWidth()
                    .background(SettingsTopBarColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(48.dp))
                Text(
                    "Настройки",
                    modifier = Modifier.weight(1f),
                    color = SettingsRowTextPrimary,
                    style = Theme.L.Type.rowTitle.copy(
                        color = SettingsRowTextPrimary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.width(48.dp))
            }
            AppSettingsScreenBody(
                imageCacheSizeBytes = 128_000_000L,
                storageStats = EmptyStorageStats,
                sizeRedTotal = 512_000_000L,
                sizeRedDownload = 64_000_000L,
                onClearImageCache = {},
                onClearDownload = {},
                savedRed = null,
                downloadRed = null,
                savedL = null,
                context = context.applicationContext,
                onBackupDataChanged = {}
            )
        }
    }
}
