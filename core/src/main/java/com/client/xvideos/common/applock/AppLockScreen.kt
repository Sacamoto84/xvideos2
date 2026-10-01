package com.client.xvideos.common.applock

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.client.xvideos.common.applock.atom.AppLockHeader
import com.client.xvideos.common.applock.molecule.AppLockInputForm
import com.client.xvideos.ui.theme.Pink80
import com.client.xvideos.ui.theme.Purple80
import com.client.xvideos.ui.theme.PurpleGrey80
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

// Здесь была отдельная AppLockActivity: она значилась в манифесте, но её никто
// никогда не запускал — замок рисуется прямо в MainActivity через [AppLockScreen].
// Мёртвый Activity удалён вместе с записью в AndroidManifest.xml.

private val AppLockDarkColorScheme = darkColorScheme(
    primary = Purple80,               // 0xFFD0BCFF - стандартный лавандовый цвет Material 3
    onPrimary = Color(0xFF381E72),     // тёмно-фиолетовый высококонтрастный текст
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,          // 0xFFCCC2DC
    onSecondary = Color(0xFF332D41),
    tertiary = Pink80,                 // 0xFFEFB8C8
    background = Color(0xFF141218),    // глубокий тёмный фон Material 3 с мягким лавандовым отливом
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),         // стандартный цвет ошибки Material 3
    onError = Color(0xFF601410),
)

/**
 * @param onUnlock проверка кода. `suspend`, потому что проверка — это 120 000
 *   итераций PBKDF2: раньше она считалась синхронно в колбэке и подвешивала
 *   интерфейс на сотни миллисекунд при каждой попытке. Экран показывается при
 *   каждом запуске приложения, так что это была самая заметная заморозка в нём.
 */
@Composable
fun AppLockScreen(
    onUnlock: suspend (String) -> Boolean
) {
    DisableAppLockAutofill()

    val context = LocalContext.current
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
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
    LaunchedEffect(isLockedOut) {
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

    // При возвращении в приложение из фона фокус и клавиатура также возвращаются в поле ввода
    DisposableEffect(lifecycleOwner, isLockedOut) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !isLockedOut) {
                scope.launch {
                    delay(100)
                    runCatching { focusRequester.requestFocus() }
                    keyboardController?.show()
                }
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
                if (remaining <= 0L) {
                    delay(50)
                    runCatching { focusRequester.requestFocus() }
                    keyboardController?.show()
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
        focusRequester = focusRequester
    )
}


@Composable
private fun AppLockScreenContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordToggle: () -> Unit,
    errorText: String?,
    lockoutRemainingMs: Long,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val isLockedOut = lockoutRemainingMs > 0L
    val lockoutSeconds = ceil(lockoutRemainingMs / 1000.0).toInt()

    MaterialTheme(colorScheme = AppLockDarkColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .background(MaterialTheme.colorScheme.background)
                .displayCutoutPadding()
                .imePadding()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                AppLockHeader()
                Spacer(Modifier.height(22.dp))
                AppLockInputForm(
                    password = password,
                    onPasswordChange = onPasswordChange,
                    showPassword = showPassword,
                    onShowPasswordToggle = onShowPasswordToggle,
                    errorText = errorText,
                    isLockedOut = isLockedOut,
                    lockoutSeconds = lockoutSeconds,
                    onSubmit = onSubmit,
                    focusRequester = focusRequester
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "XVIDEOS",
                    modifier = Modifier.alpha(0.26f),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                    style = Theme.L.Type.caption
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockScreenPreview() {
    XvideosTheme(darkTheme = true) {
        AppLockScreenContent(
            password = "123",
            onPasswordChange = {},
            showPassword = false,
            onShowPasswordToggle = {},
            errorText = null,
            lockoutRemainingMs = 0L,
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockScreenErrorPreview() {
    XvideosTheme(darkTheme = true) {
        AppLockScreenContent(
            password = "123",
            onPasswordChange = {},
            showPassword = true,
            onShowPasswordToggle = {},
            errorText = "Неверный код доступа",
            lockoutRemainingMs = 0L,
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockScreenLockedOutPreview() {
    XvideosTheme(darkTheme = true) {
        AppLockScreenContent(
            password = "",
            onPasswordChange = {},
            showPassword = false,
            onShowPasswordToggle = {},
            errorText = null,
            lockoutRemainingMs = 45000L,
            onSubmit = {}
        )
    }
}
