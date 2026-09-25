package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import android.content.Context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
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
private const val MSG_DISK_CACHE_ENABLED = "Дисковый кэш включен"
private const val MSG_DISK_CACHE_DISABLED = "Дисковый кэш выключен"
private const val MSG_DISK_CACHE_SIZE_PREFIX = "Размер кэша картинок: "
private const val DIALOG_BODY_PREFIX = "Размер на диске: "
private const val RAM_CACHE_STEP = 1
private const val RAM_CACHE_SUFFIX = "%"
private const val DISK_CACHE_STEP_MB = 50
private const val DISK_CACHE_SUFFIX_MB = " MB"

private const val MIN_RAM_CACHE_PERCENT = CoilImageLoaderFactory.MIN_RAM_CACHE_PERCENT
private const val MAX_RAM_CACHE_PERCENT = CoilImageLoaderFactory.MAX_RAM_CACHE_PERCENT
private const val MIN_DISK_CACHE_SIZE_MB = CoilImageLoaderFactory.MIN_DISK_CACHE_SIZE_MB
private const val MAX_DISK_CACHE_SIZE_MB = CoilImageLoaderFactory.MAX_DISK_CACHE_SIZE_MB

private const val ICON_HARD_DISK = R.drawable.hard_disk_24
private const val ICON_HARD_DRIVE = R.drawable.hard_drive_2_24
private const val ICON_MEMORY = R.drawable.memory_24

@Composable
internal fun CacheSettingsSection(
    ramCachePercent: Int,
    diskCacheEnabled: Boolean,
    diskCacheSizeMb: Int,
    imageCacheSizeBytes: Long,
    onClearImageCache: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val onRamCacheFinished: (Int) -> Unit = remember(context) {
        { value ->
            Settings.image_cache_ram_percent.setValue(value)
            CoilImageLoaderFactory.recreate(context)
            SnackBar.success("$TEXT_RAM_CACHE: $value$RAM_CACHE_SUFFIX")
        }
    }
    val onDiskCacheToggled: (Boolean) -> Unit = remember(context) {
        { enabled ->
            Settings.image_cache_disk_enabled.setValue(enabled)
            CoilImageLoaderFactory.recreate(context)
            SnackBar.success(if (enabled) MSG_DISK_CACHE_ENABLED else MSG_DISK_CACHE_DISABLED)
        }
    }
    val onDiskCacheLimitFinished: (Int) -> Unit = remember(context) {
        { value ->
            Settings.image_cache_disk_size_mb.setValue(value)
            CoilImageLoaderFactory.recreate(context)
            SnackBar.success("$MSG_DISK_CACHE_SIZE_PREFIX$value$DISK_CACHE_SUFFIX_MB")
        }
    }

    val normalizedRam = CoilImageLoaderFactory.normalizedRamCachePercent(ramCachePercent)
    val normalizedDisk = CoilImageLoaderFactory.normalizedDiskCacheSizeMb(diskCacheSizeMb)
    val formattedDiskSize = remember(imageCacheSizeBytes) {
        formatBytes(imageCacheSizeBytes)
    }
    val diskCacheSubtitle = if (diskCacheEnabled) TEXT_ENABLED else TEXT_DISABLED
    val clearDialogBody = remember(formattedDiskSize) {
        "$DIALOG_BODY_PREFIX$formattedDiskSize"
    }

    SettingsGroup(modifier = modifier) {
        IntSliderSetting(
            text = TEXT_RAM_CACHE,
            value = normalizedRam,
            min = MIN_RAM_CACHE_PERCENT,
            max = MAX_RAM_CACHE_PERCENT,
            step = RAM_CACHE_STEP,
            suffix = RAM_CACHE_SUFFIX,
            icon = ICON_MEMORY,
            onValueChangeFinished = onRamCacheFinished
        )
        SettingsDivider()

        SettingsSwitchRow(
            icon = ICON_HARD_DISK,
            text = TEXT_DISK_CACHE,
            subtitle = diskCacheSubtitle,
            value = diskCacheEnabled,
            onValueChange = onDiskCacheToggled
        )
        SettingsDivider()

        IntSliderSetting(
            text = TEXT_DISK_CACHE_LIMIT,
            value = normalizedDisk,
            min = MIN_DISK_CACHE_SIZE_MB,
            max = MAX_DISK_CACHE_SIZE_MB,
            step = DISK_CACHE_STEP_MB,
            suffix = DISK_CACHE_SUFFIX_MB,
            icon = ICON_HARD_DRIVE,
            enabled = diskCacheEnabled,
            onValueChangeFinished = onDiskCacheLimitFinished
        )
        SettingsDivider()

        SettingsValueRow(
            icon = ICON_HARD_DISK,
            text = TEXT_DISK_CACHE_ON_DISK,
            value = formattedDiskSize
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = ICON_HARD_DISK,
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
        context = context,
        modifier = Modifier
    )
}
