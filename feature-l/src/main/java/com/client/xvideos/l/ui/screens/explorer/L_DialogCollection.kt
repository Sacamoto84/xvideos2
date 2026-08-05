package com.client.xvideos.l.ui.screens.explorer

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.collectionDB.ui.DaialogNewCollection
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.l.featured.saved.SavedL

@Composable
fun LCollectionDialogs(savedL: SavedL) {
    if (savedL.collection.visibleDialogCreateNew) {
        DaialogNewCollection(
            visible = savedL.collection.visibleDialogCreateNew,
            onDismiss = { savedL.collection.visibleDialogCreateNew = false },
            onBlockConfirmed = { collection ->
                if (collection.isNotEmpty()) {
                    savedL.collection.createCollection(collection)
                    savedL.collection.visibleDialogCreateNew = false
                }
            }
        )
    }

    if (savedL.collection.visibleDialog) {
        L_DialogCollection(savedL = savedL)
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

    LavenderDialog(
        title = if (savedL.collection.collectionItemsPendingAdd.size > 1) {
            "Добавить в коллекцию (${savedL.collection.collectionItemsPendingAdd.size})"
        } else {
            "Добавить в коллекцию"
        },
        onDismiss = { savedL.collection.visibleDialog = false },
        content = {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)
            ) {
                items(savedL.collection.collectionList.size) { index ->
                    val collectionItem = savedL.collection.collectionList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(vertical = 4.dp)
                            .clickable(onClick = {
                                savedL.collection.addPendingToCollection(collectionItem.collection)
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (collectionItem.previewUrl != null) {
                            UrlImage(
                                url = collectionItem.previewUrl,
                                modifier = Modifier.clip(RoundedCornerShape(25)).size(72.dp)
                            )
                        } else {
                            Box(Modifier.clip(RoundedCornerShape(25)).size(72.dp).background(Color.Gray))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            collectionItem.collection,
                            color = Color.Black,
                            fontFamily = Theme.L.fontFamilyDMsanss
                        )
                    }
                }
            }
        },
        confirmText = "Создать",
        onConfirm = {
            savedL.collection.visibleDialog = false
            savedL.collection.visibleDialogCreateNew = true
        },
    )
}
