package com.client.xvideos

import android.content.Context
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.videoplayer.util.VideoDiskCacheCleaner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AppStartupInitializer @Inject constructor(
    private val storageCleanupGate: StorageCleanupGate,
    private val db: Provider<AppFileDatabase>,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val maintenanceStarted = AtomicBoolean()

    suspend fun awaitStorage() = storageCleanupGate.await()

    /** Планирует обслуживание после первого рабочего кадра, один раз на процесс. */
    fun startMaintenance() {
        if (!maintenanceStarted.compareAndSet(false, true)) return
        scope.launch {
            awaitStorage()
            Settings.migrateLegacyCredentialsIfPresent()
            AppPath.deleteTransientTrash()
            db.get().cleanupObsoleteCaches()
            VideoDiskCacheCleaner.clearLegacyCaches(context)
        }
    }
}
