package com.client.xvideos.l.ui.screens.explorer

import androidx.activity.compose.BackHandler
import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.collectionDB.ui.DaialogNewCollection
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.l.featured.saved.SavedL

private val COLLECTION_ITEM_SHAPE = RoundedCornerShape(12.dp)
private val COLLECTION_PREVIEW_SIZE = 56.dp
private val FOLDER_ICON_SIZE = 28.dp
private val FOLDER_PLACEHOLDER_BG = Color(0xFF3D3949)
private val EMPTY_COLLECTIONS_VERTICAL_PADDING = 32.dp
private val COLLECTION_LIST_MIN_HEIGHT = 120.dp
private val COLLECTION_LIST_MAX_HEIGHT = 420.dp
private val ITEM_OUTER_PADDING = 4.dp
private val ITEM_INNER_HORIZONTAL_PADDING = 8.dp
private val ITEM_INNER_VERTICAL_PADDING = 6.dp
private val ITEM_SPACER_WIDTH = 12.dp
private val EMPTY_TEXT_FONT_SIZE = 16.sp
private val ITEM_TEXT_FONT_SIZE = 16.sp

private const val TEXT_ADD_TO_COLLECTION = "Добавить в коллекцию"
private const val TEXT_NO_COLLECTIONS = "Нет коллекций"
private const val TEXT_CREATE = "Создать"

@Composable
fun LCollectionDialogs(savedL: SavedL) {
    val isAnyDialogOpen = savedL.collection.visibleDialogCreateNew || savedL.collection.visibleDialog
    val onBack = remember(savedL) {
        {
            when (resolveLCollectionDialogBackAction(
                visibleDialogCreateNew = savedL.collection.visibleDialogCreateNew,
                visibleDialog = savedL.collection.visibleDialog
            )) {
                LCollectionDialogBackAction.DISMISS_NEW_COLLECTION -> savedL.collection.visibleDialogCreateNew = false
                LCollectionDialogBackAction.DISMISS_COLLECTION_PICKER -> savedL.collection.visibleDialog = false
                LCollectionDialogBackAction.NONE -> Unit
            }
        }
    }
    BackHandler(enabled = isAnyDialogOpen, onBack = onBack)

    if (savedL.collection.visibleDialogCreateNew) {
        val onDismissNew = remember(savedL) { { savedL.collection.visibleDialogCreateNew = false } }
        val onBlockConfirmed: (String) -> Unit = remember(savedL) {
            { collection ->
                if (collection.isNotEmpty()) {
                    savedL.collection.createCollection(collection)
                    savedL.collection.visibleDialogCreateNew = false
                }
            }
        }
        DaialogNewCollection(
            visible = savedL.collection.visibleDialogCreateNew,
            onDismiss = onDismissNew,
            onBlockConfirmed = onBlockConfirmed
        )
    }

    if (savedL.collection.visibleDialog) {
        L_DialogCollection(savedL)
    }
}

/**
 * Диалог «Добавить в коллекцию» для L-раздела.
 *
 * Показывает список существующих коллекций (с превью) и кнопку создания новой.
 * Раньше жил в `ScreenRoot.kt` — перенесён сюда, чтобы L-специфика не торчала
 * наружу из корневого экрана.
 */
@Composable
fun L_DialogCollection(savedL: SavedL) {
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

    LavenderDialog(
        title = title,
        onDismiss = onDismissDialog,
        content = {
            if (savedL.collection.collectionList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = EMPTY_COLLECTIONS_VERTICAL_PADDING),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = TEXT_NO_COLLECTIONS,
                        color = Theme.DialogLavande.bodyColor,
                        fontFamily = Theme.L.fontFamilyDMsanss,
                        fontSize = EMPTY_TEXT_FONT_SIZE
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = COLLECTION_LIST_MIN_HEIGHT, max = COLLECTION_LIST_MAX_HEIGHT)
                ) {
                    items(
                        count = savedL.collection.collectionList.size,
                        key = { index -> "${savedL.collection.collectionList[index].collection}#$index" },
                    ) { index ->
                        val collectionItem = savedL.collection.collectionList[index]
                        val handleItemClick = remember(collectionItem.collection, savedL, haptic) {
                            {
                                savedL.collection.addPendingToCollection(collectionItem.collection)
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ITEM_OUTER_PADDING)
                                .clip(COLLECTION_ITEM_SHAPE)
                                .clickable(onClick = handleItemClick)
                                .padding(horizontal = ITEM_INNER_HORIZONTAL_PADDING, vertical = ITEM_INNER_VERTICAL_PADDING),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (collectionItem.previewUrl != null) {
                                UrlImage(
                                    url = collectionItem.previewUrl,
                                    modifier = Modifier.clip(COLLECTION_ITEM_SHAPE).size(COLLECTION_PREVIEW_SIZE)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(COLLECTION_ITEM_SHAPE)
                                        .size(COLLECTION_PREVIEW_SIZE)
                                        .background(FOLDER_PLACEHOLDER_BG),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Theme.DialogLavande.dismissTextColor,
                                        modifier = Modifier.size(FOLDER_ICON_SIZE)
                                    )
                                }
                            }
                            Spacer(Modifier.width(ITEM_SPACER_WIDTH))
                            Text(
                                text = collectionItem.collection,
                                color = Color.White,
                                fontSize = ITEM_TEXT_FONT_SIZE,
                                fontWeight = FontWeight.Medium,
                                fontFamily = Theme.L.fontFamilyDMsanss
                            )
                        }
                    }
                }
            }
        },
        confirmText = TEXT_CREATE,
        onConfirm = onConfirmCreate,
    )
}

internal enum class LCollectionDialogBackAction {
    DISMISS_NEW_COLLECTION,
    DISMISS_COLLECTION_PICKER,
    NONE
}

internal fun resolveLCollectionDialogBackAction(
    visibleDialogCreateNew: Boolean,
    visibleDialog: Boolean
): LCollectionDialogBackAction = when {
    visibleDialogCreateNew -> LCollectionDialogBackAction.DISMISS_NEW_COLLECTION
    visibleDialog -> LCollectionDialogBackAction.DISMISS_COLLECTION_PICKER
    else -> LCollectionDialogBackAction.NONE
}
