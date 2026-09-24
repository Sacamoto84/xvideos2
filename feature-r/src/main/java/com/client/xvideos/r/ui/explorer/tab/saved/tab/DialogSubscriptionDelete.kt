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

private val subscriptionAvatarShape = RoundedCornerShape(8.dp)
private val subscriptionAvatarPlaceholderBg = Color.DarkGray

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
        LavenderDialog(
            title = "Удалить подписку?",
            onDismiss = onDismiss,
            icon = {
                Box(
                    modifier = Modifier
                        .clip(subscriptionAvatarShape)
                        .size(96.dp)
                        .background(subscriptionAvatarPlaceholderBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (pending.urlProfile != null) {
                        UrlImage(url = pending.urlProfile)
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            },
            body = dialogBody,
            confirmText = "Удалить",
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
