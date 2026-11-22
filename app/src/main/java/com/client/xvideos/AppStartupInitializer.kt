package com.client.xvideos

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Подготавливает данные, которые нужны сразу после старта:
 * списки сохранённого R, блокировки, коллекции и временный cache DAO.
 *
 * Выполняется один раз за время жизни процесса приложения:
 * - при обычном запуске — в [SplashActivity] под заставкой;
 * - при запуске в режиме камуфляжа («Калькулятор») — в фоне из [MainActivity.launchStartupCleanup],
 *   чтобы калькулятор открывался мгновенно и без заставки с анимацией загрузки.
 */
@Singleton
class AppStartupInitializer @Inject constructor(
    private val db: Provider<AppFileDatabase>,
    private val blockRed: Provider<BlockRed>,
    private val savedRed: Provider<SavedRed>,
    private val storageCleanupGate: StorageCleanupGate,
) {
    private val initMutex = Mutex()
    private var initialized = false

    suspend fun init() = initMutex.withLock {
        if (initialized) return@withLock
        // Хранилища могут начать читать файлы при создании.
        storageCleanupGate.await()
        val savedRedInstance = savedRed.get()
        val blockRedInstance = blockRed.get()
        db.get().clearVolatileCachesOnProcessStart()
        coroutineScope {
            launch { savedRedInstance.collections.refreshCollectionList() }
            joinAll(
                blockRedInstance.refresh(),
                savedRedInstance.likes.refresh(),
                savedRedInstance.niches.refresh(),
                savedRedInstance.creators.refresh(),
            )
        }
        initialized = true
    }
}
