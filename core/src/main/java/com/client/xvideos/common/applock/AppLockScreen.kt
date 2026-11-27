package com.client.xvideos.common.applock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.client.xvideos.common.applock.molecule.AppLockScreenContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Экран показывает LockActivity — отдельная Activity поверх MainActivity. Рисовать
// замок слоем внутри MainActivity нельзя: диалоги и меню — отдельные окна и
// остаются поверх такого слоя, а содержимое под ним доступно с клавиатуры.

/**
 * @param onUnlock проверка кода. `suspend`, потому что проверка — это 120 000
 *   итераций PBKDF2: раньше она считалась синхронно в колбэке и подвешивала
 *   интерфейс на сотни миллисекунд при каждой попытке. Экран показывается при
 *   каждом запуске приложения, так что это была самая заметная заморозка в нём.
 * @param secretLost замок взведён, а проверить код нечем: вместо поля ввода
 *   показывается объяснение — см. `AppLockRepository.isSecretLost`.
 */
@Composable
fun AppLockScreen(
    onUnlock: suspend (String) -> Boolean,
    secretLost: Boolean = false,
) {
    DisableAppLockAutofill()

    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var lockoutRemainingMs by remember {
        mutableLongStateOf(AppLockRepository.lockoutRemainingMillis(context))
    }
    // Пока считается хеш, повторные нажатия не должны запускать вторую проверку.
    var isChecking by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Пока действует блокировка — обновляем обратный отсчёт раз в полсекунды.
    val isLockedOut = lockoutRemainingMs > 0L
    LaunchedEffect(isLockedOut, secretLost) {
        // Поля ввода нет — ни отсчёт, ни клавиатура не нужны.
        if (secretLost) return@LaunchedEffect
        if (isLockedOut) {
            while (AppLockRepository.lockoutRemainingMillis(context) > 0L) {
                lockoutRemainingMs = AppLockRepository.lockoutRemainingMillis(context)
                delay(500)
            }
            lockoutRemainingMs = 0L
            errorText = null
        } else {
            // При открытии экрана и при завершении блокировки автоматически фокусируемся на поле ввода и открываем клавиатуру
            delay(100)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    // При возвращении в приложение из фона фокус и клавиатура также возвращаются в поле ввода,
    // а при уходе в фон введенный незавершенный пароль стирается для защиты от компрометации.
    DisposableEffect(lifecycleOwner, isLockedOut, secretLost) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !isLockedOut && !secretLost) {
                scope.launch {
                    delay(100)
                    runCatching { focusRequester.requestFocus() }
                    keyboardController?.show()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                password = ""
                showPassword = false
                errorText = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun submit() {
        if (isChecking) return
        if (password.isEmpty()) return
        val remainingBefore = AppLockRepository.lockoutRemainingMillis(context)
        if (remainingBefore > 0L) {
            lockoutRemainingMs = remainingBefore
            return
        }
        focusManager.clearFocus()
        scope.launch {
            isChecking = true
            val success = try {
                onUnlock(password)
            } finally {
                isChecking = false
            }
            if (success) {
                AppLockRepository.resetFailedAttempts(context)
            } else {
                password = ""
                val remaining = AppLockRepository.registerFailedAttempt(context)
                lockoutRemainingMs = remaining
                errorText = "Неверный код доступа"
                if (remaining <= 0L &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                ) {
                    delay(50)
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        runCatching { focusRequester.requestFocus() }
                        keyboardController?.show()
                    }
                }
            }
        }
    }

    AppLockScreenContent(
        password = password,
        onPasswordChange = {
            password = it
            errorText = null
        },
        showPassword = showPassword,
        onShowPasswordToggle = { showPassword = !showPassword },
        errorText = errorText,
        lockoutRemainingMs = lockoutRemainingMs,
        onSubmit = ::submit,
        focusRequester = focusRequester,
        secretLost = secretLost,
    )
}
