package com.client.xvideos

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.state.ToggleableState
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
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.settings.ui.Config_G_0_4
import com.client.xvideos.common.settings.ui.components.AppLockSettingsSection
import com.client.xvideos.common.settings.ui.components.EmptyStorageStats
import com.client.xvideos.common.settings.ui.components.IntSliderSetting
import com.client.xvideos.common.settings.ui.components.SettingsAccentColor
import com.client.xvideos.common.settings.ui.components.SettingsCardColor
import com.client.xvideos.common.settings.ui.components.SettingsButtonRowWithDialog
import com.client.xvideos.common.settings.ui.components.SettingsDivider
import com.client.xvideos.common.settings.ui.components.SettingsDividerColor
import com.client.xvideos.common.settings.ui.components.SettingsGroup
import com.client.xvideos.common.settings.ui.components.SettingsListItem
import com.client.xvideos.common.settings.ui.components.SettingsPreview
import com.client.xvideos.common.settings.ui.components.SettingsRowTextPrimary
import com.client.xvideos.common.settings.ui.components.SettingsScreenBackground
import com.client.xvideos.common.settings.ui.components.SettingsSectionTitle
import com.client.xvideos.common.settings.ui.components.SettingsSwitchRow
import com.client.xvideos.common.settings.ui.components.SettingsTopBarColor
import com.client.xvideos.common.settings.ui.components.SettingsValueRow
import com.client.xvideos.common.settings.ui.components.StorageStatisticsSection
import com.client.xvideos.common.settings.ui.components.StorageStat
import com.client.xvideos.common.settings.ui.components.ThumbnailSizeSelector
import com.client.xvideos.common.settings.ui.components.WhatsAppGreen
import com.client.xvideos.common.settings.ui.components.loadStorageStats
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.common.util.getFolderSize
import com.client.xvideos.common.util.toPrettyCount3
import com.client.xvideos.l.model.ThumbnailsSize
import com.client.xvideos.l.featured.saved.LDownloadRecoveryReport
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport
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
                    style = ThemeL.Type.rowTitle.copy(
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
    );

    companion object {
        val primaryPages: List<SettingsPage> = listOf(Privacy, Cache, Storage, Backup)
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

@Composable
private fun CacheSettingsSection(
    ramCachePercent: Int,
    diskCacheEnabled: Boolean,
    diskCacheSizeMb: Int,
    imageCacheSizeBytes: Long,
    onClearImageCache: () -> Unit,
    context: Context
) {
    SettingsGroup {
        IntSliderSetting(
            text = "RAM кэш картинок",
            value = CoilImageLoaderFactory.normalizedRamCachePercent(ramCachePercent),
            min = CoilImageLoaderFactory.MIN_RAM_CACHE_PERCENT,
            max = CoilImageLoaderFactory.MAX_RAM_CACHE_PERCENT,
            step = 1,
            suffix = "%",
            icon = R.drawable.memory_24,
            onValueChangeFinished = { value ->
                Settings.image_cache_ram_percent.setValue(value)
                CoilImageLoaderFactory.recreate(context)
                SnackBar.success("RAM кэш картинок: $value%")
            }
        )
        SettingsDivider()

        SettingsSwitchRow(
            icon = R.drawable.hard_disk_24,
            text = "Дисковый кэш картинок",
            subtitle = if (diskCacheEnabled) "Включён" else "Выключен",
            value = diskCacheEnabled,
            onValueChange = { enabled ->
                Settings.image_cache_disk_enabled.setValue(enabled)
                CoilImageLoaderFactory.recreate(context)
                SnackBar.success(if (enabled) "Дисковый кэш включен" else "Дисковый кэш выключен")
            }
        )
        SettingsDivider()

        IntSliderSetting(
            text = "Лимит кэша картинок",
            value = CoilImageLoaderFactory.normalizedDiskCacheSizeMb(diskCacheSizeMb),
            min = CoilImageLoaderFactory.MIN_DISK_CACHE_SIZE_MB,
            max = CoilImageLoaderFactory.MAX_DISK_CACHE_SIZE_MB,
            step = 50,
            suffix = " MB",
            icon = R.drawable.hard_drive_2_24,
            enabled = diskCacheEnabled,
            onValueChangeFinished = { value ->
                Settings.image_cache_disk_size_mb.setValue(value)
                CoilImageLoaderFactory.recreate(context)
                SnackBar.success("Размер кэша картинок: $value MB")
            }
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_luscious,
            text = "Кэш картинок на диске",
            value = formatBytes(imageCacheSizeBytes)
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_luscious,
            text = "Очистить кэш картинок",
            value = "Очистить",
            textDialogTitle = "Очистить кэш картинок",
            textDialogBody = "Размер на диске: ${formatBytes(imageCacheSizeBytes)}",
            textDialogButton = "Очистить",
            onClick = onClearImageCache
        )
    }
}

@Composable
private fun LSettingsSection(lLogin: String) {
    SettingsGroup {
        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_luscious,
            text = "Профиль L",
            value = if (lLogin.isBlank()) "Нет" else "Выйти",
            textDialogTitle = "Выйти из профиля L",
            textDialogBody = if (lLogin.isBlank()) {
                "Вы не авторизованы в L."
            } else {
                "При следующем открытии L нужно будет снова ввести логин и пароль: $lLogin"
            },
            textDialogButton = "Выйти",
            onClick = {
                Settings.l_login.setValue("")
                Settings.l_pass.setValue("")
                SnackBar.success("Профиль L закрыт")
            }
        )
        SettingsDivider()

        val thumbnailSize = Settings.thumbalistSize.field.collectAsStateWithLifecycle().value
        val currentDisplayName = ThumbnailsSize.fromValue(thumbnailSize)?.displayName ?: "?"
        SettingsValueRow(
            icon = R.drawable.icon_luscious,
            text = "Размер миниатюры",
            value = currentDisplayName
        )
        ThumbnailSizeSelector(
            currentValue = currentDisplayName,
            onSelected = { selectedDisplayName ->
                ThumbnailsSize.fromDisplayName(selectedDisplayName)?.apply {
                    Settings.thumbalistSize.setValue(value)
                    SnackBar.success("Размер миниатюры: $displayName")
                }
            }
        )
        SettingsDivider()

        Config_G_0_4("L Gifs", Settings.l_gifsTab_G_0_4)
        Config_G_0_4("L Likes", Settings.l_likesTab_G_0_4)
        Config_G_0_4("L Collection", Settings.l_collectionTab_G_0_4)
    }
}

