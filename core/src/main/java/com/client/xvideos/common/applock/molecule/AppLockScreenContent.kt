package com.client.xvideos.common.applock.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.applock.atom.AppLockHeader
import com.client.xvideos.common.applock.atom.AppLockUnavailableNotice
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.adaptive.capOutsideCompactDp
import com.client.xvideos.common.ui.adaptive.currentWidthClass
import com.client.xvideos.ui.theme.Pink80
import com.client.xvideos.ui.theme.Purple80
import com.client.xvideos.ui.theme.PurpleGrey80
import kotlin.math.ceil

/** Наибольшая ширина формы кода на широком окне (раскрытый складной). */
private const val LOCK_FORM_MAX_WIDTH_DP = 420

private val AppLockDarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF332D41),
    tertiary = Pink80,
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

@Composable
internal fun AppLockScreenContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordToggle: () -> Unit,
    errorText: String?,
    lockoutRemainingMs: Long,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    secretLost: Boolean = false,
) {
    val isLockedOut = lockoutRemainingMs > 0L
    val lockoutSeconds = ceil(lockoutRemainingMs / 1000.0).toInt()

    val lockFormCap = capOutsideCompactDp(currentWidthClass(), LOCK_FORM_MAX_WIDTH_DP)

    MaterialTheme(colorScheme = AppLockDarkColorScheme) {
        Box(
            modifier = modifier
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
                    .verticalScroll(rememberScrollState())
                    // Только вне Compact: на широком телефоне форма прежняя.
                    .then(lockFormCap?.let { Modifier.adaptiveContentWidth(it.dp) } ?: Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (secretLost) {
                    AppLockHeader(title = "Код проверить нечем")
                    Spacer(Modifier.height(22.dp))
                    AppLockUnavailableNotice()
                } else {
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
                }
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
private fun AppLockScreenContentPreview() {
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

@Preview(showBackground = true)
@Composable
private fun AppLockScreenContentErrorPreview() {
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

@Preview(showBackground = true)
@Composable
private fun AppLockScreenContentLockedOutPreview() {
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
