package com.client.xvideos.screenSettings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
internal fun SettingsPreview(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    try { Settings.init(ctx.getSharedPreferences("preview_prefs", 0)) } catch (_: Exception) { }
    XvideosTheme { content() }
}
