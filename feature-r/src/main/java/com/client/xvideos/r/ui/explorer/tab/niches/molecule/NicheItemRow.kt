package com.client.xvideos.r.ui.explorer.tab.niches.molecule

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.Niche

@Composable
fun NicheItemRow(
    item: Niche,
    savedRed: SavedRed,
    onNicheClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(item.id, onNicheClick) { { onNicheClick(item.id) } }
    val nicheProvider = remember(item) { { item } }
    val redProvider = remember(savedRed) { { savedRed } }
    Box(modifier = modifier) {
        NichePreview2(
            niches = nicheProvider,
            onClick = onClick,
            savedRed = redProvider,
        )
    }
}

@Preview
@Composable
private fun NicheItemRowPreview() {
    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(78.dp)
            .background(Theme.tabLevel3, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Sample Niche",
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp),
            fontFamily = Theme.R.fontFamilyDMsanss
        )
    }
}
