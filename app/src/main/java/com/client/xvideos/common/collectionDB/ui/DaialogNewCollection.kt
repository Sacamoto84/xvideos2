package com.client.xvideos.common.collectionDB.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme


@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun DailogNewCollectionPreview() {
    DaialogNewCollection(
        visible = true,
        onDismiss = {},
        onBlockConfirmed = { it -> println(it) }
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

    LavenderDialog(
        title = "Создать коллекцию",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                label = { Text("Название коллекции") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Theme.L.DialogLavande.buttonBackground,
                    unfocusedTextColor = Theme.L.DialogLavande.buttonBackground,
                    cursorColor = Theme.L.DialogLavande.buttonBackground,
                    focusedBorderColor = Theme.L.DialogLavande.buttonBackground,
                    unfocusedBorderColor = Theme.L.DialogLavande.buttonBackground,
                    focusedLabelColor = Theme.L.DialogLavande.buttonBackground,
                    unfocusedLabelColor = Theme.L.DialogLavande.buttonBackground,
                ),
            )
        },
        confirmText = "Создать",
        onConfirm = {
            onBlockConfirmed(text)
            onDismiss()
        },
    )
}
