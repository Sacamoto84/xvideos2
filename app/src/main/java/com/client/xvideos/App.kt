package com.client.xvideos

import android.app.Application
import android.content.Context
import com.client.xvideos.common.util.defaultSharedPreferences
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.client.xvideos.common.AppBuildInfo
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.p2p.P2pReceiveManager
import com.client.xvideos.common.p2p.P2pSendPreparers
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.traficStatistic.NetworkTrafficMonitor
import com.client.xvideos.common.traficStatistic.NetworkTrafficMonitorEntryPoint
import com.client.xvideos.l.featured.saved.LSendPreparer
import com.client.xvideos.p2p.sectionBundleImporter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        // BuildConfig генерируется на модуль, у :core он свой — поля приложения
        // базовый слой получает отсюда.
        AppBuildInfo.init(debug = BuildConfig.DEBUG, versionName = BuildConfig.VERSION_NAME)
        // Контекст для кода в модулях, до которого не дотягивается ни DI, ни
        // Compose: класс приложения им не виден.
        AppContextHolder.init(this)

        // Строго первым делом: Hilt-синглтоны читают пути прямо в конструкторе.
        // init() только назначает пути и создаёт папки — на главном потоке это
        // дёшево. Рекурсивная чистка staging-папок уходит в фон, её результата
        // ждут через awaitStorageCleanup().
        AppPath.init(this)
        // Gate берётся лениво через EntryPoint, а не через `@Inject lateinit`:
        // инъекция в Application происходит внутри super.onCreate(), то есть
        // до AppPath.init(), а Hilt-синглтоны читают пути в конструкторе.
        EntryPointAccessors
            .fromApplication(this, StorageCleanupEntryPoint::class.java)
            .storageCleanupGate()
            .start(scope) { AppPath.cleanupTransientDirs() }

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        // Монитор берётся из графа — тем же EntryPoint, что и gate уборки:
        // здесь мы уже после AppPath.init(), синглтоны создавать можно.
        networkTrafficMonitor = EntryPointAccessors
            .fromApplication(this, NetworkTrafficMonitorEntryPoint::class.java)
            .networkTrafficMonitor()
        networkTrafficMonitor.startMonitoring()

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
    }


    /**
     * Освобождает глобальные ресурсы при завершении процесса приложения.
     *
     * На реальных устройствах вызывается редко, но полезен для корректной
     * остановки `NetworkTrafficMonitor` в тестах и эмуляторных сценариях.
     */
    override fun onTerminate() {
        super.onTerminate()
        // Проверка инициализации, а не голое обращение: если onCreate упал до
        // создания монитора, здесь вылетал UninitializedPropertyAccessException
        // и затирал в логе настоящую причину падения.
        if (::networkTrafficMonitor.isInitialized) {
            networkTrafficMonitor.destroy()
        }
    }

}

/** Доступ к [StorageCleanupGate] из `App.onCreate`, где инъекция ещё слишком ранняя. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageCleanupEntryPoint {
    fun storageCleanupGate(): StorageCleanupGate
}

