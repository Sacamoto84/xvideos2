package com.client.xvideos

import android.app.Application
import android.content.Context
import com.client.xvideos.common.util.defaultSharedPreferences
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.traficStatistic.NetworkTrafficMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * Главный класс приложения.
 *
 * Отвечает за глобальную инициализацию: Hilt, Timber, Coil ImageLoader,
 * мониторинг сетевого трафика, настройки приложения и фоновые подписки
 * на общие события из `EventBus`.
 */
@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {

    //val ksafe = KSafe(applicationContext, lazyLoad = true)

    /**
     * Возвращает общий Coil `ImageLoader`, который используется всеми экранами.
     *
     * Фабрика вынесена отдельно, чтобы кэш, interceptors и прогресс загрузки
     * картинок настраивались в одном месте.
     */
    override fun newImageLoader(context: Context): ImageLoader {
        return CoilImageLoaderFactory.getImageLoader(this)
    }

    // Сохраняем оригинальный обработчик
    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    lateinit var networkTrafficMonitor: NetworkTrafficMonitor
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Основная точка старта процесса приложения.
     *
     * Последовательно:
     * 1. сохраняет singleton-ссылку на `Application`;
     * 2. подключает Timber в debug-сборке;
     * 3. запускает монитор сетевого трафика;
     * 4. применяет SSL-совместимость для старых Android;
     * 5. инициализирует настройки из `SharedPreferences`;
     * 6. подписывается на события логирования из общего event bus.
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        super.onCreate()

        instance = this
        // Строго первым делом: Hilt-синглтоны читают пути прямо в конструкторе.
        AppPath.init(this)

        if (BuildConfig.DEBUG) Timber.plant(DebugTree())

        // Инициализируем монитор трафика
        networkTrafficMonitor = NetworkTrafficMonitor()
        networkTrafficMonitor.startMonitoring()

        // Сохраняем оригинальный обработчик ПЕРЕД установкой нашего
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Устанавливаем наш обработчик исключений
//        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
//            try {
//                Timber.e(throwable, "🚨 UNCAUGHT EXCEPTION in thread: ${thread.name}")
//
//                // Логируем код нашего приложения
//                val ourCodeElements = throwable.stackTrace
//                    .filter { it.className.contains("com.client.xvideos") }
//
//                if (ourCodeElements.isNotEmpty()) {
//                    Timber.e("📍 Your code locations:")
//                    ourCodeElements.forEach { element ->
//                        Timber.e("   ${element.className}.${element.methodName}:${element.lineNumber}")
//                    }
//                } else {
//                    Timber.e("📍 No code from our app found in stack trace")
//                }
//
//                // Логируем suppressed exceptions
//                throwable.suppressedExceptions.forEach { suppressed ->
//                    Timber.e(suppressed, "🔗 Suppressed exception:")
//                }
//
//                // Логируем цепочку причин
//                var cause = throwable.cause
//                var level = 1
//                while (cause != null) {
//                    Timber.e(cause, "🔗 Caused by (level $level):")
//
//                    // Ищем наш код в причине
//                    cause.stackTrace
//                        .filter { it.className.contains("com.client.xvideos") }
//                        .forEach { element ->
//                            Timber.e("   📍 In cause: ${element.className}.${element.methodName}:${element.lineNumber}")
//                        }
//
//                    cause = cause.cause
//                    level++
//
//                    // Защита от бесконечных циклов
//                    if (level > 10) break
//                }
//
//            } catch (loggingException: Exception) {
//                // Если логирование падает, выводим в System.err
//                System.err.println("Failed to log exception: $loggingException")
//                loggingException.printStackTrace()
//            } finally {
//                // Всегда вызываем оригинальный обработчик
//                originalHandler?.uncaughtException(thread, throwable)
//            }
//        }

        // Настроить SLF4J для использования Timber
        // Настроить SLF4J для использования Timber
        //System.setProperty("slf4j.provider", "com.arcao.slf4j.timber.TimberLoggerProvider")

        // Совместимость со старыми корневыми сертификатами обеспечивается через
        // res/xml/network_security_config.xml (доверие к ISRG Root X1), а НЕ через
        // глобальное отключение проверки TLS. Прежний trust-all код удалён.

        val prefs = defaultSharedPreferences()
        // Контекст нужен, чтобы Settings открыл зашифрованное хранилище для
        // учётных данных Luscious и перенёс туда старые открытые значения.
        Settings.init(prefs, this)

//        val loggingInterceptor = Interceptor { chain ->
//            val request = chain.request()
//            val startTime = System.currentTimeMillis()
//
//            // Получаем информацию о том, откуда вызван запрос
//            val callerInfo = Thread.currentThread().stackTrace
//                .drop(2) // пропускаем первые системные вызовы
//                .firstOrNull { it.className.contains("com.client.xvideos") }
//                ?.let { "${it.className}.${it.methodName}:${it.lineNumber}" }
//                ?: "Unknown caller"
//
//            Log.d("OkHttp", "🌐 REQUEST: ${request.method} ${request.url}")
//            Log.d("OkHttp", "📱 Called from: $callerInfo")
//            Log.d("OkHttp", "📋 Headers: ${request.headers}")
//
//            try {
//                val response = chain.proceed(request)
//                val endTime = System.currentTimeMillis()
//                val duration = endTime - startTime
//
//                Log.d("OkHttp", "✅ RESPONSE: ${response.code} ${response.message} (${duration}ms)")
//                response
//
//            } catch (e: Exception) {
//                val endTime = System.currentTimeMillis()
//                val duration = endTime - startTime
//
//                Log.e("OkHttp", "❌ REQUEST FAILED after ${duration}ms")
//                Log.e("OkHttp", "📱 Called from: $callerInfo")
//                Log.e("OkHttp", "🔍 URL: ${request.url}")
//                Log.e("OkHttp", "💥 Exception: ${e.javaClass.simpleName}: ${e.message}")
//
//                throw e
//            }
//        }

        // Enable only for debug flavor to avoid perf regressions in release
        //Composer.setDiagnosticStackTraceEnabled(BuildConfig.DEBUG)


        // Подписка-заглушка отключена (#14): тело обработки Event.Log полностью
        // закомментировано, поэтому сам сбор событий пока бесполезен. Вернуть,
        // когда определимся с логированием (например, записью лога в файл).
//        scope.launch {
//            EventBus.events.collect { event ->
//                if (event is Event.Log) {
//                    //saveLogToFile(event.message)
//                }
//            }
//        }

    }


    /**
     * Освобождает глобальные ресурсы при завершении процесса приложения.
     *
     * На реальных устройствах вызывается редко, но полезен для корректной
     * остановки `NetworkTrafficMonitor` в тестах и эмуляторных сценариях.
     */
    override fun onTerminate() {
        super.onTerminate()
        networkTrafficMonitor.destroy()
    }

    companion object {
        /**
         * Singleton-доступ к `Application` там, где пока нет DI-контекста.
         *
         * Использовать осторожно: для новых зависимостей предпочтительнее Hilt,
         * чтобы не разносить глобальное состояние по коду.
         */
        lateinit var instance: App
            private set
    }

}

