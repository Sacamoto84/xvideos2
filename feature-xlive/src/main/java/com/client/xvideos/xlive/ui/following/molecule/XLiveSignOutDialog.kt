package com.client.xvideos.xlive.ui.following.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog

/**
 * Подтверждение выхода с сайта: войти снова — это страница входа с капчей,
 * случайное нажатие дорого.
 */
@Composable
fun XLiveSignOutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    LavenderDialog(
        title = "Выйти с сайта?",
        onDismiss = onDismiss,
        body = AnnotatedString("Подписки пропадут с экрана, пока вы не войдёте снова. На сайте они останутся."),
        confirmText = "Выйти",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview
@Composable
private fun XLiveSignOutDialogPreview() {
    XLiveSignOutDialog(onDismiss = {}, onConfirm = {})
}
