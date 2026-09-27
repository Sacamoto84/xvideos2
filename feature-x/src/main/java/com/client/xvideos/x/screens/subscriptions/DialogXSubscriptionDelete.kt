package com.client.xvideos.x.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.x.model.SelectedXCreator

/**
 * Диалог подтверждения отписки от канала или актрисы/модели.
 *
 * @param creator Выбранный автор [SelectedXCreator].
 * @param onDismiss Колбэк закрытия диалога.
 * @param onConfirm Колбэк подтверждения удаления (отписки).
 */
@Composable
fun DialogXSubscriptionDelete(
    creator: SelectedXCreator?,
    onDismiss: () -> Unit,
    onConfirm: (SelectedXCreator) -> Unit,
) {
    creator?.let { pending ->
        val handleConfirm = remember(pending, onConfirm) {
            { onConfirm(pending) }
        }
        val dialogBody = remember(pending.name, pending.isModel) {
            buildAnnotatedString {
                append(if (pending.isModel) "Удалить актрису «" else "Удалить канал «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из подписок?")
            }
        }
        val iconContent: @Composable () -> Unit = remember(pending.avatarUrl, pending.isModel) {
            {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(96.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    val url = pending.avatarUrl
                    if (!url.isNullOrBlank()) {
                        UrlImage(url = url, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(
                            imageVector = if (pending.isModel) Icons.Default.Person else Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        LavenderDialog(
            title = "Удалить подписку?",
            onDismiss = onDismiss,
            icon = iconContent,
            body = dialogBody,
            confirmText = "Удалить",
            onConfirm = handleConfirm,
            destructive = true,
        )
    }
}
