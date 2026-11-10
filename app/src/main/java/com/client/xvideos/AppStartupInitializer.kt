package com.client.xvideos

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicBoolean
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
) {
    private val initialized = AtomicBoolean(false)

    suspend fun init() = coroutineScope {
        if (!initialized.compareAndSet(false, true)) return@coroutineScope
        val savedRedInstance = savedRed.get()
        val blockRedInstance = blockRed.get()
        val dbInstance = db.get()

        dbInstance.clearVolatileCachesOnProcessStart()

        val jobs = listOf(
            async { blockRedInstance.refresh() },
            async { savedRedInstance.likes.refresh() },
            async { savedRedInstance.niches.refresh() },
            async { savedRedInstance.creators.refresh() },
            async { savedRedInstance.collections.refreshCollectionList() },
        )

        jobs.awaitAll()
    }
}
