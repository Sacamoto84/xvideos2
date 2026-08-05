package com.client.xvideos

import android.app.Application
import android.content.Context
import com.client.xvideos.common.util.defaultSharedPreferences
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.client.xvideos.common.AppBuildInfo
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.log.CrashLog
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.p2p.P2pReceiveManager
import com.client.xvideos.common.p2p.P2pSendPreparers
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.traficStatistic.NetworkTrafficMonitor
import com.client.xvideos.l.featured.saved.LSendPreparer
import com.client.xvideos.p2p.sectionBundleImporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    lateinit var networkTrafficMonitor: NetworkTrafficMonitor
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // @Volatile: присваивается на главном потоке в onCreate, читается из
    // IO-корутины MainActivity.
    @Volatile
    private var storageCleanup: Job? = null

    /**
     * Ждёт разовую уборку staging-папок, запущенную в [onCreate].
     *
     * Обязателен перед первым обращением к `AppPath.p2p_inbox`, `p2p_outbox`
     * и `l_cacheDownload`: уборка их рекурсивно удаляет и создаёт заново, и
     * работа с ними параллельно с этим потеряет файлы.
     */
    suspend fun awaitStorageCleanup() {
        storageCleanup?.join()
    }

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
        // BuildConfig генерируется на модуль, у :core он свой — поля приложения
        // базовый слой получает отсюда. Ставится до CrashLog: заголовок падения
        // печатает versionName.
        AppBuildInfo.init(debug = BuildConfig.DEBUG, versionName = BuildConfig.VERSION_NAME)

        // Строго первым делом: Hilt-синглтоны читают пути прямо в конструкторе.
        // init() только назначает пути и создаёт папки — на главном потоке это
        // дёшево. Рекурсивная чистка staging-папок уходит в фон, её результата
        // ждут через awaitStorageCleanup().
        AppPath.init(this)
        storageCleanup = scope.launch { AppPath.cleanupTransientDirs() }

        // В релизе дерево тоже сажается — иначе Timber.e становится пустышкой и
        // об ошибке у пользователя узнать неоткуда. Релизное пишет ERROR в файл
        // во внутренней памяти, без сети. Ставится после AppPath.init(): ему
        // нужен путь.
        if (BuildConfig.DEBUG) Timber.plant(DebugTree()) else Timber.plant(CrashLog.releaseTree())
        CrashLog.install()

        // Инициализируем монитор трафика
        networkTrafficMonitor = NetworkTrafficMonitor()
        NetworkTrafficMonitor.current = networkTrafficMonitor
        networkTrafficMonitor.startMonitoring()

        // Обработчик необработанных исключений ставит CrashLog.install() выше:
        // он дописывает стектрейс в журнал и передаёт управление прежнему
        // обработчику, чтобы система отработала падение как обычно.

        // Совместимость со старыми корневыми сертификатами обеспечивается через
        // res/xml/network_security_config.xml (доверие к ISRG Root X1), а НЕ через
        // глобальное отключение проверки TLS. Прежний trust-all код удалён.

        val prefs = defaultSharedPreferences()
        // Контекст нужен, чтобы Settings открыл зашифрованное хранилище для
        // учётных данных Luscious и перенёс туда старые открытые значения.
        Settings.init(prefs, this)

        // P2P знает про разделы только отсюда: базовый слой умеет передавать
        // байты, но не знает, куда их класть и как скачать несохранённый item.
        P2pReceiveManager.importerFactory = ::sectionBundleImporter
        P2pSendPreparers.l = LSendPreparer

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