@Composable
private fun RSettingsSection(
    sizeRedTotal: Long,
    sizeRedDownload: Long,
    onClearDownload: () -> Unit,
    savedRed: SavedRed?,
    downloadRed: DownloadRed?,
    isNichesCacheDownloading: Boolean,
    nichesCacheProgress: Float,
    nichesCacheSize: Int,
    nichesCacheLastModifiedHour: Long
) {
    val scope = rememberCoroutineScope()
    var isRecoveringDownload by remember { mutableStateOf(false) }
    var recoveryReport by remember { mutableStateOf<RedDownloadRecoveryReport?>(null) }

    SettingsGroup {
        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Размер всех папок Red",
            value = sizeRedTotal.toPrettyCount3()
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Размер папки Download",
            value = sizeRedDownload.toPrettyCount3()
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_red,
            text = "Очистить папку Download",
            value = "Очистить",
            textDialogTitle = "Очистка папки Download",
            textDialogBody = "Подтвердить очистку: ${sizeRedDownload.toPrettyCount3()}",
            textDialogButton = "Очистить",
            onClick = onClearDownload
        )
        SettingsDivider()

        SettingsListItem(
            icon = R.drawable.hard_drive_2_24,
            text = "Докачать Download по .info",
            subtitle = redDownloadRecoveryText(recoveryReport, isRecoveringDownload),
            trailing = {
                Button(
                    enabled = downloadRed != null && !isRecoveringDownload,
                    onClick = {
                        val redDownloader = downloadRed ?: return@Button
                        isRecoveringDownload = true
                        scope.launch {
                            redDownloader.recoverIncompleteDownloads(onComplete = { report ->
                                scope.launch {
                                    recoveryReport = report
                                    isRecoveringDownload = false
                                    if (report.incompleteItems == 0) {
                                        SnackBar.success("Download проверен: все файлы на месте")
                                    } else {
                                        SnackBar.success(
                                            "Запущено: видео ${report.queuedVideo}, превью ${report.queuedPreview}"
                                        )
                                    }
                                }
                            })
                        }
                    }
                ) {
                    Text("Старт")
                }
            }
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Кэш Niches",
            value = "$nichesCacheSize \u2022 ${nichesCacheLastModifiedHour}h"
        )

        if (isNichesCacheDownloading) {
            LinearProgressIndicator(
                progress = { nichesCacheProgress },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            color = WhatsAppGreen,
            trackColor = SettingsDividerColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        }
        SettingsDivider()

        SettingsListItem(
            icon = R.drawable.icon_red,
            text = "Обновить кэш Niches",
            subtitle = if (isNichesCacheDownloading) "Идёт обновление" else "Данные для поиска и фильтров R",
            trailing = {
                Button(
                    enabled = savedRed != null && !isNichesCacheDownloading,
                    onClick = { savedRed?.nichesCache?.refresh() }
                ) {
                    Text("Обновить")
                }
            }
        )
    }
}

