package com.client.xvideos.common.di

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Область приложения живёт весь процесс, и работу на ней запускают без `try`.
 * Необработанное исключение в такой корутине не должно доходить до обработчика
 * потока: на устройстве он закрывает приложение.
 */
class ApplicationScopeTest {

    @Test
    fun `исключение в корутине области приложения не доходит до обработчика потока`() = runBlocking {
        val escaped = AtomicReference<Throwable?>(null)
        val reported = AtomicReference<Throwable?>(null)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> escaped.set(error) }
        try {
            val scope = applicationScope(reportUncaught = { reported.set(it) })

            scope.launch { error("сбой в фоновой работе") }.join()

            assertNull("исключение ушло в обработчик потока", escaped.get())
            assertEquals("сбой в фоновой работе", reported.get()?.message)
            assertEquals("область должна остаться рабочей", 42, scope.async { 42 }.await())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }

    @Test
    fun `область из модуля тоже не роняет процесс`() = runBlocking {
        val escaped = AtomicReference<Throwable?>(null)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> escaped.set(error) }
        try {
            CoroutinesModule.provideApplicationScope().launch { error("сбой") }.join()

            assertNull("исключение ушло в обработчик потока", escaped.get())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }
}
