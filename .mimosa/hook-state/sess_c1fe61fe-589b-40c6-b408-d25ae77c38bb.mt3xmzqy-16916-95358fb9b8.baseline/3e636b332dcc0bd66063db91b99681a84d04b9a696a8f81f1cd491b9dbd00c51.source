package com.client.xvideos.common.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/** Доступ к [ApplicationScope] из composable вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationScopeEntryPoint {
    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

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
    return remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, ApplicationScopeEntryPoint::class.java)
            .applicationScope()
    }
}
