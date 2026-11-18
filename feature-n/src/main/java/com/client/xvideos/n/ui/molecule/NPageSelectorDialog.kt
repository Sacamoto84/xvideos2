package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.common.ui.keyboard.KeyboardNumber
import com.client.xvideos.common.ui.keyboard.KeyboardNumberTheme

private val N_KEYBOARD_THEME = KeyboardNumberTheme(
    colorBackground = Color(0xFF222222),
    colorBorderBackground = Color(0xFF2E2E2E),
    colorText = Color.White,
    buttonColor = Color(0xFF2B2B2B),
    colorButtonBorder = Color(0xFF383838)
)

/**
 * Диалог быстрого перехода на страницу с цифровой клавиатурой.
 */
@Composable
fun NPageSelectorDialog(
    pageMax: Int,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier = modifier
                .clip(shape)
                .border(1.dp, Color(0xFF3E3E3E), shape)
                .background(Color(0xFF1E1E1E))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            KeyboardNumber(
                theme = N_KEYBOARD_THEME,
                value = -1,
                max = pageMax,
                onClick = onPageSelected
            )
        }
    }
}

@Preview
@Composable
private fun NPageSelectorDialogPreview() {
    NPageSelectorDialog(
        pageMax = 99,
        onDismiss = {},
        onPageSelected = {}
    )
}
