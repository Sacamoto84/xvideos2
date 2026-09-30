package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.r.model.UserInfo

@Composable
fun DeleteCreatorDialog(
    item: UserInfo?,
    onDismiss: () -> Unit,
    onConfirm: (UserInfo) -> Unit
) {
    item?.let { pending ->
        val handleConfirm = remember(pending, onConfirm) {
            { onConfirm(pending) }
        }
        LavenderDialog(
            title = "Удалить автора?",
            onDismiss = onDismiss,
            icon = {
                pending.profileImageUrl?.let {
                    UrlImage(it, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(96.dp))
                }
            },
            body = buildAnnotatedString {
                append("Удалить «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из сохранённых?")
            },
            confirmText = "Удалить",
            onConfirm = handleConfirm,
            destructive = true,
        )
    }
}

@Preview
@Composable
private fun DeleteCreatorDialogPreview() {
    val sampleUser = UserInfo(
        name = "Sample Creator",
        username = "samplecreator",
        profileImageUrl = "https://via.placeholder.com/96",
        followers = 21_193,
        views = 32_986_108,
        publishedGifs = 2_176,
        url = "https://example.com/samplecreator"
    )
    DeleteCreatorDialog(
        item = sampleUser,
        onDismiss = {},
        onConfirm = {}
    )
}
