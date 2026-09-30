package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun TagChip(
    text: String,
    select: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleTagClick = remember(text, onClick) { { onClick(text) } }
    val chipShape = RoundedCornerShape(16.dp)
    Text(
        text = text,
        color = if (select) Color.Black else Color.White,
        fontSize = 14.sp,
        fontFamily = Theme.R.fontFamilyPopinsRegular,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .height(32.dp)
            .clip(chipShape)
            .background(if (select) Theme.R.colorYellow else Color.Transparent)
            .border(1.dp, Theme.R.colorYellow, chipShape)
            .clickable(onClick = handleTagClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .wrapContentWidth()
    )
}

@Preview
@Composable
private fun TagChipPreview() {
    TagChip(
        text = "Amateur",
        select = true,
        onClick = {}
    )
}
