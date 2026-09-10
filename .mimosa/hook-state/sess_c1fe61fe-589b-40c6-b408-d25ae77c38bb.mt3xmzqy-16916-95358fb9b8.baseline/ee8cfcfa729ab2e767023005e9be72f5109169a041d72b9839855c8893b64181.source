package com.client.xvideos.l.ui.screens.albumLandingTag.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.l.model.enum.SelectIndex

@Composable
fun ScreenLRootBottomNavigator(
    selectIndex: SelectIndex,
    onSelected: (SelectIndex) -> Unit
) {

    val haptic = LocalHapticFeedback.current

    val colorSelect = Theme.L.grey2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Theme.L.grey4),
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .height(46.dp)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Default) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelected(SelectIndex.Default)
                        },
                        onLongClick = {

                        }
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Menu, contentDescription = null, tint = Theme.L.textColor)
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Manga) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {

                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelected(SelectIndex.Manga)
                        }
                    ),


                contentAlignment = Alignment.Center
            )
            {
                Text(
                    "Manga",
                    color = Theme.L.textColor,
                    fontSize = 16.sp,
                    fontFamily = Theme.L.fontFamilyKarla
                )
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Hentai) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {

                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelected(SelectIndex.Hentai)
                        }
                    ),
                contentAlignment = Alignment.Center
            )
            {
                Text(
                    "Hentai",
                    color = Theme.L.textColor,
                    fontSize = 16.sp,
                    fontFamily = Theme.L.fontFamilyKarla
                )
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Porn) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {

                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelected(SelectIndex.Porn)
                        }
                    ),
                contentAlignment = Alignment.Center
            )
            {
                Text(
                    "Porn",
                    color = Theme.L.textColor,
                    fontSize = 16.sp,
                    fontFamily = Theme.L.fontFamilyKarla
                )
            }

        }

    }
}
