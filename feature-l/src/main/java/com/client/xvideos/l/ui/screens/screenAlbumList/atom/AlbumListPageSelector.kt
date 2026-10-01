package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.PageSelectorDialogContent

@Preview
@Composable
private fun AlbumListPageSelectorPreview() {
    AlbumListPageSelector(1, 199)
}

fun calculatePrevAlbumPage(page: Int): Int = (page - 1).coerceAtLeast(0)

fun calculateNextAlbumPage(page: Int, pageMax: Int): Int =
    (page + 1).coerceAtMost((pageMax - 1).coerceAtLeast(0))

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
