package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.Audience

@Composable
fun AudienceChip(
    item: Audience,
    onClick: (Audience) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val handleClick = remember(item, onClick) { { onClick(item) } }
    val borderColor = Theme.L.secondaryColor
    val chipBorderModifier = remember(borderColor) {
        Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
    }
    val chipModifier = if (modifier == Modifier) chipBorderModifier else modifier.then(chipBorderModifier)

    Text(
        text = item.title,
        modifier = chipModifier
            .clickable(onClick = handleClick)
            .padding(4.dp),
        color = Theme.L.primaryColor,
        style = textStyle
    )
}

@Preview
@Composable
private fun AudienceChipPreview() {
    AudienceChip(
        item = Audience(id = "1", title = "Audience 1", url = "url"),
        onClick = {},
        textStyle = Theme.L.Type.rowValue
    )
}
