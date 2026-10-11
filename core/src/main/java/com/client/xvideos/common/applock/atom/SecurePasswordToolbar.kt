package com.client.xvideos.common.applock.atom

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.tooling.preview.Preview

/**
 * Оборачивает поля ввода конфиденциальных данных (паролей, PIN-кодов доступа)
 * и фильтрует контекстное текстовое меню (TextToolbar).
 *
 * Блокирует действия копирования (`onCopyRequested = null`) и вырезания (`onCutRequested = null`),
 * предотвращая утечку секрета в системный буфер обмена (Android Clipboard), сохраняя при этом
 * возможность безопасной вставки (`onPasteRequested`) и выделения (`onSelectAllRequested`).
 */
@Composable
fun SecurePasswordToolbar(
    content: @Composable () -> Unit,
) {
    val currentToolbar = LocalTextToolbar.current
    val secureToolbar = remember(currentToolbar) {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = currentToolbar.status

            override fun hide() = currentToolbar.hide()

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                currentToolbar.showMenu(
                    rect = rect,
                    onCopyRequested = null,
                    onPasteRequested = onPasteRequested,
                    onCutRequested = null,
                    onSelectAllRequested = onSelectAllRequested
                )
            }
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides secureToolbar) {
        content()
    }
}

@Preview
@Composable
private fun SecurePasswordToolbarPreview() {
    SecurePasswordToolbar {
        Text("Secure Field")
    }
}
