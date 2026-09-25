package com.client.xvideos.r.ui.video.player_row_mini.atom

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.common.util.toMinSec
import com.client.xvideos.common.util.toPrettyCount
import com.composables.core.Icon

private val TILE_SHAPE = RoundedCornerShape(8.dp)
private val SHADOW_OFFSET = 1.dp
private val PADDING_DEFAULT = 8.dp
private const val PLACEHOLDER_TEXT = "-"

private val TILE_BOX_BASE_MODIFIER = Modifier
    .fillMaxSize()
    .clip(TILE_SHAPE)

private val SHADOW_OFFSET_MODIFIER = Modifier.offset(SHADOW_OFFSET, SHADOW_OFFSET)

private val INDEX_TEXT_MODIFIER = Modifier
    .padding(start = PADDING_DEFAULT)
    .then(SHADOW_OFFSET_MODIFIER)

private val BOTTOM_ROW_BASE_MODIFIER = Modifier.fillMaxWidth()

private val VIEWS_ROW_MODIFIER = Modifier.padding(PADDING_DEFAULT)
private val VIEWS_TEXT_MODIFIER = Modifier.padding(start = PADDING_DEFAULT)
private val DURATION_TEXT_MODIFIER = Modifier.padding(PADDING_DEFAULT)

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

    Box(modifier = modifier.then(TILE_BOX_BASE_MODIFIER)) {
        // Индекс картинки
        Text(
            indexText,
            color = Color.Gray,
            modifier = INDEX_TEXT_MODIFIER,
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )

        // Нижний ряд с лайками и длительностью
        Row(
            modifier = BOTTOM_ROW_BASE_MODIFIER.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isVisibleView) {
                Row(
                    modifier = VIEWS_ROW_MODIFIER,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShadowedIcon()
                    ShadowedText(
                        text = prettyViews,
                        modifier = VIEWS_TEXT_MODIFIER
                    )
                }
            }

            if (isVisibleDuration) {
                ShadowedText(
                    text = prettyDuration,
                    modifier = DURATION_TEXT_MODIFIER
                )
            }
        }
    }
}

@Composable
private fun ShadowedIcon(
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(R.drawable.rg_button)
    Box(modifier = modifier) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.Black,
            modifier = SHADOW_OFFSET_MODIFIER
        )
        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun ShadowedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            color = Color.Black,
            modifier = SHADOW_OFFSET_MODIFIER,
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )
        Text(
            text = text,
            color = Color.White,
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )
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