private fun redDownloadRecoveryText(
    report: RedDownloadRecoveryReport?,
    isWorking: Boolean
): String {
    if (isWorking) return "Сканирование .info и запуск недостающих загрузок"
    if (report == null) return "Если после backup есть только .info, скачает недостающие mp4/jpg"
    if (report.incompleteItems == 0) return "Все файлы на месте: ${report.totalInfoFiles} info"
    return "Найдено ${report.incompleteItems} из ${report.totalInfoFiles} • видео ${report.queuedVideo} • превью ${report.queuedPreview}"
}

private fun redDownloadRecoveryConsoleText(report: RedDownloadRecoveryReport): String {
    return listOf(
        "---------",
        "R итог",
        "Info: всего ${report.totalInfoFiles}, неполных ${report.incompleteItems}",
        "Скачано/очередь: видео ${report.queuedVideo}, preview ${report.queuedPreview}",
        "Пропущено: нет video URL ${report.skippedNoVideoUrl}, нет preview URL ${report.skippedNoPreviewUrl}",
        "Ошибки: битых info ${report.invalidInfoFiles}"
    ).joinToString("\n")
}

private fun lDownloadRecoveryConsoleText(report: LDownloadRecoveryReport): String {
    return listOf(
        "---------",
        "L итог",
        "Metadata: всего ${report.totalMetadataFiles}, неполных ${report.incompleteItems}",
        "Скачано: media ${report.downloadedMedia}, preview ${report.downloadedPreview}",
        "Пропущено: нет media URL ${report.skippedNoMediaUrl}, нет preview URL ${report.skippedNoPreviewUrl}",
        "Ошибки: media ${report.failedMedia}, preview ${report.failedPreview}, битых metadata ${report.invalidMetadataFiles}"
    ).joinToString("\n")
}

private fun shouldAutoRecoverL(selectedPaths: Set<String>): Boolean {
    return selectedPaths.any { path ->
        path == "L" || path == "L/Likes" || path.startsWith("L/Likes/") ||
                path == "L/Collection" || path.startsWith("L/Collection/")
    }
}

private fun shouldAutoRecoverRedDownload(selectedPaths: Set<String>): Boolean {
    return selectedPaths.any { path ->
        path == "R" || path == "R/Download" || path.startsWith("R/Download/")
    }
}

