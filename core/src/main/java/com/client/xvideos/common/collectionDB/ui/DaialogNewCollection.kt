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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.IncognitoKeyboard


@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun DailogNewCollectionPreview() {
    DaialogNewCollection(
        visible = true,
        onDismiss = {},
        onBlockConfirmed = { println(it) }
    )
}

@Composable
fun DaialogNewCollection(
    visible: Boolean,
    onDismiss: () -> Unit,
    onBlockConfirmed: (String) -> Unit,
) {

    if (!visible) return               // короче читается

    var text by remember(visible) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val onTextChange: (String) -> Unit = remember {
        { text = it }
    }

    val onConfirmAction: () -> Unit = remember(text, onBlockConfirmed, onDismiss) {
        {
            val trimmed = text.trim()
            if (trimmed.isNotEmpty()) {
                onBlockConfirmed(trimmed)
                onDismiss()
            }
        }
    }

    LavenderDialog(
        title = "Создать коллекцию",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = IncognitoKeyboard.options(),
                label = { Text("Название коллекции") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Theme.DialogLavande.buttonBackground,
                    unfocusedBorderColor = Color(0x66FFFFFF),
                    focusedLabelColor = Theme.DialogLavande.dismissTextColor,
                    unfocusedLabelColor = Theme.DialogLavande.bodyColor,
                ),
            )
        },
        confirmText = "Создать",
        confirmEnabled = text.isNotBlank(),
        onConfirm = onConfirmAction,
    )
}
