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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.l.model.enum.SelectIndex
import com.client.xvideos.ui.theme.XvideosTheme



@Composable
fun ScreenLRootBottomNavigator(
    selectIndex: SelectIndex,
    onSelected: (SelectIndex) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val colorSelect = Theme.L.grey2

    val onDefaultClick = remember(haptic, onSelected) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onSelected(SelectIndex.Default)
        }
    }
    val onMangaLongClick = remember(haptic, onSelected) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onSelected(SelectIndex.Manga)
        }
    }
    val onHentaiLongClick = remember(haptic, onSelected) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onSelected(SelectIndex.Hentai)
        }
    }
    val onPornLongClick = remember(haptic, onSelected) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onSelected(SelectIndex.Porn)
        }
    }

    val navBarBase = remember(Theme.L.grey4) {
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Theme.L.grey4)
    }
    val boxModifier = if (modifier == Modifier) navBarBase else modifier.then(navBarBase)

    val itemTextStyle = remember(Theme.L.textColor, Theme.L.fontFamilyKarla) {
        androidx.compose.ui.text.TextStyle(
            color = Theme.L.textColor,
            fontSize = 16.sp,
            fontFamily = Theme.L.fontFamilyKarla
        )
    }

    Box(
        modifier = boxModifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val itemBoxBase = Modifier
                .height(46.dp)
                .weight(1f)

            Box(
                modifier = itemBoxBase
                    .background(if (selectIndex == SelectIndex.Default) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = onDefaultClick,
                        onLongClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Theme.L.textColor)
            }
            VerticalDivider()
            Box(
                modifier = itemBoxBase
                    .background(if (selectIndex == SelectIndex.Manga) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onMangaLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Manga",
                    style = itemTextStyle
                )
            }
            VerticalDivider()
            Box(
                modifier = itemBoxBase
                    .background(if (selectIndex == SelectIndex.Hentai) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onHentaiLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Hentai",
                    style = itemTextStyle
                )
            }
            VerticalDivider()
            Box(
                modifier = itemBoxBase
                    .background(if (selectIndex == SelectIndex.Porn) colorSelect else Color.Transparent)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onPornLongClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Porn",
                    style = itemTextStyle
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenLRootBottomNavigatorPreview() {
    XvideosTheme(darkTheme = true) {
        ScreenLRootBottomNavigator(
            selectIndex = SelectIndex.Default,
            onSelected = {}
        )
    }
}
