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

private val NAV_BAR_HEIGHT = 48.dp
private val NAV_ITEM_HEIGHT = 46.dp
private val NAV_ITEM_FONT_SIZE = 16.sp

private const val CD_MENU = "Menu"
private const val TITLE_MANGA = "Manga"
private const val TITLE_HENTAI = "Hentai"
private const val TITLE_PORN = "Porn"

private val ICON_MENU = Icons.Filled.Menu
private val COLOR_TRANSPARENT = Color.Transparent
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val ROW_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val NAV_BAR_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(NAV_BAR_HEIGHT)
private val ROW_BASE_MODIFIER = Modifier.fillMaxSize()
private val NO_OP_CLICK: () -> Unit = {}

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

    Box(
        modifier = modifier
            .then(NAV_BAR_BASE_MODIFIER)
            .background(Theme.L.grey4),
    ) {
        Row(
            modifier = ROW_BASE_MODIFIER,
            verticalAlignment = ROW_VERTICAL_ALIGNMENT,
            horizontalArrangement = ROW_HORIZONTAL_ARRANGEMENT
        ) {
            Box(
                modifier = Modifier
                    .height(NAV_ITEM_HEIGHT)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Default) colorSelect else COLOR_TRANSPARENT)
                    .combinedClickable(
                        onClick = onDefaultClick,
                        onLongClick = NO_OP_CLICK
                    ),
                contentAlignment = BOX_ALIGNMENT_CENTER
            ) {
                Icon(ICON_MENU, contentDescription = CD_MENU, tint = Theme.L.textColor)
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .height(NAV_ITEM_HEIGHT)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Manga) colorSelect else COLOR_TRANSPARENT)
                    .combinedClickable(
                        onClick = NO_OP_CLICK,
                        onLongClick = onMangaLongClick
                    ),
                contentAlignment = BOX_ALIGNMENT_CENTER
            ) {
                Text(
                    TITLE_MANGA,
                    color = Theme.L.textColor,
                    fontSize = NAV_ITEM_FONT_SIZE,
                    fontFamily = Theme.L.fontFamilyKarla
                )
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .height(NAV_ITEM_HEIGHT)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Hentai) colorSelect else COLOR_TRANSPARENT)
                    .combinedClickable(
                        onClick = NO_OP_CLICK,
                        onLongClick = onHentaiLongClick
                    ),
                contentAlignment = BOX_ALIGNMENT_CENTER
            ) {
                Text(
                    TITLE_HENTAI,
                    color = Theme.L.textColor,
                    fontSize = NAV_ITEM_FONT_SIZE,
                    fontFamily = Theme.L.fontFamilyKarla
                )
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .height(NAV_ITEM_HEIGHT)
                    .weight(1f)
                    .background(if (selectIndex == SelectIndex.Porn) colorSelect else COLOR_TRANSPARENT)
                    .combinedClickable(
                        onClick = NO_OP_CLICK,
                        onLongClick = onPornLongClick
                    ),
                contentAlignment = BOX_ALIGNMENT_CENTER
            ) {
                Text(
                    TITLE_PORN,
                    color = Theme.L.textColor,
                    fontSize = NAV_ITEM_FONT_SIZE,
                    fontFamily = Theme.L.fontFamilyKarla
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
