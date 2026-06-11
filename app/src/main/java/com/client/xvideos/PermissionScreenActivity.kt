package com.client.xvideos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.alexstyl.warden.PermissionState
import com.alexstyl.warden.Warden
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.launch

/**
 * Экран запроса файловых разрешений.
 *
 * Показывается перед `MainActivity`, если приложению ещё нельзя читать и писать
 * во внешнее хранилище. На Android 11+ ведёт пользователя в системный экран
 * `MANAGE_EXTERNAL_STORAGE`, на старых версиях запрашивает обычное разрешение
 * `WRITE_EXTERNAL_STORAGE`.
 */
class PermissionScreenActivity : ComponentActivity() {

    /**
     * Создаёт Compose UI проверки разрешений.
     *
     * Если разрешение уже выдано, экран сразу переводит пользователя в
     * `MainActivity`; иначе показывает кнопку запроса.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            XvideosTheme {
                var hasPermission by remember { mutableStateOf(PermissionStorage.hasPermissions(this)) }

                LaunchedEffect(hasPermission) {
                    if (hasPermission) {
                        navigateToMain()
                    }
                }

                PermissionScreenContent(
                    hasPermission = hasPermission,
                    onRequestPermission = {
                        PermissionStorage.requestPermissions(this@PermissionScreenActivity)
                    }
                )
            }
        }
    }

    /**
     * UI-состояние экрана разрешений.
     *
     * При `hasPermission = false` показывает объясняющий текст и кнопку.
     * Если разрешение есть, ничего не рисует, потому что переход в главный экран
     * выполняется через `LaunchedEffect` или `onResume()`.
     */
    @Composable
    private fun PermissionScreenContent(
        hasPermission: Boolean,
        onRequestPermission: () -> Unit
    ) {
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .semantics { testTagsAsResourceId = true },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Отсутствуют Файловые разрешения",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    color = Color(0xFFFFE800)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.padding(horizontal = 8.dp).testTag("bPermission"),
                    onClick = onRequestPermission
                ) {
                    Text(
                        text = "Запрос",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }

    /**
     * Повторно проверяет разрешения после возврата из системных настроек.
     *
     * Это основной путь для Android 11+, где пользователь выдаёт доступ
     * не внутри приложения, а на отдельном системном экране.
     */
    override fun onResume() {
        super.onResume()
        // Проверка при возврате из настроек
        if (PermissionStorage.hasPermissions(this)) {
            navigateToMain()
        }
    }

    /**
     * Закрывает экран разрешений и открывает основной экран приложения.
     */
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * Preview для проверки верстки экрана разрешений в Android Studio.
     */
    @Preview(showBackground = true)
    @Composable
    private fun PermissionScreenPreview() {
        XvideosTheme {
            PermissionScreenContent(
                hasPermission = false,
                onRequestPermission = {}
            )
        }
    }

    /**
     * Утилита для проверки и запроса файловых разрешений.
     *
     * Спрятана внутрь activity, потому что логика тесно связана с Android API
     * и нужна только на этапе допуска пользователя к основному UI.
     */
    object PermissionStorage {

        /**
         * Проверяет, может ли приложение работать с внешним хранилищем.
         *
         * На Android R+ используется `Environment.isExternalStorageManager()`,
         * потому что доступ ко всем файлам выдаётся через отдельный системный
         * режим. На старых версиях достаточно `WRITE_EXTERNAL_STORAGE`.
         */
        fun hasPermissions(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Запускает системный сценарий выдачи файлового разрешения.
         *
         * Для Android R+ открывает настройки доступа ко всем файлам конкретно
         * для текущего приложения, а при ошибке падает назад на общий экран.
         * Для старых Android запрашивает runtime permission через Warden.
         */
        fun requestPermissions(activity: ComponentActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        "package:${activity.packageName}".toUri()
                    ).apply {
                        addCategory("android.intent.category.DEFAULT")
                    }
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    activity.startActivity(intent)
                }
            } else {
                activity.lifecycleScope.launch {
                    val result = Warden.with(activity).requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                    val message = when (result) {
                        is PermissionState.Denied -> "WRITE_EXTERNAL_STORAGE Denied"
                        PermissionState.Granted -> "WRITE_EXTERNAL_STORAGE Granted"
                    }
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
