package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun ButtonSeeAll(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = remember { RoundedCornerShape(8.dp) }
    Box(
        modifier = modifier
            .padding(top = 4.dp)
            .padding(horizontal = 4.dp)
            .fillMaxWidth()
            .height(32.dp)
            .clip(buttonShape)
            .border(1.dp, Theme.L.grey2, buttonShape)
            .background(Theme.L.grey3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "See All >",
            color = Theme.L.textColor,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview
@Composable
private fun ButtonSeeAllPreview() {
    ButtonSeeAll(onClick = {})
}
