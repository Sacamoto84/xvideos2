package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme

@Composable
fun CollectionRenameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var renameValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    val onValueChange: (String) -> Unit = remember { { text -> renameValue = text } }
    val onConfirmClick: () -> Unit = remember(onConfirm) { { onConfirm(renameValue) } }

    LavenderDialog(
        title = "Переименовать коллекцию",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = renameValue,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = modifier.fillMaxWidth(),
                label = { Text("Название коллекции") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Theme.DialogLavande.buttonBackground,
                    unfocusedBorderColor = Color(0x66FFFFFF),
                    focusedLabelColor = Theme.DialogLavande.dismissTextColor,
                    unfocusedLabelColor = Theme.DialogLavande.bodyColor,
                )
            )
        },
        confirmText = "Сохранить",
        onConfirm = onConfirmClick
    )
}

@Preview
@Composable
private fun CollectionRenameDialogPreview() {
    CollectionRenameDialog(
        initialValue = "Old Name",
        onDismiss = {},
        onConfirm = {}
    )
}
