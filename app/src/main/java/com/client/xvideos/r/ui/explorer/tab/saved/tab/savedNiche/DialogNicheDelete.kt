package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DialogNicheDelete(
    item: NichesInfo?,
    onDismiss: () -> Unit,
    onConfirm: (NichesInfo) -> Unit
) {
    item?.let { pending ->
        LavenderDialog(
            title = "Удалить группу?",
            onDismiss = onDismiss,
            icon = { UrlImage(pending.thumbnail, modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .size(96.dp)) },
            body = buildAnnotatedString {
                append("Удалить «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из сохранённых?")
            },
            confirmText = "Удалить",
            onConfirm = { onConfirm(pending) },
            destructive = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogNicheDeletePreview() {
    XvideosTheme {
        DialogNicheDelete(
            item = NichesInfo(
                id = "id",
                name = "Sample Niche",
                thumbnail = "https://via.placeholder.com/96"
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
