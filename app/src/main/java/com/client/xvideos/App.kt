package com.client.xvideos

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.client.xvideos.common.duress.DuressWipeIsolation
import com.client.xvideos.common.util.defaultSharedPreferences
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.client.xvideos.common.AppBuildInfo
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.coil.CoilImageLoaderFactory
import com.client.xvideos.common.p2p.P2pReceiveManager
import com.client.xvideos.common.p2p.P2pSendPreparers
import com.client.xvideos.common.applock.AppLockLifecycleObserver
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.util.runCatchingCancellable
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

    /**
     * Возвращает общий Coil `ImageLoader`, который используется всеми экранами.
     *
     * Фабрика вынесена отдельно, чтобы кэш, interceptors и прогресс загрузки
     * картинок настраивались в одном месте.
     */
    override fun newImageLoader(context: Context): ImageLoader {
        return CoilImageLoaderFactory.getImageLoader(this)
    }

    private val networkTrafficMonitor: Lazy<NetworkTrafficMonitor> = lazy {
        EntryPointAccessors.fromApplication(this, NetworkTrafficMonitorEntryPoint::class.java)
            .networkTrafficMonitor()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(DelicateCoroutinesApi::class, ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        super.onCreate()

        // Процесс перезапуска после кода уничтожения не инициализирует сервисы и Hilt
        if (getProcessName().endsWith(":restart")) {
            return
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        // BuildConfig генерируется на модуль, у :core он свой — поля приложения
        // базовый слой получает отсюда.
        AppBuildInfo.init(debug = BuildConfig.DEBUG, versionName = BuildConfig.VERSION_NAME)
        // Контекст для кода в модулях, до которого не дотягивается ни DI, ни
        // Compose: класс приложения им не виден.
        AppContextHolder.init(this)

        // Если процесс был убит посреди стирания данных, доделываем изоляцию
        // строго до создания Hilt-синглтонов и AppPath.init()
        DuressWipeIsolation.ensureIsolatedBeforeStartup(this)

        // Если предыдущий запуск оборвало во время восстановления бэкапа,
        // откатываем или доводим журнал строго до AppPath.init(), Hilt и Settings.
        val storeRoot = AppPath.storeRootDir(this)
        runCatchingCancellable { XlrBackupManager.recoverInterruptedRestoreBeforeStartup(storeRoot) }
            .onFailure { Timber.e(it, "App: откат оборванного восстановления не удался") }

        // Строго первым делом: Hilt-синглтоны читают пути прямо в конструкторе.
        // init() только назначает пути и создаёт папки — на главном потоке это
        // дёшево. Временные папки отделяются в фоне до доступа к хранилищу.
        AppPath.init(this)
        // Gate берётся лениво через EntryPoint, а не через `@Inject lateinit`:
        // инъекция в Application происходит внутри super.onCreate(), то есть
        // до AppPath.init(), а Hilt-синглтоны читают пути в конструкторе.
        EntryPointAccessors
            .fromApplication(this, StorageCleanupEntryPoint::class.java)
            .storageCleanupGate()
            .start(scope) {
                // Зачищаем оставшийся временный каталог бэкапа и отложенные удаления.
                runCatchingCancellable { XlrBackupManager.recoverInterruptedRestore() }
                    .onFailure { Timber.e(it, "App: доочистка бэкапа не удалась") }
                DuressWipeIsolation.cleanPendingWipe(this@App)
                AppPath.prepareTransientDirs()
            }

        scope.launch { networkTrafficMonitor.value.startMonitoring() }

        // Совместимость со старыми корневыми сертификатами обеспечивается через
        // res/xml/network_security_config.xml (доверие к ISRG Root X1), а НЕ через
        // глобальное отключение проверки TLS. Прежний trust-all код удалён.

        val prefs = defaultSharedPreferences()
        // Обычные настройки нужны экрану замка. Профили открываются при обращении.
        Settings.init(prefs, this)
        scope.launch { AppLockRepository.warmUp(this@App) }
        AppLockLifecycleObserver.install(this)
        val mainHandler = Handler(Looper.getMainLooper())
        AppLockSession.addOnLockedListener {
            mainHandler.post { P2pReceiveManager.stop() }
        }

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
        if (networkTrafficMonitor.isInitialized()) {
            networkTrafficMonitor.value.destroy()
        }
    }

}

/** Доступ к [StorageCleanupGate] из `App.onCreate`, где инъекция ещё слишком ранняя. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageCleanupEntryPoint {
    fun storageCleanupGate(): StorageCleanupGate
}

