package com.client.xvideos.common.ui.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.Settings

@Composable
internal fun rememberScrollButtonEffect(): ScrollButtonEffect {
    val effectName by Settings.scroll_buttons_effect.field.collectAsStateWithLifecycle()
    return remember(effectName) { ScrollButtonEffect.fromNameOrDefault(effectName) }
}
