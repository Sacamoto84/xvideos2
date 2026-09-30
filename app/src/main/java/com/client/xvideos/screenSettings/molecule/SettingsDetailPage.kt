package com.client.xvideos.screenSettings.molecule
import com.client.xvideos.screenSettings.model.SettingsDetailParams

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.SettingsDataHolders
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.backup.BackupSettingsSection
import com.client.xvideos.screenSettings.components.AppLockSettingsSection
import com.client.xvideos.screenSettings.components.EmptyStorageStats
import com.client.xvideos.screenSettings.components.StorageStatisticsSection
import com.client.xvideos.screenSettings.section.AppearanceSettingsSection
import com.client.xvideos.screenSettings.section.CacheSettingsSection
import com.client.xvideos.screenSettings.section.LSettingsSection
import com.client.xvideos.screenSettings.section.NetworkSettingsSection
import com.client.xvideos.screenSettings.section.P2PSettingsSection
import com.client.xvideos.screenSettings.section.RSettingsSection
import com.client.xvideos.screenSettings.section.WebServerSettingsSection
import com.client.xvideos.screenSettings.section.XSettingsSection

@Composable
internal fun SettingsDetailPage(
    params: SettingsDetailParams,
    modifier: Modifier = Modifier,
) {
    val ramCachePercent by Settings.image_cache_ram_percent.field.collectAsStateWithLifecycle()
    val diskCacheEnabled by Settings.image_cache_disk_enabled.field.collectAsStateWithLifecycle()
    val diskCacheSizeMb by Settings.image_cache_disk_size_mb.field.collectAsStateWithLifecycle()
    val lLogin by Settings.l_login.field.collectAsStateWithLifecycle()

    val isNichesCacheDownloading = params.data.savedRed?.nichesCache?.isDownloading ?: false
    val nichesCacheProgress = params.data.savedRed?.nichesCache?.progress ?: 0f
    val nichesCacheSize = params.data.savedRed?.nichesCache?.list?.size ?: 0
    val nichesCacheLastModifiedHour = params.data.savedRed?.nichesCache?.lastModifiedHour ?: 0L

    Spacer(Modifier.height(4.dp))
    when (params.currentPage) {
        SettingsPage.Main -> Unit
        SettingsPage.Privacy -> AppLockSettingsSection(modifier = modifier)
        SettingsPage.Network -> NetworkSettingsSection(modifier = modifier)
        SettingsPage.Cache -> CacheSettingsSection(
            ramCachePercent = ramCachePercent,
            diskCacheEnabled = diskCacheEnabled,
            diskCacheSizeMb = diskCacheSizeMb,
            imageCacheSizeBytes = params.imageCacheSizeBytes,
            onClearImageCache = params.onClearImageCache,
            context = params.context,
            modifier = modifier
        )
        SettingsPage.L -> LSettingsSection(lLogin = lLogin, modifier = modifier)
        SettingsPage.Red -> RSettingsSection(
            sizeRedTotal = params.sizeRedTotal,
            sizeRedDownload = params.sizeRedDownload,
            onClearDownload = params.onClearDownload,
            savedRed = params.data.savedRed,
            downloadRed = params.data.downloadRed,
            isNichesCacheDownloading = isNichesCacheDownloading,
            nichesCacheProgress = nichesCacheProgress,
            nichesCacheSize = nichesCacheSize,
            nichesCacheLastModifiedHour = nichesCacheLastModifiedHour,
            modifier = modifier
        )
        SettingsPage.X -> XSettingsSection(modifier = modifier)
        SettingsPage.Appearance -> AppearanceSettingsSection(modifier = modifier)
        SettingsPage.Storage -> StorageStatisticsSection(params.storageStats, modifier = modifier)
        SettingsPage.Backup -> BackupSettingsSection(
            context = params.context,
            data = params.data,
            onDataChanged = params.onBackupDataChanged,
            modifier = modifier
        )
        SettingsPage.P2P -> P2PSettingsSection()
        SettingsPage.WebServer -> WebServerSettingsSection(modifier = modifier)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsDetailPagePreview() {
    val context = LocalContext.current
    SettingsDetailPage(
        params = SettingsDetailParams(
            currentPage = SettingsPage.Appearance,
            imageCacheSizeBytes = 0L,
            storageStats = EmptyStorageStats,
            sizeRedTotal = 0L,
            sizeRedDownload = 0L,
            onClearImageCache = {},
            onClearDownload = {},
            data = SettingsDataHolders(),
            context = context,
            onBackupDataChanged = {}
        )
    )
}
