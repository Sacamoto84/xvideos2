package com.client.xvideos.common.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Область, живущая столько же, сколько процесс.
 *
 * Нужна там, где работа должна пережить уход экрана из композиции — например,
 * колбэк закрытия полноэкранного просмотра доскролливает список, который в этот
 * момент лежит в бэкстеке и уже не скомпонован. `rememberCoroutineScope()` там
 * не годится: его отменяют вместе с композицией.
 *
 * Диспетчер у неё IO — переключаться на главный поток вызывающий обязан сам.
 */
@Composable
fun rememberApplicationScope(): CoroutineScope {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    return remember(context, inPreview) {
        if (inPreview) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        } else {
            EntryPointAccessors
                .fromApplication(context.applicationContext, ApplicationScopeEntryPoint::class.java)
                .applicationScope()
        }
    }
}
