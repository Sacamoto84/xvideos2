package com.client.xvideos.common.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Immutable

@Immutable
sealed interface UiMessage {
    val text: String

    data class Info(override val text: String): UiMessage
    data class Success(override val text: String): UiMessage
    data class Error(override val text: String): UiMessage
    data class Warning(override val text: String): UiMessage
}

val UiMessage.isError: Boolean get() = this is UiMessage.Error
val UiMessage.isSuccess: Boolean get() = this is UiMessage.Success
val UiMessage.isWarning: Boolean get() = this is UiMessage.Warning
val UiMessage.isInfo: Boolean get() = this is UiMessage.Info
val UiMessage.isNotEmpty: Boolean get() = text.isNotEmpty()

@Immutable
class UiSnackbarVisuals(
    val ui: UiMessage,
    override val message: String = ui.text,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = if (ui is UiMessage.Error) SnackbarDuration.Long else SnackbarDuration.Short,
    override val withDismissAction: Boolean = false
) : SnackbarVisuals

suspend fun SnackbarHostState.show(ui: UiMessage) {
    if (ui.text.isBlank()) return
    showSnackbar(UiSnackbarVisuals(ui))
}
