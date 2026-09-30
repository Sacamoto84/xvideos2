package com.client.xvideos.screenSettings.molecule

import android.content.Context
import androidx.compose.runtime.Immutable
import com.client.xvideos.screenSettings.SettingsDataHolders
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.StorageStat

@Immutable
internal data class SettingsDetailParams(
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
