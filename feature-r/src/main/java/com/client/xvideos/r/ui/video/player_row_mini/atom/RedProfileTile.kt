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

@Composable
fun RedProfileTile(item: GifsInfo, index: Int, isVisibleView : Boolean = true, isVisibleDuration : Boolean = true) {
    val indexText = remember(index) { index.toString() }
    val prettyViews = remember(item.views) { item.views?.toPrettyCount() ?: PLACEHOLDER_TEXT }
    val prettyDuration = remember(item.duration) { item.duration?.toMinSec() ?: PLACEHOLDER_TEXT }

    Box(modifier = Modifier.fillMaxSize().clip(TILE_SHAPE)) {
        //Индекс картинки
        Text(
            indexText,
            color = Color.Gray,
            modifier = Modifier.padding(start = PADDING_DEFAULT).offset(SHADOW_OFFSET, SHADOW_OFFSET),
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )

        //Нижний ряд с лайками и длительностью
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isVisibleView) {
                Row(
                    modifier = Modifier.padding(PADDING_DEFAULT),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box {
                        Icon(
                            painter = painterResource(R.drawable.rg_button),
                            contentDescription = null,
                            tint = Color.Black, modifier = Modifier.offset(SHADOW_OFFSET, SHADOW_OFFSET)
                        )
                        Icon(
                            painter = painterResource(R.drawable.rg_button),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Box {
                        Text(
                            prettyViews,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(start = PADDING_DEFAULT)
                                .offset(SHADOW_OFFSET, SHADOW_OFFSET),
                            fontFamily = Theme.R.fontFamilyPopinsMedium
                        )

                        Text(
                            prettyViews,
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = PADDING_DEFAULT),
                            fontFamily = Theme.R.fontFamilyPopinsMedium
                        )
                    }

                }
            }

            if (isVisibleDuration) {
                Box {

                    Text(
                        prettyDuration,
                        color = Color.Black,
                        modifier = Modifier
                            .padding(PADDING_DEFAULT)
                            .offset(SHADOW_OFFSET, SHADOW_OFFSET),
                        fontFamily = Theme.R.fontFamilyPopinsMedium
                    )

                    Text(
                        prettyDuration,
                        color = Color.White,
                        modifier = Modifier
                            .padding(PADDING_DEFAULT),
                        fontFamily = Theme.R.fontFamilyPopinsMedium
                    )
                }
            }

        }

    }

}
