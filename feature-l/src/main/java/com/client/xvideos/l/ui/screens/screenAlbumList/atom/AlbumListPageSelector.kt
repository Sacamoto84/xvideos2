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

private val DEFAULT_ALBUM_KEYBOARD_THEME = KeyboardNumberTheme(
    colorBackground = Color(0xFF2D2D2D),
    colorBorderBackground = Color(0xFF282828),
    colorText = Color(0xFFFFFFFF),
    buttonColor = Color(0xFF282828),
    colorButtonBorder = Color(0xFF232323),
)

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
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
        Modifier
            .fillMaxHeight()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
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

    val rowBaseModifier = Modifier
        .fillMaxWidth()
        .height(48.dp)

    Row(
        modifier = if (modifier == Modifier) rowBaseModifier else modifier.then(rowBaseModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        AlbumPageNavButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Предыдущая страница",
            onClick = onPrevPage,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .weight(2f)
                .then(centerBoxModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                pageText,
                color = Theme.L.textColor,
                style = pageTextStyle
            )
        }

        AlbumPageNavButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Следующая страница",
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
    val navButtonBase = Modifier
        .fillMaxHeight()
        .background(Theme.L.red)
    val baseModifier = if (modifier == Modifier) navButtonBase else modifier.then(navButtonBase)
    Box(
        modifier = baseModifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            tint = Color.White,
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
    val dialogBaseModifier = Modifier
        .clip(PAGE_SELECTOR_DIALOG_SHAPE)
        .border(2.dp, Color(0xFF3E3E3E), PAGE_SELECTOR_DIALOG_SHAPE)
        .background(Color(0xFF373737))
        .padding(16.dp)
    Box(
        modifier = if (modifier == Modifier) dialogBaseModifier else modifier.then(dialogBaseModifier),
        contentAlignment = Alignment.Center
    ) {
        KeyboardNumber(
            theme = DEFAULT_ALBUM_KEYBOARD_THEME,
            value = -1,
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
