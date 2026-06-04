package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.common.saved.SelectedCreator
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text

@Composable
fun DialogSubscriptionDelete(
    user: () -> SelectedCreator?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    user()?.let { pending ->
        AlertDialog(
            icon = {
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .size(96.dp).background(Color.DarkGray), contentAlignment = Alignment.Center) {


                    if (pending.urlProfile != null) {
                        UrlImage( url = pending.urlProfile )
                    }
                    else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            },
            onDismissRequest = onDismiss,
            title = { Text("Удалить подписку?", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Text(buildAnnotatedString {
                    append("Удалить автора «")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                    append("» из подписок?")
                }, fontSize = 16.sp)
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(pending.name) }) {
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
private fun DialogSubscriptionDeletePreview() {
    XvideosTheme {
        DialogSubscriptionDelete(
            user = { SelectedCreator( name = "SampleUser", true, "https://via.placeholder.com/96" ) },
            onDismiss = {},
            onConfirm = {}
        )
    }
}
