package com.client.xvideos.screenRoot.atom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R

private val MENU_BUTTON_SHAPE = RoundedCornerShape(16.dp)

/**
 * Универсальная кнопка выбора раздела с иконкой.
 *
 * @param iconId ресурс drawable, отображаемый внутри кнопки.
 * @param tag optional test tag для UI-тестов.
 * @param onClick callback, вызываемый при нажатии.
 */
@Composable
fun MenuButtonSelect(
    iconId: Int,
    tag: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val baseModifier = modifier
        .padding(16.dp)
        .fillMaxWidth()
        .clip(MENU_BUTTON_SHAPE)
        .border(2.dp, Color(0xFF565656), MENU_BUTTON_SHAPE)
        .background(Color(0xFF212121))
        .clickable(onClick = onClick)
        .padding(vertical = 16.dp)

    val finalModifier = if (tag.isNotEmpty()) baseModifier.testTag(tag) else baseModifier

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(iconId),
            contentDescription = null,
            modifier = Modifier.height(80.dp),
            contentScale = ContentScale.FillHeight
        )
    }
}

@Preview
@Composable
private fun MenuButtonSelectPreview() {
    MenuButtonSelect(
        iconId = R.drawable.icon_xvideos_white,
        onClick = {}
    )
}
