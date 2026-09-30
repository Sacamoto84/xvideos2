package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.molecule

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.AlbumDetails

@Composable
fun SubscribedAlbumUnlikeDialog(
    album: AlbumDetails,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Удалить альбом с сервера?") },
        text = { Text("Удалить «${album.title}» из подписок на сервере Luscious?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview
@Composable
private fun SubscribedAlbumUnlikeDialogPreview() {
    SubscribedAlbumUnlikeDialog(
        album = AlbumDetails(id = "1", title = "Test Album"),
        onConfirm = {},
        onDismiss = {}
    )
}
