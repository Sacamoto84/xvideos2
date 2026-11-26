package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.excludedFromAutofill

@Composable
internal fun AccountCollectionNameDialog(
    initialName: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, (Boolean) -> Unit) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialName == null) "Новая коллекция R" else "Переименовать коллекцию") },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().excludedFromAutofill(),
                value = name,
                onValueChange = { name = it },
                enabled = !saving,
                singleLine = true,
                label = { Text("Название") },
            )
        },
        confirmButton = {
            TextButton(enabled = !saving && trimmed.isNotEmpty() && trimmed != initialName, onClick = {
                saving = true
                onSave(trimmed) { saved ->
                    saving = false
                    if (saved) onDismiss()
                }
            }) {
                Text(if (initialName == null) "Создать" else "Сохранить")
            }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") } },
    )
}

@Preview
@Composable
private fun AccountCollectionNameDialogPreview() {
    AccountCollectionNameDialog(initialName = "Избранное", onDismiss = {}, onSave = { _, done -> done(true) })
}
