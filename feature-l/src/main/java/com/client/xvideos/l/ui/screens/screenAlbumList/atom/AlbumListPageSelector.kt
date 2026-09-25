package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.common.ui.keyboard.KeyboardNumber
import com.client.xvideos.common.ui.keyboard.KeyboardNumberTheme

@Preview
@Composable
private fun Preview() {
    AlbumListPageSelector(1, 199)
}

fun calculatePrevAlbumPage(page: Int): Int = (page - 1).coerceAtLeast(0)

fun calculateNextAlbumPage(page: Int, pageMax: Int): Int =
    (page + 1).coerceAtMost((pageMax - 1).coerceAtLeast(0))

private val PAGE_SELECTOR_DIALOG_SHAPE = RoundedCornerShape(16.dp)
private val PAGE_SELECTOR_DIALOG_BORDER_COLOR = Color(0xFF3E3E3E)
private val PAGE_SELECTOR_DIALOG_BG_COLOR = Color(0xFF373737)

private val DEFAULT_ALBUM_KEYBOARD_THEME = KeyboardNumberTheme(
    colorBackground = Color(0xFF2D2D2D),
    colorBorderBackground = Color(0xFF282828),
    colorText = Color(0xFFFFFFFF),
    buttonColor = Color(0xFF282828),
    colorButtonBorder = Color(0xFF232323),
)

private val SELECTOR_HEIGHT = 48.dp
private val DIALOG_BORDER_WIDTH = 2.dp
private val DIALOG_PADDING = 16.dp
private val BORDER_LINE_WIDTH = 1.dp
private const val DEFAULT_KEYBOARD_VALUE = -1
private const val CD_PREV_PAGE = "Предыдущая страница"
private const val CD_NEXT_PAGE = "Следующая страница"

private val PREV_PAGE_ICON = Icons.AutoMirrored.Filled.KeyboardArrowLeft
private val NEXT_PAGE_ICON = Icons.AutoMirrored.Filled.KeyboardArrowRight
private val BOX_CENTER_ALIGNMENT = Alignment.Center
private val COLOR_WHITE = Color.White

private val PAGE_NAV_BUTTON_BASE_MODIFIER = Modifier
    .fillMaxHeight()
    .background(Theme.L.red)

private val PAGE_SELECTOR_DIALOG_MODIFIER = Modifier
    .clip(PAGE_SELECTOR_DIALOG_SHAPE)
    .border(DIALOG_BORDER_WIDTH, PAGE_SELECTOR_DIALOG_BORDER_COLOR, PAGE_SELECTOR_DIALOG_SHAPE)
    .background(PAGE_SELECTOR_DIALOG_BG_COLOR)
    .padding(DIALOG_PADDING)

private val SELECTOR_ROW_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(SELECTOR_HEIGHT)

private val SELECTOR_ROW_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val SELECTOR_ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val PAGE_CENTER_BOX_BASE_MODIFIER = Modifier.fillMaxHeight()

private val HAPTIC_CONFIRM = HapticFeedbackType.Confirm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListPageSelector(
    page: Int,
    pageMax: Int,
    onChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {

    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    val onPrevPage = remember(page, onChange) { { onChange(calculatePrevAlbumPage(page)) } }
    val onNextPage = remember(page, pageMax, onChange) { { onChange(calculateNextAlbumPage(page, pageMax)) } }
    val onOpenDialog = remember(haptic) {
        {
            haptic.performHapticFeedback(HAPTIC_CONFIRM)
            expanded = true
        }
    }
    val onDismissDialog = remember { { expanded = false } }

    val pageText = remember(page, pageMax) { "Page ${page + 1} of ${pageMax.coerceAtLeast(1)}" }
    val pageTextStyle = remember(Theme.L.Type.rowTitle) { Theme.L.Type.rowTitle.copy(textAlign = TextAlign.Center) }
    val onKeyboardNumberClick = remember(onChange) {
        { selectedNumber: Int ->
            onChange(selectedNumber - 1)
            expanded = false
        }
    }

    val borderLineColor = Theme.L.grey3
    val centerBoxModifier = remember(borderLineColor, onOpenDialog) {
        PAGE_CENTER_BOX_BASE_MODIFIER
            .drawBehind {
                val strokeWidth = BORDER_LINE_WIDTH.toPx()
                drawLine(
                    color = borderLineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = borderLineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .clickable(onClick = onOpenDialog)
    }

    Row(
        modifier = if (modifier == Modifier) SELECTOR_ROW_BASE_MODIFIER else modifier.then(SELECTOR_ROW_BASE_MODIFIER),
        horizontalArrangement = SELECTOR_ROW_HORIZONTAL_ARRANGEMENT,
        verticalAlignment = SELECTOR_ROW_VERTICAL_ALIGNMENT
    ) {

        AlbumPageNavButton(
            icon = PREV_PAGE_ICON,
            contentDescription = CD_PREV_PAGE,
            onClick = onPrevPage,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .weight(2f)
                .then(centerBoxModifier),
            contentAlignment = BOX_CENTER_ALIGNMENT
        ) {
            Text(
                pageText,
                color = Theme.L.textColor,
                style = pageTextStyle
            )
        }

        AlbumPageNavButton(
            icon = NEXT_PAGE_ICON,
            contentDescription = CD_NEXT_PAGE,
            onClick = onNextPage,
            modifier = Modifier.weight(1f)
        )
    }

    //-- Диалог --
    if (expanded) {
        Dialog(onDismissRequest = onDismissDialog) {
            PageSelectorDialogContent(
                pageMax = pageMax,
                onKeyboardNumberClick = onKeyboardNumberClick
            )
        }
    }
}

@Composable
private fun AlbumPageNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseModifier = if (modifier == Modifier) PAGE_NAV_BUTTON_BASE_MODIFIER else modifier.then(PAGE_NAV_BUTTON_BASE_MODIFIER)
    Box(
        modifier = baseModifier.clickable(onClick = onClick),
        contentAlignment = BOX_CENTER_ALIGNMENT
    ) {
        Icon(
            imageVector = icon,
            tint = COLOR_WHITE,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun PageSelectorDialogContent(
    pageMax: Int,
    onKeyboardNumberClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = if (modifier == Modifier) PAGE_SELECTOR_DIALOG_MODIFIER else modifier.then(PAGE_SELECTOR_DIALOG_MODIFIER),
        contentAlignment = BOX_CENTER_ALIGNMENT
    ) {
        KeyboardNumber(
            theme = DEFAULT_ALBUM_KEYBOARD_THEME,
            value = DEFAULT_KEYBOARD_VALUE,
            max = pageMax,
            onClick = onKeyboardNumberClick
        )
    }
}

@Preview
@Composable
private fun PageSelectorDialogContentPreview() {
    PageSelectorDialogContent(
        pageMax = 99,
        onKeyboardNumberClick = {}
    )
}
