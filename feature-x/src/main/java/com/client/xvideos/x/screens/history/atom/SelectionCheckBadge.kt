package com.client.xvideos.x.screens.history.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Чекбокс выбора элемента в режиме множественного выделения истории.
 */
@Composable
fun SelectionCheckBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val badgeModifier = modifier
        .size(28.dp)
        .clip(CircleShape)
        .then(
            if (isSelected) {
                Modifier
                    .background(Color(0xFFE91E63))
                    .border(width = 1.5.dp, color = Color.White, shape = CircleShape)
            } else {
                Modifier
                    .background(Color(0x99000000))
                    .border(width = 1.5.dp, color = Color.LightGray, shape = CircleShape)
            }
        )
    Box(
        modifier = badgeModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Выбрано",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SelectionCheckBadgePreview() {
    SelectionCheckBadge(isSelected = true)
}
