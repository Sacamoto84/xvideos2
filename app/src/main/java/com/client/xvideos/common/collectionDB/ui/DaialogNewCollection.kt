package com.client.xvideos.common.collectionDB.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.composables.core.HorizontalSeparator


@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun DailogNewCollectionPreview() {
    DaialogNewCollection(
        visible = true,
        onDismiss = {},
        onBlockConfirmed = {it -> println(it)}
    )
}

@Composable
fun DaialogNewCollection(
    visible: Boolean,
    onDismiss: () -> Unit,
    onBlockConfirmed: (String) -> Unit,
) {

    if (!visible) return               // короче читается

    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF3F3F3F), RoundedCornerShape(12.dp))
                .background(Color(0xFF090909))
        ) {

            Text("Создать коллекцию", color = Color.White, modifier = Modifier.padding(start = 8.dp, top = 8.dp), fontSize = 18.sp)

            /* Поле ввода названия */
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                    .fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                label = { Text("Название коллекции") },
            )

            HorizontalSeparator(Color(0xFF363636))

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF232323))
                    //.padding(vertical = 8.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton( onClick = onDismiss ) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(
                    //colors = ButtonDefaults.buttonColors(containerColor = ThemeRed.colorYellow),
                    onClick = {
                        onBlockConfirmed(text)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text( text = "Создать", color = Color.Black )
                }
            }
        }

    }

}
