package com.client.xvideos

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.applock.AccessCodeVisualTransformation
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlin.math.ceil

// Здесь была отдельная AppLockActivity: она значилась в манифесте, но её никто
// никогда не запускал — замок рисуется прямо в MainActivity через [AppLockScreen].
// Мёртвый Activity удалён вместе с записью в AndroidManifest.xml.

@Composable
internal fun AppLockScreen(
    onUnlock: (String) -> Boolean
) {
    DisableAppLockAutofill()

    val context = LocalContext.current
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var lockoutRemainingMs by remember {
        mutableLongStateOf(AppLockRepository.lockoutRemainingMillis(context))
    }
    val focusManager = LocalFocusManager.current

    // Пока действует блокировка — обновляем обратный отсчёт раз в полсекунды.
    LaunchedEffect(lockoutRemainingMs > 0L) {
        while (AppLockRepository.lockoutRemainingMillis(context) > 0L) {
            lockoutRemainingMs = AppLockRepository.lockoutRemainingMillis(context)
            delay(500)
        }
        lockoutRemainingMs = 0L
    }

    fun submit() {
        val remainingBefore = AppLockRepository.lockoutRemainingMillis(context)
        if (remainingBefore > 0L) {
            lockoutRemainingMs = remainingBefore
            return
        }
        focusManager.clearFocus()
        val success = onUnlock(password)
        if (success) {
            AppLockRepository.resetFailedAttempts(context)
        } else {
            password = ""
            val until = AppLockRepository.registerFailedAttempt(context)
            val remaining = (until - System.currentTimeMillis()).coerceAtLeast(0L)
            lockoutRemainingMs = remaining
            errorText = if (remaining > 0L) {
                "Слишком много попыток. Подождите ${ceil(remaining / 1000.0).toInt()} с"
            } else {
                "Неверный код доступа"
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
        onSubmit = ::submit
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
    onSubmit: () -> Unit
) {
    val isLockedOut = lockoutRemainingMs > 0L
    val lockoutSeconds = ceil(lockoutRemainingMs / 1000.0).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(Color(0xFF101014))
            .imePadding()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.height(28.dp))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFFE800),
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Введите код доступа",
                color = Theme.L.textColor,
                style = Theme.L.Type.heroTitle.copy(textAlign = TextAlign.Center)
            )
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLockedOut,
                label = { Text("Код доступа") },
                isError = errorText != null || isLockedOut,
                visualTransformation = if (showPassword) VisualTransformation.None else AccessCodeVisualTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty() && !isLockedOut) onSubmit() }),
                textStyle = Theme.L.Type.body.copy(color = Color.White),
                trailingIcon = {
                    IconButton(onClick = onShowPasswordToggle) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color(0xFFFFE800)
                        )
                    }
                },
                supportingText = {
                    if (errorText != null || isLockedOut) {
                        Text(
                            text = if (isLockedOut) "Повторите через $lockoutSeconds с" else (errorText
                                ?: ""),
                            color = Color(0xFFFF7A7A)
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                enabled = password.isNotBlank() && !isLockedOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLockedOut) "Подождите $lockoutSeconds с" else "Разблокировать")
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "XVIDEOS",
                modifier = Modifier.alpha(0.26f),
                color = Color.White,
                style = Theme.L.Type.caption.copy(color = Color.White)
            )
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
