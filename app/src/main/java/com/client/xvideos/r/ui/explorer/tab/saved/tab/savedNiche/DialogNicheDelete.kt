package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text

@Composable
fun DialogNicheDelete(
    item: NichesInfo?,
    onDismiss: () -> Unit,
    onConfirm: (NichesInfo) -> Unit
) {
    item?.let { pending ->
        AlertDialog(
            icon = { UrlImage(pending.thumbnail, modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .size(96.dp)) },
            onDismissRequest = onDismiss,
            title = { Text("Удалить группу?", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Text(buildAnnotatedString {
                    append("Удалить «")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                    append("» из сохранённых?")
                }, fontSize = 16.sp)
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(pending) }) {
                    Text("Удалить", fontSize = 16.sp, color = Color(0xFF6552A5))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", fontSize = 16.sp, color = Color(0xFF6552A5))
                }
            },
            containerColor = Color(0xFFEBE6EE)
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