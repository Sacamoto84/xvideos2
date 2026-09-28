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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.core.R
import com.client.xvideos.x.model.SelectedXCreator
import com.client.xvideos.x.model.XSubscriptionItem

/**
 * Диалог подтверждения отписки от канала или актрисы/модели.
 *
 * @param item Выбранная подписка [XSubscriptionItem].
 * @param onDismiss Колбэк закрытия диалога.
 * @param onConfirm Колбэк подтверждения удаления (отписки).
 */
@Composable
fun DialogXSubscriptionDelete(
    item: XSubscriptionItem?,
    onDismiss: () -> Unit,
    onConfirm: (XSubscriptionItem) -> Unit,
) {
    item?.let { pending ->
        val handleConfirm = remember(pending, onConfirm) {
            { onConfirm(pending) }
        }
        val dialogBody = remember(pending.displayName, pending.isModel) {
            buildAnnotatedString {
                append(if (pending.isModel) "Удалить актрису «" else "Удалить канал «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.displayName) }
                append("» из подписок?")
            }
        }
        val iconContent: @Composable () -> Unit = remember(pending.avatarUrl, pending.bannerUrl, pending.isModel) {
            {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(80.dp)
                        .background(Color(0xFF222228)),
                    contentAlignment = Alignment.Center
                ) {
                    val url = pending.avatarUrl.ifBlank { pending.bannerUrl }
                    if (url.isNotBlank()) {
                        UrlImage(url = url, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(
                            text = if (pending.isModel) "\uE9B8" else "\uE956",
                            style = TextStyle(
                                color = if (pending.isModel) Color(0xFFDE2600) else Color(0xFF1E88E5),
                                fontSize = 32.sp,
                                fontFamily = FontFamily(Font(R.font.iconfont))
                            )
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

@Composable
fun DialogXSubscriptionDelete(
    creator: SelectedXCreator?,
    onDismiss: () -> Unit,
    onConfirm: (SelectedXCreator) -> Unit,
) {
    creator?.let {
        DialogXSubscriptionDelete(
            item = it.item,
            onDismiss = onDismiss,
            onConfirm = { onConfirm(creator) }
        )
    }
}
