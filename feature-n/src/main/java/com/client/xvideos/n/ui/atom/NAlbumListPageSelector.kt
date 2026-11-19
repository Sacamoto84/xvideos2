package com.client.xvideos.n.ui.atom

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.scroll.AlbumPageNavButton
import com.client.xvideos.n.ui.molecule.NPageSelectorDialog

fun calculatePrevNPage(page: Int): Int = (page - 1).coerceAtLeast(1)

fun calculateNextNPage(page: Int, pageMax: Int): Int =
    (page + 1).coerceAtMost(pageMax.coerceAtLeast(1))

/**
 * Атомарный переключатель страниц в стиле пагинации каталога.
 */
@Composable
fun NAlbumListPageSelector(
    page: Int,
    pageMax: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }

    val onPrev = remember(page, onChange, haptic) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onChange(calculatePrevNPage(page))
        }
    }
    val onNext = remember(page, pageMax, onChange, haptic) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onChange(calculateNextNPage(page, pageMax))
        }
    }
    val onOpenDialog = remember(haptic) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            showDialog = true
        }
    }
    val onDismissDialog = remember { { showDialog = false } }
    val onSelectFromDialog = remember(onChange) {
        { selected: Int ->
            showDialog = false
            onChange(selected)
        }
    }

    val pageText = remember(page, pageMax) { "Стр. $page из ${pageMax.coerceAtLeast(1)}" }
    val borderLineColor = Color(0xFF383838)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumPageNavButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Предыдущая страница",
            onClick = onPrev,
            enabled = page > 1,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = borderLineColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = borderLineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = stroke
                    )
                }
                .clickable(onClick = onOpenDialog),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pageText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        AlbumPageNavButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Следующая страница",
            onClick = onNext,
            enabled = page < pageMax,
            modifier = Modifier.weight(1f)
        )
    }

    if (showDialog) {
        NPageSelectorDialog(
            pageMax = pageMax.coerceAtLeast(1),
            onDismiss = onDismissDialog,
            onPageSelected = onSelectFromDialog
        )
    }
}

@Preview
@Composable
private fun NAlbumListPageSelectorPreview() {
    NAlbumListPageSelector(
        page = 1,
        pageMax = 50,
        onChange = {}
    )
}
