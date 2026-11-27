package com.client.xvideos.common.ui.pagination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

@Composable
internal fun PageNumberInputButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hint = "Ввести номер страницы"
    HintTooltip(text = hint) {
        Box(
            modifier = modifier
                .padding(horizontal = 0.5.dp)
                .size(48.dp)
                .background(Color(0xFF2C2C2C))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = hint,
                tint = Color(0xFFFF9900),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PageNumberInputButtonPreview() {
    PageNumberInputButton(onClick = {})
}
