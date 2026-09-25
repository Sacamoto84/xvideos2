package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

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
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme

private val NICHE_ICON_CORNER = 8.dp
private val NICHE_ICON_SHAPE = RoundedCornerShape(NICHE_ICON_CORNER)
private val NICHE_ICON_SIZE = 96.dp
private val NICHE_ICON_BASE_MODIFIER = Modifier
    .clip(NICHE_ICON_SHAPE)
    .size(NICHE_ICON_SIZE)
private const val DIALOG_TITLE = "Удалить группу?"
private const val CONFIRM_TEXT = "Удалить"
private const val TEXT_DELETE_NICHE_PREFIX = "Удалить «"
private const val TEXT_DELETE_NICHE_SUFFIX = "» из сохранённых?"
private val SPAN_STYLE_BOLD = SpanStyle(fontWeight = FontWeight.Bold)

@Composable
fun DialogNicheDelete(
    item: NichesInfo?,
    onDismiss: () -> Unit,
    onConfirm: (NichesInfo) -> Unit
) {
    item?.let { pending ->
        val handleConfirm = remember(pending, onConfirm) {
            { onConfirm(pending) }
        }
        val dialogBody = remember(pending.name) {
            buildAnnotatedString {
                append(TEXT_DELETE_NICHE_PREFIX)
                withStyle(SPAN_STYLE_BOLD) { append(pending.name) }
                append(TEXT_DELETE_NICHE_SUFFIX)
            }
        }
        val iconContent: @Composable () -> Unit = remember(pending.thumbnail) {
            {
                UrlImage(
                    url = pending.thumbnail,
                    modifier = NICHE_ICON_BASE_MODIFIER
                )
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