@Composable
private fun XSettingsSection() {
    val xvideosRow2 = Settings.xvideos_row2.field.collectAsStateWithLifecycle().value
    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.icon_xvideos_white,
            text = "2 столбика",
            subtitle = if (xvideosRow2) "Включено" else "Выключено",
            value = xvideosRow2,
            onValueChange = { Settings.xvideos_row2.setValue(it) }
        )
        SettingsDivider()

        val xvideosShemale = Settings.xvideos_shemale.field.collectAsStateWithLifecycle().value
        SettingsSwitchRow(
            icon = R.drawable.icon_xvideos_white,
            text = "Shemale",
            subtitle = if (xvideosShemale) "Включено" else "Выключено",
            value = xvideosShemale,
            onValueChange = { Settings.xvideos_shemale.setValue(it) }
        )
    }
}

private enum class BackupFlowScreen(val title: String) {
    CREATE("Создать"),
    RESTORE("Восстановить")
}

@Composable
private fun BackupSettingsSection(
    context: Context,
    downloadRed: DownloadRed?,
    savedL: SavedL?,
    onDataChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf(BackupFlowScreen.CREATE) }
    var isWorking by rememberSaveable { mutableStateOf(false) }
    var lBackupMode by rememberSaveable { mutableStateOf(XlrBackupContentMode.MINI) }
    var rBackupMode by rememberSaveable { mutableStateOf(XlrBackupContentMode.MINI) }
    var backupItems by remember { mutableStateOf<List<XlrBackupItem>>(emptyList()) }
    var selectedBackupPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreItems by remember { mutableStateOf<List<XlrBackupItem>>(emptyList()) }
    var selectedRestorePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val backupConsole = remember { mutableStateListOf<String>() }
    val backupOptions = remember(lBackupMode, rBackupMode) {
        XlrBackupOptions(lMode = lBackupMode, rMode = rBackupMode)
    }

    fun appendBackupLog(message: String) {
        if (backupConsole.size >= 200) {
            backupConsole.removeAt(0)
        }
        backupConsole.add(message)
    }

    suspend fun refreshBackupItems() {
        val items = XlrBackupManager.currentBackupItems(backupOptions)
        backupItems = items
        if (selectedBackupPaths.isEmpty()) {
            selectedBackupPaths = initialSectionSelection(items)
        }
    }

    LaunchedEffect(backupOptions) {
        refreshBackupItems()
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null || isWorking) return@rememberLauncherForActivityResult
        if (selectedBackupPaths.isEmpty()) {
            SnackBar.error("Выберите хотя бы одну папку")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            isWorking = true
            appendBackupLog(
                "Создание backup: L=${backupContentModeTitle(backupOptions.lMode)}, R=${backupContentModeTitle(backupOptions.rMode)}"
            )
            XlrBackupManager.createBackup(context, uri, selectedBackupPaths, backupOptions)
                .onSuccess { report ->
                    appendBackupLog("Backup создан: ${report.files} файлов, ${formatBytes(report.bytes)}")
                    SnackBar.success("Backup создан: ${report.files} файлов, ${formatBytes(report.bytes)}")
                    refreshBackupItems()
                }
                .onFailure { error ->
                    appendBackupLog("Ошибка создания backup: ${error.message ?: error::class.java.simpleName}")
                    SnackBar.error(error.message ?: "Ошибка создания backup")
                }
            isWorking = false
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null || isWorking) return@rememberLauncherForActivityResult
        scope.launch {
            isWorking = true
            XlrBackupManager.inspectBackup(context, uri)
                .onSuccess { items ->
                    restoreUri = uri
                    restoreItems = items
                    selectedRestorePaths = initialSectionSelection(items)
                    SnackBar.success("Backup открыт: ${items.size} папок")
                }
                .onFailure { error ->
                    restoreUri = null
                    restoreItems = emptyList()
                    selectedRestorePaths = emptySet()
                    SnackBar.error(error.message ?: "Ошибка чтения backup")
                }
            isWorking = false
        }
    }

    val backupReport = XlrBackupManager.reportForSelection(backupItems, selectedBackupPaths)
    val restoreReport = XlrBackupManager.reportForSelection(restoreItems, selectedRestorePaths)

    SettingsGroup {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = "Backup X/L/R",
            value = if (screen == BackupFlowScreen.CREATE) {
                "Создание архива выбранных папок. DB, настройки и кеши не входят в ZIP."
            } else {
                "Восстановление заменяет выбранные папки. Для R Download после restore автоматически проверяются .info."
            }
        )
        BackupModeSelector(
            selected = screen,
            enabled = !isWorking,
            onSelected = { screen = it }
        )
        SettingsDivider()

        when (screen) {
            BackupFlowScreen.CREATE -> {
                SettingsValueRow(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Выбрано для архива",
                    value = if (isWorking) "Идет операция" else selectionSummaryText(backupReport)
                )
                SettingsDivider()
                BackupContentModeSelector(
                    title = "L backup",
                    value = lBackupMode,
                    enabled = !isWorking,
                    description = "Мини: Likes/Collection без медиа, только metadata",
                    onValueChange = { lBackupMode = it }
                )
                SettingsDivider()
                BackupContentModeSelector(
                    title = "R backup",
                    value = rBackupMode,
                    enabled = !isWorking,
                    description = "Мини: Download без mp4/jpg, только .info",
                    onValueChange = { rBackupMode = it }
                )
                SettingsDivider()
                BackupSelectionActions(
                    enabled = !isWorking,
                    onSelectAll = { selectedBackupPaths = initialSectionSelection(backupItems) },
                    onSelectNone = { selectedBackupPaths = emptySet() }
                )
                BackupFolderList(
                    items = backupItems,
                    selectedPaths = selectedBackupPaths,
                    enabled = !isWorking,
                    onToggle = { path -> selectedBackupPaths = toggleBackupPath(backupItems, selectedBackupPaths, path) }
                )
                SettingsDivider()
                SettingsListItem(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Создать ZIP",
                    subtitle = selectionSummaryText(backupReport),
                    trailing = {
                        Button(
                            enabled = !isWorking && selectedBackupPaths.isNotEmpty(),
                            onClick = { createBackupLauncher.launch(XlrBackupManager.defaultFileName()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsAccentColor,
                                contentColor = SettingsScreenBackground
                            )
                        ) {
                            Text("Создать")
                        }
                    }
                )
            }

            BackupFlowScreen.RESTORE -> {
                SettingsListItem(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Открыть ZIP",
                    subtitle = restoreUri?.lastPathSegment ?: "Сначала выберите архив",
                    trailing = {
                        Button(
                            enabled = !isWorking,
                            onClick = {
                                restoreBackupLauncher.launch(
                                    arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsAccentColor,
                                contentColor = SettingsScreenBackground
                            )
                        ) {
                            Text("Выбрать")
                        }
                    }
                )

                if (restoreItems.isEmpty()) {
                    SettingsDivider()
                    SettingsValueRow(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Что восстановить",
                        value = "Выберите ZIP-файл, после этого появятся папки X, L и R из архива."
                    )
                } else {
                    SettingsDivider()
                    SettingsValueRow(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Выбрано для восстановления",
                        value = if (isWorking) "Идет операция" else selectionSummaryText(restoreReport)
                    )
                    BackupSelectionActions(
                        enabled = !isWorking,
                        onSelectAll = { selectedRestorePaths = initialSectionSelection(restoreItems) },
                        onSelectNone = { selectedRestorePaths = emptySet() }
                    )
                    BackupFolderList(
                        items = restoreItems,
                        selectedPaths = selectedRestorePaths,
                        enabled = !isWorking,
                        onToggle = { path ->
                            selectedRestorePaths = toggleBackupPath(restoreItems, selectedRestorePaths, path)
                        }
                    )
                    SettingsDivider()
                    SettingsButtonRowWithDialog(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Восстановить выбранное",
                        value = if (isWorking) "Идет..." else "Восстановить",
                        textDialogTitle = "Восстановить backup",
                        textDialogBody = "Выбранные папки будут заменены данными из ZIP: ${selectionSummaryText(restoreReport)}. DB, настройки и кеши не трогаются.",
                        textDialogButton = "Восстановить",
                        onClick = {
                            val uri = restoreUri
                            if (uri == null) {
                                SnackBar.error("Сначала выберите ZIP")
                                return@SettingsButtonRowWithDialog
                            }
                            if (selectedRestorePaths.isEmpty()) {
                                SnackBar.error("Выберите хотя бы одну папку")
                                return@SettingsButtonRowWithDialog
                            }
                            if (!isWorking) {
                                scope.launch {
                                    isWorking = true
                                    appendBackupLog("Восстановление backup: ${selectionSummaryText(restoreReport)}")
                                    val autoRecoverL = shouldAutoRecoverL(selectedRestorePaths)
                                    val autoRecoverRedDownload = shouldAutoRecoverRedDownload(selectedRestorePaths)
                                    XlrBackupManager.restoreBackup(context, uri, selectedRestorePaths)
                                        .onSuccess { report ->
                                            refreshBackupItems()
                                            onDataChanged()
                                            SnackBar.success("Backup восстановлен: ${report.files} файлов. Перезапустите приложение.")
                                            appendBackupLog("Backup восстановлен: ${report.files} файлов, ${formatBytes(report.bytes)}")
                                            if (autoRecoverL) {
                                                val lSaved = savedL
                                                if (lSaved == null) {
                                                    SnackBar.error("L Likes/Collection восстановлены, но L-загрузчик недоступен")
                                                } else {
                                                    appendBackupLog("L Likes/Collection: сканирую metadata")
                                                    lSaved.recoverIncompleteSavedMedia(
                                                        onEvent = { message ->
                                                            scope.launch { appendBackupLog(message) }
                                                        },
                                                        onComplete = { recoveryReport ->
                                                            scope.launch { appendBackupLog(lDownloadRecoveryConsoleText(recoveryReport)) }
                                                        }
                                                    )
                                                }
                                            }
                                            if (autoRecoverRedDownload) {
                                                val redDownloader = downloadRed
                                                if (redDownloader == null) {
                                                    SnackBar.error("R Download восстановлен, но загрузчик недоступен")
                                                } else {
                                                    appendBackupLog("R Download: сканирую .info")
                                                    redDownloader.recoverIncompleteDownloads(
                                                        onEvent = { message ->
                                                            scope.launch { appendBackupLog(message) }
                                                        },
                                                        onComplete = { recoveryReport ->
                                                            scope.launch { appendBackupLog(redDownloadRecoveryConsoleText(recoveryReport)) }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        .onFailure { error ->
                                            appendBackupLog("Ошибка восстановления backup: ${error.message ?: error::class.java.simpleName}")
                                            SnackBar.error(error.message ?: "Ошибка восстановления backup")
                                        }
                                    isWorking = false
                                }
                            }
                        }
                    )
                }
            }
        }
        SettingsDivider()
        BackupConsole(
            lines = backupConsole,
            onClear = { backupConsole.clear() }
        )
    }
}

@Composable
private fun BackupModeSelector(
    selected: BackupFlowScreen,
    enabled: Boolean,
    onSelected: (BackupFlowScreen) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        BackupFlowScreen.entries.forEachIndexed { index, item ->
            SegmentedButton(
                enabled = enabled,
                selected = selected == item,
                onClick = { onSelected(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = BackupFlowScreen.entries.size,
                    baseShape = RoundedCornerShape(8.dp)
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = SettingsAccentColor,
                    activeContentColor = SettingsScreenBackground,
                    activeBorderColor = SettingsAccentColor,
                    inactiveContainerColor = SettingsCardColor,
                    inactiveContentColor = ThemeL.textColor,
                    inactiveBorderColor = SettingsDividerColor
                ),
                label = {
                    Text(
                        text = item.title,
                        color = if (selected == item) SettingsScreenBackground else SettingsRowTextPrimary,
                        style = ThemeL.Type.button
                    )
                }
            )
        }
    }
}

@Composable
private fun BackupContentModeSelector(
    title: String,
    value: XlrBackupContentMode,
    enabled: Boolean,
    description: String,
    onValueChange: (XlrBackupContentMode) -> Unit
) {
    SettingsValueRow(
        icon = R.drawable.hard_drive_2_24,
        text = title,
        value = "${backupContentModeTitle(value)} • $description"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        XlrBackupContentMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                enabled = enabled,
                selected = value == mode,
                onClick = { onValueChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = XlrBackupContentMode.entries.size
                ),
                label = { Text(backupContentModeTitle(mode)) }
            )
        }
    }
}

private fun backupContentModeTitle(mode: XlrBackupContentMode): String {
    return when (mode) {
        XlrBackupContentMode.MINI -> "Мини"
        XlrBackupContentMode.FULL -> "Полный"
    }
}

@Composable
private fun BackupConsole(
    lines: List<String>,
    onClear: () -> Unit
) {
    val visibleLines = lines
        .ifEmpty { listOf("Пока пусто") }
        .flatMap { entry -> entry.lineSequence().toList() }

    SettingsListItem(
        icon = R.drawable.hard_drive_2_24,
        text = "Консоль backup",
        subtitle = if (lines.isEmpty()) "Здесь будет процесс восстановления файлов из сети" else "${visibleLines.size} строк",
        trailing = {
            TextButton(
                enabled = lines.isNotEmpty(),
                onClick = onClear
            ) {
                Text("Очистить", color = SettingsAccentColor)
            }
        }
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(SettingsTopBarColor, RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Column {
            visibleLines.forEach { line ->
                BackupConsoleLine(line)
            }
        }
    }
}

@Composable
private fun BackupConsoleLine(line: String) {
    val lower = line.lowercase()
    val isSummary = line.startsWith("---------") || lower.contains("итог")
    val isError = lower.contains("ошиб") ||
            lower.contains("бит") ||
            lower.contains("не скачан") ||
            lower.contains("failed")
    val isSuccess = lower.contains("скачано") ||
            lower.contains("очередь") ||
            lower.contains("готов") ||
            lower.contains("создан") ||
            lower.contains("восстановлен")
    val isWarning = lower.contains("пропущено") ||
            lower.contains("нет ")
    val color = when {
        isSummary -> SettingsAccentColor
        isError -> ThemeL.r0
        isSuccess -> ThemeL.g0
        isWarning -> ThemeL.lavender
        else -> SettingsRowTextPrimary
    }
    val style = if (isSummary) {
        ThemeL.Type.rowTitle.copy(color = color)
    } else {
        ThemeL.Type.rowSubtitle.copy(color = color)
    }

    Text(
        text = line,
        color = color,
        style = style,
        modifier = Modifier.padding(vertical = if (isSummary) 2.dp else 1.dp)
    )
}

@Composable
private fun BackupSelectionActions(
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            enabled = enabled,
            onClick = onSelectAll
        ) {
            Text("Все X/L/R")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            enabled = enabled,
            onClick = onSelectNone
        ) {
            Text("Снять", color = SettingsAccentColor)
        }
    }
}

@Composable
private fun BackupFolderList(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit
) {
    if (items.isEmpty()) {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = "Папки",
            value = "Нет данных для backup"
        )
        return
    }

    var expandedSections by rememberSaveable { mutableStateOf(emptyList<String>()) }

    items
        .filter { it.parentPath == null }
        .forEachIndexed { index, section ->
            val children = items.filter { it.parentPath == section.path }
            if (index > 0) SettingsDivider()
            BackupSectionGroup(
                section = section,
                children = children,
                items = items,
                selectedPaths = selectedPaths,
                expanded = section.path in expandedSections,
                enabled = enabled,
                onToggleExpanded = {
                    expandedSections = if (section.path in expandedSections) {
                        expandedSections - section.path
                    } else {
                        expandedSections + section.path
                    }
                },
                onToggle = onToggle
            )
        }
}

@Composable
private fun BackupSectionGroup(
    section: XlrBackupItem,
    children: List<XlrBackupItem>,
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (String) -> Unit
) {
    val state = backupSectionToggleState(items, selectedPaths, section)

    SettingsListItem(
        icon = backupItemIcon(section.section),
        text = section.title,
        subtitle = "${section.files} файлов • ${formatBytes(section.bytes)}",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TriStateCheckbox(
                    state = state,
                    enabled = enabled,
                    onClick = { onToggle(section.path) }
                )
                TextButton(
                    enabled = enabled,
                    onClick = onToggleExpanded
                ) {
                    Icon(
                        painter = painterResource(R.drawable.exo_ic_chevron_right),
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        tint = SettingsAccentColor,
                        modifier = Modifier.rotate(if (expanded) 90f else 0f)
                    )
                }
            }
        },
        onClick = { if (enabled) onToggleExpanded() }
    )

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Column {
            if (children.isEmpty()) {
                SettingsValueRow(
                    icon = backupItemIcon(section.section),
                    text = section.title,
                    value = "Нет вложенных папок"
                )
            } else {
                children.forEach { child ->
                    SettingsDivider()
                    SettingsListItem(
                        icon = backupItemIcon(child.section),
                        text = backupItemTitle(child),
                        subtitle = "${child.path} • ${child.files} файлов • ${formatBytes(child.bytes)}",
                        trailing = {
                            Checkbox(
                                checked = isBackupPathChecked(items, selectedPaths, child),
                                enabled = enabled,
                                onCheckedChange = { onToggle(child.path) }
                            )
                        },
                        onClick = { if (enabled) onToggle(child.path) }
                    )
                }
            }
        }
    }
}

