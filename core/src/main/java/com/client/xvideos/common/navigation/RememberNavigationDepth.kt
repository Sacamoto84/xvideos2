package com.client.xvideos.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors

/**
 * Глубина навигации прямо из Hilt-графа.
 *
 * Раньше разделы доставали её через `LocalRootScreenModel.current.depthState`,
 * то есть знали корневой экран приложения. Через граф получается тот же
 * singleton, но знать про точку сборки уже не нужно — и разделы можно выносить
 * в отдельные модули.
 */
@Composable
fun rememberNavigationDepth(): NavigationDepthState {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, NavigationDepthEntryPoint::class.java)
            .navigationDepthState()
    }
}
