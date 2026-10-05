package com.client.xvideos.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.client.xvideos.common.snackbar.SnackBar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Singleton

/**
 * Область приложения: живёт столько же, сколько процесс.
 *
 * Работу на ней запускают «выстрелил и забыл», без `try` вокруг. Без
 * обработчика любое исключение в такой корутине доходило до обработчика потока
 * и закрывало приложение — хватало одного неожиданного поля в ответе сервера.
 * Здесь оно попадает в [reportUncaught], а область остаётся рабочей.
 *
 * @param reportUncaught что делать с необработанным исключением; подменяется в тестах.
 */
internal fun applicationScope(
    reportUncaught: (Throwable) -> Unit = ::reportUncaughtInApplicationScope,
): CoroutineScope = CoroutineScope(
    SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error -> reportUncaught(error) }
)

/** Пишет сбой в лог и сообщает пользователю, что действие не выполнено. */
private fun reportUncaughtInApplicationScope(error: Throwable) {
    Timber.e(error, "Необработанное исключение в области приложения")
    SnackBar.error("Внутренняя ошибка: действие не выполнено")
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    /** Scope живёт столько же, сколько и процесс (SingletonComponent) */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = applicationScope()


    /**
    ```kotlin
        class SomeRepo @Inject constructor(
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher
        )
    ```
     */
    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

}
