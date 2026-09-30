package com.client.xvideos.l.ui.screens.albumLandingTag.molecule

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.ui.screens.albumLandingTag.atom.LandingTagAlbumItem

@Composable
fun LandingTagSectionItem(
    item: Landing_page_albumSection,
    screenWidth: Dp,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (Landing_page_albumSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSeeAll = remember(item, onSeeAllClick) { { onSeeAllClick(item) } }
    val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }
    val displayAlbums = remember(item.items) { item.items.take(9) }

    Column(modifier = modifier) {
        Text(
            item.title,
            color = Theme.L.textColor,
            fontSize = 24.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
        )

        FlowRow(
            maxItemsInEachRow = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displayAlbums.forEach { album ->
                LandingTagAlbumItem(
                    album = album,
                    itemWidth = itemWidth,
                    onAlbumClick = onAlbumClick
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                .fillMaxWidth()
                .height(40.dp)
                .border(2.dp, Theme.L.grey3, RoundedCornerShape(8.dp))
                .clickable(onClick = onSeeAll),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "See All >",
                color = Theme.L.textColor,
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyKarla,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview
@Composable
private fun LandingTagSectionItemPreview() {
    LandingTagSectionItem(
        item = Landing_page_albumSection(
            title = "Hentai Pictures",
            count = 0,
            itemType = "album",
            url = "",
            items = emptyList()
        ),
        screenWidth = 360.dp,
        onAlbumClick = {},
        onSeeAllClick = {}
    )
}
