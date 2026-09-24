package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.r.common.saved.SelectedCreator
import com.client.xvideos.ui.theme.XvideosTheme

private val SUBSCRIPTION_AVATAR_SHAPE = RoundedCornerShape(8.dp)
private val SUBSCRIPTION_AVATAR_PLACEHOLDER_BG = Color.DarkGray
private val AVATAR_BOX_SIZE = 96.dp
private val PERSON_ICON_SIZE = 32.dp
private const val DIALOG_TITLE = "Удалить подписку?"
private const val CONFIRM_TEXT = "Удалить"

@Composable
fun DialogSubscriptionDelete(
    user: SelectedCreator?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    user?.let { pending ->
        val handleConfirm = remember(pending.name, onConfirm) {
            { onConfirm(pending.name) }
        }
        val dialogBody = remember(pending.name) {
            buildAnnotatedString {
                append("Удалить автора «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из подписок?")
            }
        }
        val iconContent: @Composable () -> Unit = remember(pending.urlProfile) {
            {
                Box(
                    modifier = Modifier
                        .clip(SUBSCRIPTION_AVATAR_SHAPE)
                        .size(AVATAR_BOX_SIZE)
                        .background(SUBSCRIPTION_AVATAR_PLACEHOLDER_BG),
                    contentAlignment = Alignment.Center
                ) {
                    val url = pending.urlProfile
                    if (url != null) {
                        UrlImage(url = url)
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(PERSON_ICON_SIZE),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        LavenderDialog(
            title = DIALOG_TITLE,
            onDismiss = onDismiss,
            icon = iconContent,
            body = dialogBody,
            confirmText = CONFIRM_TEXT,
            onConfirm = handleConfirm,
            destructive = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogSubscriptionDeletePreview() {
    XvideosTheme {
        DialogSubscriptionDelete(
            user = SelectedCreator(name = "SampleUser", true, "https://via.placeholder.com/96"),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
