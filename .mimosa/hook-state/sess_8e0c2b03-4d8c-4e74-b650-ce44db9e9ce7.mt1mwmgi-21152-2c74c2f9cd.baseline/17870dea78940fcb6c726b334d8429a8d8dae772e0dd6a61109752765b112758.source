package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import android.content.Context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.IntSliderSetting
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
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
