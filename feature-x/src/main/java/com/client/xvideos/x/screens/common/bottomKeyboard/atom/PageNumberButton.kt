package com.client.xvideos.x.screens.common.bottomKeyboard.atom

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Кнопка номера страницы в ленте пагинации.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageNumberButton(
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageText = remember(pageNumber) { pageNumber.toString() }
    val baseModifier = modifier
        .padding(horizontal = 0.5.dp)
        .height(48.dp)
        .background(Color(0xFF252525))
    val selectedModifier = if (isSelected) baseModifier.border(2.dp, Color(0xFFFF9900)) else baseModifier

    Box(
        modifier = selectedModifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = pageText, color = Color(0xFFCCCCCC))
    }
}

@Preview
@Composable
private fun PageNumberButtonPreview() {
    PageNumberButton(
        pageNumber = 1,
        isSelected = true,
        onClick = {},
        onLongClick = {}
    )
}
