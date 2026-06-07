package com.client.xvideos.l.ui.screens.explorer

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.common.collectionDB.ui.DaialogNewCollection
import com.client.xvideos.common.coil.UrlImage
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

    Dialog(
        onDismissRequest = { savedL.collection.visibleDialog = false }
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 280.dp, max = 560.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF3F3F3F), RoundedCornerShape(12.dp))
                .background(Color(0xFF090909))
        ) {
            Text(
                text = if (savedL.collection.collectionItemsPendingAdd.size > 1) {
                    "Добавить в коллекцию (${savedL.collection.collectionItemsPendingAdd.size})"
                } else {
                    "Добавить в коллекцию"
                },
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                LazyColumn(state = rememberLazyListState()) {
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
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(25))
                                        .size(72.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(25))
                                        .size(72.dp)
                                        .background(Color.Gray)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                collectionItem.collection,
                                color = Color.White,
                                fontFamily = Theme.L.fontFamilyDMsanss
                            )
                        }
                    }
                }
            }

            com.composables.core.HorizontalSeparator(Color(0xFF363636))

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF232323))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { savedL.collection.visibleDialog = false }) {
                    Text("Отмена", color = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        savedL.collection.visibleDialog = false
                        savedL.collection.visibleDialogCreateNew = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Theme.L.primaryColor)
                ) {
                    Text(text = "Создать", color = Color.Black)
                }
            }
        }
    }
}
