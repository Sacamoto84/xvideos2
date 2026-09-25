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

private val SUBSCRIPTION_AVATAR_CORNER = 8.dp
private val SUBSCRIPTION_AVATAR_SHAPE = RoundedCornerShape(SUBSCRIPTION_AVATAR_CORNER)
private val SUBSCRIPTION_AVATAR_PLACEHOLDER_BG = Color.DarkGray
private val AVATAR_BOX_SIZE = 96.dp
private val PERSON_ICON_SIZE = 32.dp
private val COLOR_WHITE = Color.White
private val PERSON_ICON_TINT = COLOR_WHITE
private val ICON_PERSON = Icons.Default.Person
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val SPAN_STYLE_BOLD = SpanStyle(fontWeight = FontWeight.Bold)
private val AVATAR_BOX_BASE_MODIFIER = Modifier
    .clip(SUBSCRIPTION_AVATAR_SHAPE)
    .size(AVATAR_BOX_SIZE)
    .background(SUBSCRIPTION_AVATAR_PLACEHOLDER_BG)
private val PERSON_ICON_MODIFIER = Modifier.size(PERSON_ICON_SIZE)
private const val DIALOG_TITLE = "Удалить подписку?"
private const val CONFIRM_TEXT = "Удалить"
private const val TEXT_DELETE_AUTHOR_PREFIX = "Удалить автора «"
private const val TEXT_DELETE_AUTHOR_SUFFIX = "» из подписок?"

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
                append(TEXT_DELETE_AUTHOR_PREFIX)
                withStyle(SPAN_STYLE_BOLD) { append(pending.name) }
                append(TEXT_DELETE_AUTHOR_SUFFIX)
            }
        }
        val iconContent: @Composable () -> Unit = remember(pending.urlProfile) {
            {
                Box(
                    modifier = AVATAR_BOX_BASE_MODIFIER,
                    contentAlignment = BOX_ALIGNMENT_CENTER
                ) {
                    val url = pending.urlProfile
                    if (url != null) {
                        UrlImage(url = url)
                    } else {
                        Icon(
                            ICON_PERSON,
                            contentDescription = null,
                            modifier = PERSON_ICON_MODIFIER,
                            tint = PERSON_ICON_TINT
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
