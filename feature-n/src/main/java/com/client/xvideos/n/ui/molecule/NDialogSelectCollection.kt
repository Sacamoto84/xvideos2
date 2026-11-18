package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.atom.NCollectionRowItem

/**
 * Диалог выбора коллекции для добавления картинки.
 */
@Composable
fun NDialogSelectCollection(
    collections: List<CollectionEntity<NPicture>>,
    onDismiss: () -> Unit,
    onConfirmCreate: () -> Unit,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Добавить в коллекцию",
    coverUrlProvider: (String, List<NPicture>) -> String? = { _, items -> items.firstOrNull()?.thumbnailUrl }
) {
    LavenderDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = "Создать",
        onConfirm = onConfirmCreate,
        content = {
            if (collections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет созданных коллекций",
                        color = Color(0xFFAAAAAA),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 380.dp)
                ) {
                    items(
                        items = collections,
                        key = { it.collection }
                    ) { entity ->
                        val handleSelect = remember(entity.collection, onSelectCollection) {
                            { onSelectCollection(entity.collection) }
                        }
                        val thumb = remember(entity.collection, entity.items, coverUrlProvider) {
                            coverUrlProvider(entity.collection, entity.items)
                        }
                        NCollectionRowItem(
                            name = entity.collection,
                            itemsCount = entity.items.size,
                            previewUrl = thumb,
                            onClick = handleSelect
                        )
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun NDialogSelectCollectionPreview() {
    NDialogSelectCollection(
        collections = emptyList(),
        onDismiss = {},
        onConfirmCreate = {},
        onSelectCollection = {},
        coverUrlProvider = { _, _ -> null }
    )
}
