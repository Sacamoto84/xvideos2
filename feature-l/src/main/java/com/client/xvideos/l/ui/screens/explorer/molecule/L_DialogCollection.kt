package com.client.xvideos.l.ui.screens.explorer.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.atom.LCollectionRowItem

private const val TEXT_ADD_TO_COLLECTION = "Добавить в коллекцию"

@Composable
fun L_DialogCollection(
    savedL: SavedL,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val onDismissDialog: () -> Unit = remember(savedL) { { savedL.collection.visibleDialog = false } }
    val onConfirmCreate: () -> Unit = remember(savedL) {
        {
            savedL.collection.visibleDialog = false
            savedL.collection.visibleDialogCreateNew = true
        }
    }
    val title = remember(savedL.collection.collectionItemsPendingAdd.size) {
        val pendingCount = savedL.collection.collectionItemsPendingAdd.size
        if (pendingCount > 1) {
            "$TEXT_ADD_TO_COLLECTION ($pendingCount)"
        } else {
            TEXT_ADD_TO_COLLECTION
        }
    }

    L_DialogCollectionContent(
        title = title,
        collectionList = savedL.collection.collectionList,
        onDismiss = onDismissDialog,
        onConfirmCreate = onConfirmCreate,
        onItemClick = { collectionName ->
            savedL.collection.addPendingToCollection(collectionName)
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        },
        modifier = modifier,
    )
}

@Composable
fun L_DialogCollectionContent(
    title: String,
    collectionList: List<LCollectionEntity>,
    onDismiss: () -> Unit,
    onConfirmCreate: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LavenderDialog(
        title = title,
        onDismiss = onDismiss,
        content = {
            if (collectionList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет коллекций",
                        color = Theme.DialogLavande.bodyColor,
                        fontFamily = Theme.L.fontFamilyDMsanss,
                        fontSize = 16.sp
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 420.dp)
                ) {
                    items(
                        count = collectionList.size,
                        key = { index -> "${collectionList[index].collection}#$index" },
                    ) { index ->
                        val collectionItem = collectionList[index]
                        val handleItemClick = remember(collectionItem.collection, onItemClick) {
                            {
                                onItemClick(collectionItem.collection)
                            }
                        }
                        LCollectionRowItem(
                            name = collectionItem.collection,
                            previewUrl = collectionItem.previewUrl,
                            onClick = handleItemClick
                        )
                    }
                }
            }
        },
        confirmText = "Создать",
        onConfirm = onConfirmCreate,
    )
}

@Preview
@Composable
private fun L_DialogCollectionPreview() {
    L_DialogCollectionContent(
        title = TEXT_ADD_TO_COLLECTION,
        collectionList = listOf(
            LCollectionEntity(
                collection = "Favorites",
                previewUrl = null,
                itemsCount = 5,
                lastModifiedAt = 1000L,
                duplicateCount = 0,
                hasManualCover = false
            )
        ),
        onDismiss = {},
        onConfirmCreate = {},
        onItemClick = {}
    )
}
