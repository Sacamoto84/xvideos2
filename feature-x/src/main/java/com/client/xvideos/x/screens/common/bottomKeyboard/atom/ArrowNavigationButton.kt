package com.client.xvideos.x.screens.common.bottomKeyboard.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Кнопка-стрелка навигации по страницам («<» или «>»).
 */
@Composable
fun ArrowNavigationButton(
    arrow: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 0.5.dp)
            .size(48.dp)
            .background(if (!enabled) Color(0xFF2C2C2C) else Color(0xFFFF9000))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = arrow,
            color = if (!enabled) Color.DarkGray else Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun ArrowNavigationButtonPreview() {
    ArrowNavigationButton(
        arrow = "<",
        enabled = true,
        onClick = {}
    )
}