private fun initialSectionSelection(items: List<XlrBackupItem>): Set<String> {
    return items
        .filter { item -> item.parentPath == null && (item.files > 0 || item.bytes > 0L) }
        .mapTo(mutableSetOf()) { it.path }
}

private fun toggleBackupPath(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    path: String
): Set<String> {
    val item = items.firstOrNull { it.path == path } ?: return selectedPaths
    val selected = selectedPaths.toMutableSet()
    val checked = isBackupPathChecked(items, selectedPaths, item)

    if (item.parentPath == null) {
        val children = items.filter { it.parentPath == item.path }.map { it.path }
        selected.remove(item.path)
        selected.removeAll(children)
        if (!checked) selected.add(item.path)
        return selected
    }

    val parentPath = item.parentPath
    val siblings = items.filter { it.parentPath == parentPath }.map { it.path }
    if (parentPath in selected) {
        selected.remove(parentPath)
        selected.addAll(siblings)
    }

    if (checked) {
        selected.remove(item.path)
    } else {
        selected.add(item.path)
    }

    if (siblings.isNotEmpty() && siblings.all { it in selected }) {
        selected.removeAll(siblings)
        selected.add(parentPath)
    }

    return selected
}

private fun isBackupPathChecked(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    item: XlrBackupItem
): Boolean {
    if (item.path in selectedPaths) return true
    if (item.parentPath != null && item.parentPath in selectedPaths) return true
    if (item.parentPath == null) {
        val children = items.filter { it.parentPath == item.path }
        return children.isNotEmpty() && children.all { it.path in selectedPaths }
    }
    return false
}

private fun backupSectionToggleState(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    section: XlrBackupItem
): ToggleableState {
    if (section.path in selectedPaths) return ToggleableState.On
    val children = items.filter { it.parentPath == section.path }
    if (children.isEmpty()) return ToggleableState.Off
    val selectedChildren = children.count { it.path in selectedPaths }
    return when {
        selectedChildren == 0 -> ToggleableState.Off
        selectedChildren == children.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
}

private fun selectionSummaryText(report: XlrBackupReport): String {
    return "${report.files} файлов • ${formatBytes(report.bytes)}"
}

private fun backupItemTitle(item: XlrBackupItem): String {
    return if (item.parentPath == null) item.title else "  ${item.title}"
}

@DrawableRes
private fun backupItemIcon(section: String): Int {
    return when (section) {
        "X" -> R.drawable.icon_xvideos_white
        "L" -> R.drawable.icon_luscious
        "R" -> R.drawable.icon_red
        else -> R.drawable.hard_drive_2_24
    }
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
                    style = ThemeL.Type.rowTitle.copy(
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
