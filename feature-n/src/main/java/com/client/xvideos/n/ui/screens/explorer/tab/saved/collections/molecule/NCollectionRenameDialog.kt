package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.client.xvideos.common.ui.excludedFromAutofill

/**
 * Диалог переименования коллекции манги.
 */
@Composable
fun NCollectionRenameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var renameValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    val onValueChange: (String) -> Unit = remember { { text -> renameValue = text } }

    val trimmedValue by remember { derivedStateOf { renameValue.trim() } }
    val canConfirm by remember(initialValue) {
        derivedStateOf { trimmedValue.isNotEmpty() && trimmedValue != initialValue.trim() }
    }

    LavenderDialog(
        title = "Переименовать коллекцию",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = renameValue,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = modifier
                    .fillMaxWidth()
                    .excludedFromAutofill(),
                label = { Text("Название коллекции") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color(0xFFED2553),
                    unfocusedBorderColor = Color(0x66FFFFFF),
                    focusedLabelColor = Color(0xFFED2553),
                    unfocusedLabelColor = Theme.DialogLavande.bodyColor
                )
            )
        },
        confirmText = "Сохранить",
        confirmEnabled = canConfirm,
        onConfirm = { if (canConfirm) onConfirm(trimmedValue) }
    )
}

@Preview
@Composable
private fun NCollectionRenameDialogPreview() {
    NCollectionRenameDialog(
        initialValue = "Старое имя",
        onDismiss = {},
        onConfirm = {}
    )
}
