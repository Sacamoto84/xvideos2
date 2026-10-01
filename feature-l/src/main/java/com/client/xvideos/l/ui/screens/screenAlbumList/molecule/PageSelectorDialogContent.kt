package com.client.xvideos.l.ui.screens.screenAlbumList.molecule

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
import com.client.xvideos.common.ui.keyboard.KeyboardNumber
import com.client.xvideos.common.ui.keyboard.KeyboardNumberTheme

private val DEFAULT_ALBUM_KEYBOARD_THEME = KeyboardNumberTheme(
    colorBackground = Color(0xFF2D2D2D),
    colorBorderBackground = Color(0xFF282828),
    colorText = Color(0xFFFFFFFF),
    buttonColor = Color(0xFF282828),
    colorButtonBorder = Color(0xFF232323),
)

@Composable
fun PageSelectorDialogContent(
    pageMax: Int,
    onKeyboardNumberClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val dialogBaseModifier = Modifier
        .clip(shape)
        .border(2.dp, Color(0xFF3E3E3E), shape)
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
