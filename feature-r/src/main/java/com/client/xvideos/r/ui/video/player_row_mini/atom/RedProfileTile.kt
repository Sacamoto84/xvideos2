package com.client.xvideos.r.ui.video.player_row_mini.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toMinSec
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.GifsInfo

private const val PLACEHOLDER_TEXT = "-"

@Composable
fun RedProfileTile(
    item: GifsInfo,
    index: Int,
    isVisibleView: Boolean = true,
    isVisibleDuration: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val indexText = remember(index) { index.toString() }
    val prettyViews = remember(item.views) { item.views?.toPrettyCount() ?: PLACEHOLDER_TEXT }
    val prettyDuration = remember(item.duration) { item.duration?.toMinSec() ?: PLACEHOLDER_TEXT }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Индекс картинки
        Text(
            text = indexText,
            color = Color.Gray,
            modifier = Modifier
                .padding(start = 8.dp)
                .offset(1.dp, 1.dp),
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )

        // Нижний ряд с лайками и длительностью
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isVisibleView) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShadowedIcon()
                    ShadowedText(
                        text = prettyViews,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (isVisibleDuration) {
                ShadowedText(
                    text = prettyDuration,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun RedProfileTilePreview() {
    RedProfileTile(
        item = GifsInfo(
            id = "sample_id",
            views = 12500,
            duration = 15.4
        ),
        index = 1
    )
}
