package com.client.xvideos.r.ui.niche.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.r.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Preview as NichePreviewModel
import com.client.xvideos.ui.theme.XvideosTheme

private val NICHE_CARD_SHAPE = RoundedCornerShape(8.dp)
private val NICHE_THUMBNAIL_SHAPE = RoundedCornerShape(6.dp)
private val NICHE_CARD_HEIGHT = 80.dp
private val NICHE_SHADOW_ELEVATION = 10.dp
private val NICHE_THUMBNAIL_SIZE = 72.dp
private val STAT_ICON_SIZE = 16.dp
private val STAT_FONT_SIZE = 16.sp
private val CARD_HORIZONTAL_PADDING = 4.dp
private val THUMBNAIL_PADDING = 4.dp
private val INFO_VERTICAL_PADDING = 4.dp
private val STAT_TEXT_PADDING = 4.dp

private val COLOR_WHITE = Color.White
private val ALIGN_START = Alignment.Start
private val ALIGN_CENTER_HORIZONTALLY = Alignment.CenterHorizontally
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val COLUMN_VERTICAL_ARRANGEMENT = Arrangement.SpaceBetween
private val TEXT_ALIGN_START = TextAlign.Start
private val TEXT_ALIGN_CENTER = TextAlign.Center

private val STAT_ICON_SIZE_MODIFIER = Modifier.size(STAT_ICON_SIZE)
private val THUMBNAIL_BASE_MODIFIER = Modifier
    .padding(THUMBNAIL_PADDING)
    .clip(NICHE_THUMBNAIL_SHAPE)
    .size(NICHE_THUMBNAIL_SIZE)
private val NAME_TEXT_PADDING = Modifier.padding(end = STAT_TEXT_PADDING)
private val STAT_TEXT_PADDING_MODIFIER = Modifier
    .padding(start = STAT_TEXT_PADDING, end = STAT_TEXT_PADDING)
    .wrapContentWidth(ALIGN_CENTER_HORIZONTALLY)
private val INFO_COLUMN_BASE_MODIFIER = Modifier
    .padding(vertical = INFO_VERTICAL_PADDING)
    .fillMaxHeight()

private val NICHE_CARD_STATIC_MODIFIER = Modifier
    .height(NICHE_CARD_HEIGHT)
    .padding(horizontal = CARD_HORIZONTAL_PADDING)
    .shadow(NICHE_SHADOW_ELEVATION, NICHE_CARD_SHAPE)
    .clip(NICHE_CARD_SHAPE)

@Composable
fun NichePreview(
    niches: () -> Niche,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val niche = niches()
    val subscribersText = remember(niche.subscribers) { niche.subscribers.toPrettyCount() }
    val gifsText = remember(niche.gifs) { niche.gifs.toPrettyCount() }

    val baseModifier = if (modifier == Modifier) {
        NICHE_CARD_STATIC_MODIFIER
    } else {
        modifier.then(NICHE_CARD_STATIC_MODIFIER)
    }

    Column(
        modifier = baseModifier
            .background(Theme.tabLevel3)
            .clickable(onClick = onClick)
    ) {
        Row {
            UrlImage(
                url = niche.thumbnail,
                modifier = THUMBNAIL_BASE_MODIFIER
            )

            Column(
                verticalArrangement = COLUMN_VERTICAL_ARRANGEMENT,
                horizontalAlignment = ALIGN_START,
                modifier = INFO_COLUMN_BASE_MODIFIER
            ) {
                Text(
                    text = niche.name,
                    modifier = NAME_TEXT_PADDING,
                    color = COLOR_WHITE,
                    textAlign = TEXT_ALIGN_START,
                )

                Row(verticalAlignment = ROW_VERTICAL_ALIGNMENT) {
                    Icon(
                        painter = painterResource(R.drawable.members),
                        contentDescription = null,
                        tint = COLOR_WHITE,
                        modifier = STAT_ICON_SIZE_MODIFIER
                    )
                    Text(
                        text = subscribersText,
                        modifier = STAT_TEXT_PADDING_MODIFIER,
                        color = COLOR_WHITE,
                        textAlign = TEXT_ALIGN_CENTER,
                        fontSize = STAT_FONT_SIZE
                    )
                }
                Row(verticalAlignment = ROW_VERTICAL_ALIGNMENT) {
                    Icon(
                        painter = painterResource(R.drawable.posts),
                        contentDescription = null,
                        tint = COLOR_WHITE,
                        modifier = STAT_ICON_SIZE_MODIFIER
                    )
                    Text(
                        text = gifsText,
                        modifier = STAT_TEXT_PADDING_MODIFIER,
                        color = COLOR_WHITE,
                        textAlign = TEXT_ALIGN_CENTER,
                        fontSize = STAT_FONT_SIZE
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NichePreviewPreview() {
    XvideosTheme {
        NichePreview(
            niches = {
                Niche(
                    id = "female-backs",
                    name = "Female Backs",
                    gifs = 245,
                    subscribers = 914,
                    thumbnail = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
                    previews = listOf(
                        NichePreviewModel(
                            id = "dangerouswanmice",
                            thumbnail = "https://media.redgifs.com/DangerousWanMice-mobile.jpg"
                        ),
                        NichePreviewModel(
                            id = "weirddaringbovine",
                            thumbnail = "https://media.redgifs.com/WeirdDaringBovine-mobile.jpg"
                        ),
                        NichePreviewModel(
                            id = "unsteadyphonywren",
                            thumbnail = "https://media.redgifs.com/UnsteadyPhonyWren-mobile.jpg"
                        )
                    )
                )
            },
            onClick = {}
        )
    }
}
