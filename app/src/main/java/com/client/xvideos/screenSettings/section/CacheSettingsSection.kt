package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import android.content.Context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.IntSliderSetting
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.formatBytes

private const val TEXT_RAM_CACHE = "RAM кэш картинок"
private const val TEXT_DISK_CACHE = "Дисковый кэш картинок"
private const val TEXT_DISK_CACHE_LIMIT = "Лимит кэша картинок"
private const val TEXT_DISK_CACHE_ON_DISK = "Кэш картинок на диске"
private const val TEXT_CLEAR_IMAGE_CACHE = "Очистить кэш картинок"
private const val TEXT_CLEAR = "Очистить"
private const val TEXT_ENABLED = "Включён"
private const val TEXT_DISABLED = "Выключен"
private const val DISK_CACHE_STEP_MB = 50
private const val DISK_CACHE_SUFFIX_MB = " MB"

@Composable
internal fun CacheSettingsSection(
    ramCachePercent: Int,
    diskCacheEnabled: Boolean,
    diskCacheSizeMb: Int,
    imageCacheSizeBytes: Long,
    onClearImageCache: () -> Unit,
    context: Context
) {
    val onRamCacheFinished: (Int) -> Unit = remember(context) {
        { value ->
            Settings.image_cache_ram_percent.setValue(value)
            CoilImageLoaderFactory.recreate(context)
            SnackBar.success("$TEXT_RAM_CACHE: $value%")
        }
    }
    val onDiskCacheToggled: (Boolean) -> Unit = remember(context) {
        { enabled ->
            Settings.image_cache_disk_enabled.setValue(enabled)
            CoilImageLoaderFactory.recreate(context)
            SnackBar.success(if (enabled) "Дисковый кэш включен" else "Дисковый кэш выключен")
        }
    }
    val onDiskCacheLimitFinished: (Int) -> Unit = remember(context) {
        { value ->
            Settings.image_cache_disk_size_mb.setValue(value)
            CoilImageLoaderFactory.recreate(context)
            SnackBar.success("Размер кэша картинок: $value MB")
        }
    }

    val normalizedRam = remember(ramCachePercent) {
        CoilImageLoaderFactory.normalizedRamCachePercent(ramCachePercent)
    }
    val normalizedDisk = remember(diskCacheSizeMb) {
        CoilImageLoaderFactory.normalizedDiskCacheSizeMb(diskCacheSizeMb)
    }
    val formattedDiskSize = remember(imageCacheSizeBytes) {
        formatBytes(imageCacheSizeBytes)
    }
    val diskCacheSubtitle = remember(diskCacheEnabled) {
        if (diskCacheEnabled) TEXT_ENABLED else TEXT_DISABLED
    }
    val clearDialogBody = remember(formattedDiskSize) {
        "Размер на диске: $formattedDiskSize"
    }

    SettingsGroup {
        IntSliderSetting(
            text = TEXT_RAM_CACHE,
            value = normalizedRam,
            min = CoilImageLoaderFactory.MIN_RAM_CACHE_PERCENT,
            max = CoilImageLoaderFactory.MAX_RAM_CACHE_PERCENT,
            step = 1,
            suffix = "%",
            icon = R.drawable.memory_24,
            onValueChangeFinished = onRamCacheFinished
        )
        SettingsDivider()

        SettingsSwitchRow(
            icon = R.drawable.hard_disk_24,
            text = TEXT_DISK_CACHE,
            subtitle = diskCacheSubtitle,
            value = diskCacheEnabled,
            onValueChange = onDiskCacheToggled
        )
        SettingsDivider()

        IntSliderSetting(
            text = TEXT_DISK_CACHE_LIMIT,
            value = normalizedDisk,
            min = CoilImageLoaderFactory.MIN_DISK_CACHE_SIZE_MB,
            max = CoilImageLoaderFactory.MAX_DISK_CACHE_SIZE_MB,
            step = DISK_CACHE_STEP_MB,
            suffix = DISK_CACHE_SUFFIX_MB,
            icon = R.drawable.hard_drive_2_24,
            enabled = diskCacheEnabled,
            onValueChangeFinished = onDiskCacheLimitFinished
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.hard_disk_24,
            text = TEXT_DISK_CACHE_ON_DISK,
            value = formattedDiskSize
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = R.drawable.hard_disk_24,
            text = TEXT_CLEAR_IMAGE_CACHE,
            value = TEXT_CLEAR,
            textDialogTitle = TEXT_CLEAR_IMAGE_CACHE,
            textDialogBody = clearDialogBody,
            textDialogButton = TEXT_CLEAR,
            onClick = onClearImageCache
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun CacheSettingsSectionPreview() = SettingsPreview {
    val context = LocalContext.current
    CacheSettingsSection(
        ramCachePercent = 15,
        diskCacheEnabled = true,
        diskCacheSizeMb = 512,
        imageCacheSizeBytes = 1024L * 1024L * 128L,
        onClearImageCache = {},
        context = context
    )
}
