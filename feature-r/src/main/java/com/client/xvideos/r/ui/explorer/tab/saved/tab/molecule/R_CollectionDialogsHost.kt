package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.CollectionCoverIcon

@Immutable
data class R_CollectionDialogData(
    val itemPendingAction: String?,
    val itemPendingRename: String?,
    val itemPendingDelete: String?,
    val renameValue: String,
)

@Immutable
data class R_CollectionDialogActions(
    val onDismissAction: () -> Unit,
    val onDismissRename: () -> Unit,
    val onDismissDelete: () -> Unit,
    val onRenameValueChange: (String) -> Unit,
    val onRenameAction: (String) -> Unit,
    val onShareAction: (String) -> Unit,
    val onDeleteAction: (String) -> Unit,
    val onConfirmRename: (String, String) -> Unit,
    val onConfirmDelete: (String) -> Unit,
)

@Composable
fun R_CollectionDialogsHost(
    dialogData: R_CollectionDialogData,
    coverOf: (String) -> String?,
    actions: R_CollectionDialogActions,
) {
    // ---------- Меню действий (long-press) ----------
    dialogData.itemPendingAction?.let { pending ->
        val onRenameClick = remember(pending, actions.onRenameAction) { { actions.onRenameAction(pending) } }
        val onShareClick = remember(pending, actions.onShareAction) { { actions.onShareAction(pending) } }
        val onDeleteClick = remember(pending, actions.onDeleteAction) { { actions.onDeleteAction(pending) } }
        val menuItemStyle = remember { Theme.L.Type.menuItem.copy(color = Color.White) }
        val iconTint = Theme.DialogLavande.buttonBackground
        LavenderDialog(
            title = "Действие с коллекцией",
            onDismiss = actions.onDismissAction,
            icon = { CollectionCoverIcon(coverOf(pending)) },
            content = {
                Text(
                    pending,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                DropdownMenuItem(
                    text = { Text("Переименовать", style = menuItemStyle) },
                    onClick = onRenameClick,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = iconTint) }
                )
                DropdownMenuItem(
                    text = { Text("Поделиться (P2P)", style = menuItemStyle) },
                    onClick = onShareClick,
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = iconTint) }
                )
                DropdownMenuItem(
                    text = { Text("Удалить коллекцию", style = menuItemStyle) },
                    onClick = onDeleteClick,
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = iconTint) }
                )
            },
        )
    }

    // ---------- Переименование ----------
    dialogData.itemPendingRename?.let { pending ->
        val onConfirm = remember(pending, dialogData.renameValue, actions.onConfirmRename) {
            { actions.onConfirmRename(pending, dialogData.renameValue) }
        }
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Theme.DialogLavande.buttonBackground,
            unfocusedBorderColor = Color(0x66FFFFFF),
            focusedLabelColor = Theme.DialogLavande.dismissTextColor,
            unfocusedLabelColor = Theme.DialogLavande.bodyColor,
        )
        LavenderDialog(
            title = "Переименовать коллекцию",
            onDismiss = actions.onDismissRename,
            icon = { CollectionCoverIcon(coverOf(pending)) },
            content = {
                OutlinedTextField(
                    value = dialogData.renameValue,
                    onValueChange = actions.onRenameValueChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название коллекции") },
                    colors = textFieldColors,
                )
            },
            confirmText = "Сохранить",
            onConfirm = onConfirm,
        )
    }

    // ---------- Удаление ----------
    dialogData.itemPendingDelete?.let { pending ->
        val onConfirm = remember(pending, actions.onConfirmDelete) {
            { actions.onConfirmDelete(pending) }
        }
        val dialogBody = remember(pending) {
            buildAnnotatedString {
                append("Удалить «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
                append("» из коллекции")
            }
        }
        LavenderDialog(
            title = "Удалить коллекцию?",
            onDismiss = actions.onDismissDelete,
            icon = { CollectionCoverIcon(coverOf(pending)) },
            body = dialogBody,
            confirmText = "Удалить",
            onConfirm = onConfirm,
            destructive = true,
        )
    }
}

@Preview
@Composable
private fun R_CollectionDialogsHostPreview() {
    R_CollectionDialogsHost(
        dialogData = R_CollectionDialogData(
            itemPendingAction = null,
            itemPendingRename = null,
            itemPendingDelete = null,
            renameValue = ""
        ),
        coverOf = { null },
        actions = R_CollectionDialogActions(
            onDismissAction = {},
            onDismissRename = {},
            onDismissDelete = {},
            onRenameValueChange = {},
            onRenameAction = {},
            onShareAction = {},
            onDeleteAction = {},
            onConfirmRename = { _, _ -> },
            onConfirmDelete = {}
        )
    )
}
