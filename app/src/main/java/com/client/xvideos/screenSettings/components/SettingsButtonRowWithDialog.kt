package com.client.xvideos.screenSettings.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.screenSettings.DialogButton
import com.client.xvideos.screenSettings.molecule.SettingsButtonRow

/**
 * [SettingsButtonRow], нажатие которой сначала спрашивает подтверждение.
 */
@Composable
fun SettingsButtonRowWithDialog(
    @DrawableRes icon: Int = 0,
    text: String,
    value: String,
    textDialogTitle: String,
    textDialogBody: String,
    textDialogButton: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val onDismiss = remember { { visible = false } }
    val onOpen = remember { { visible = true } }

    DialogButton(
        visible = visible,
        title = textDialogTitle,
        body = textDialogBody,
        buttonText = textDialogButton,
        onDismiss = onDismiss,
        onBlockConfirmed = onClick
    )

    SettingsButtonRow(
        icon = icon,
        text = text,
        value = value,
        subtitle = subtitle,
        color = SettingsDestructiveColor,
        onClick = onOpen
    )
}

@Preview
@Composable
private fun SettingsButtonRowWithDialogPreview() {
    SettingsButtonRowWithDialog(
        icon = R.drawable.icon_red,
        text = "Очистить",
        value = "Сброс",
        textDialogTitle = "Подтвердить",
        textDialogBody = "Очистить кеш?",
        textDialogButton = "Очистить",
        onClick = {}
    )
}
