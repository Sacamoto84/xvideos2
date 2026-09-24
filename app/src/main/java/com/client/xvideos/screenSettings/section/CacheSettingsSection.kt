package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import android.content.Context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
            SnackBar.success("RAM кэш картинок: $value%")
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

    SettingsGroup {
        IntSliderSetting(
            text = "RAM кэш картинок",
            value = CoilImageLoaderFactory.normalizedRamCachePercent(ramCachePercent),
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
            text = "Дисковый кэш картинок",
            subtitle = if (diskCacheEnabled) "Включён" else "Выключен",
            value = diskCacheEnabled,
            onValueChange = onDiskCacheToggled
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
            onValueChangeFinished = onDiskCacheLimitFinished
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.hard_disk_24,
            text = "Кэш картинок на диске",
            value = formatBytes(imageCacheSizeBytes)
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = R.drawable.hard_disk_24,
            text = "Очистить кэш картинок",
            value = "Очистить",
            textDialogTitle = "Очистить кэш картинок",
            textDialogBody = "Размер на диске: ${formatBytes(imageCacheSizeBytes)}",
            textDialogButton = "Очистить",
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
