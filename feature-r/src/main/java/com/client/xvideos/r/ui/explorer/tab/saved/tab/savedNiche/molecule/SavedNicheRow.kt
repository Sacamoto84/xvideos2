package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.NichesInfo

@Composable
fun SavedNicheRow(
    item: NichesInfo,
    onClick: (NichesInfo) -> Unit,
    onDeleteClick: (NichesInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val onRowClick = remember(item, onClick) { { onClick(item) } }
    val onRowDelete = remember(item, onDeleteClick) { { onDeleteClick(item) } }
    val rowShape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .padding(vertical = 2.dp, horizontal = 6.dp)
            .fillMaxWidth()
            .clip(rowShape)
            .background(Theme.tabLevel3)
            .clickable(onClick = onRowClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        UrlImage(item.thumbnail, modifier = Modifier.size(96.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            item.name,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = Theme.R.fontFamilyDMsanss,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .width(96.dp)
                .height(48.dp)
                .clip(rowShape)
                .border(1.dp, Color.White, rowShape)
                .background(Color.Black)
                .clickable(onClick = onRowDelete),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Выйти",
                fontFamily = Theme.R.fontFamilyDMsanss,
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Preview
@Composable
private fun SavedNicheRowPreview() {
    SavedNicheRow(
        item = NichesInfo(id = "1", name = "Sample Niche", thumbnail = "https://example.com/thumb.jpg"),
        onClick = {},
        onDeleteClick = {}
    )
}
