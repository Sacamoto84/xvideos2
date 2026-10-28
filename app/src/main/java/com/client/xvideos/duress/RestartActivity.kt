package com.client.xvideos.duress

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import com.client.xvideos.common.rotation.RotationLockKeeper

/**
 * Вспомогательная Activity, выполняющаяся в отдельном легковесном процессе `:restart`.
 *
 * Нужна для чистого перезапуска приложения после срабатывания кода уничтожения:
 * убивает старый процесс со всеми фоновыми соединениями, экранами и кешами в памяти,
 * запускает чистую [com.client.xvideos.SplashActivity] и завершает свой процесс.
 */
class RestartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oldPid = intent.getIntExtra(EXTRA_OLD_PID, -1)
        if (oldPid != -1) {
            Process.killProcess(oldPid)
        }

        val targetIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TARGET_INTENT)
        }

        if (targetIntent != null) {
            startActivity(targetIntent)
        }

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        Process.killProcess(Process.myPid())
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)
    }

    companion object {
        const val EXTRA_OLD_PID = "extra_old_pid"
        const val EXTRA_TARGET_INTENT = "extra_target_intent"
    }
}
