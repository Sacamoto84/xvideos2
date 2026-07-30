package com.client.xvideos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.client.xvideos.PermissionScreenActivity.PermissionStorage
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Стартовая activity со splash screen.
 *
 * Держит заставку на экране, пока подготавливаются локальные данные:
 * списки сохранённого RedGifs, блокировки, коллекции и временный cache DAO.
 * После инициализации передаёт управление `MainActivity`.
 */
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var db: javax.inject.Provider<AppFileDatabase>

    @Inject
    lateinit var blockRed: javax.inject.Provider<BlockRed>

    @Inject
    lateinit var savedRed: javax.inject.Provider<SavedRed>

    private var isReady = false

    /**
     * Запускает splash screen и фоновую инициализацию приложения.
     *
     * `setKeepOnScreenCondition` привязан к `isReady`, поэтому системная заставка
     * остаётся видимой до завершения `initApp()`.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // включаем API
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // держим сплэш пока инициализация не завершена
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Запускаем инициализацию
        lifecycleScope.launch {
            // имитация тяжёлой работы
            withContext(Dispatchers.IO) {
                initApp()
            }
            isReady = true
            if (AppLockRepository.isEnabled(this@SplashActivity)) {
                AppLockSession.lock()
            }
            // Extra про необходимость замка сюда больше не кладём: MainActivity
            // сознательно определяет это сама через AppLockRepository.shouldShowLock,
            // чтобы прямой запуск Activity (напр. через adb) не обходил замок.
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    /**
     * Подготавливает данные, которые нужны сразу после старта.
     *
     * Работа выполняется только при наличии файловых разрешений. Все операции
     * запускаются параллельно через `async`, после чего `awaitAll()` гарантирует,
     * что главный экран откроется уже с обновлёнными saved/block/cache данными.
     */
    suspend fun initApp() = coroutineScope {
        if (PermissionStorage.hasPermissions(this@SplashActivity)) {

            val savedRedInstance = savedRed.get()
            val blockRedInstance = blockRed.get()
            val dbInstance = db.get()

            dbInstance.clearVolatileCachesOnProcessStart()

            val jobs = listOf(
                async { savedRedInstance.refreshTagList() },
                async { blockRedInstance.refresh() },
                async { savedRedInstance.likes.refresh() },
                async { savedRedInstance.niches.refresh() },
                async { savedRedInstance.creators.refresh() },
                async { savedRedInstance.collections.refreshCollectionList() }
            )

            // ждём все задачи
            jobs.awaitAll()
        }
    }

}
