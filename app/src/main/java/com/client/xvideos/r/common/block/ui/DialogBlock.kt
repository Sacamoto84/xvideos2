package com.redgifs.common.block.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.ui.theme.XvideosTheme
import com.composables.core.HorizontalSeparator

@Composable
fun DialogBlock(
    visible: Boolean,
    onDismiss: () -> Unit,
    onBlockConfirmed: () -> Unit,
) {

    if (visible) {

        Dialog(
            onDismissRequest = onDismiss,
        ) {

            Box(
                modifier = Modifier
                    //.displayCutoutPadding()
                    //.systemBarsPadding()
                    .widthIn(min = 280.dp, max = 560.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE4E4E4), RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {

                Column {
                    Column(Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)) {
                        Text(
                            text = "Подтвердите блокировку",
                            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Вы уверены, что хотите заблокировать этот GIFs?",
                            style = TextStyle(color = Color(0xFF474747), fontSize = 14.sp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    HorizontalSeparator(Color(0xFFCCCCCC))

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {

                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("Отмена")
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            onClick = {
                                onBlockConfirmed()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Блокировать",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
private fun DialogBlockPreview() {
    XvideosTheme {
        DialogBlock(
            visible = true,
            onDismiss = {},
            onBlockConfirmed = {}
        )
    }
}
