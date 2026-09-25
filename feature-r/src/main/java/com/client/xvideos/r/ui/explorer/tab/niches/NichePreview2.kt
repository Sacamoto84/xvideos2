package com.client.xvideos.r.ui.explorer.tab.niches

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.r.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCountInt
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme

private val NICHE_CARD_SHAPE = RoundedCornerShape(16.dp)
private val NICHE_IMAGE_SHAPE = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
private val NICHE_BUTTON_SHAPE = RoundedCornerShape(10.dp)
private const val BUTTON_FOLLOW_TEXT = "Подписаться"
private const val BUTTON_UNFOLLOW_TEXT = "Выйти"

private val CARD_HORIZONTAL_PADDING = 8.dp
private val CARD_HEIGHT = 78.dp
private val IMAGE_START_PADDING = 4.dp
private val IMAGE_SIZE = 70.dp
private val CONTENT_START_PADDING = 8.dp
private val CONTENT_VERTICAL_PADDING = 4.dp
private val ROW_HEIGHT = 23.dp
private val STAT_SPACER_PADDING = 4.dp
private val STAT_ICON_SIZE = 18.dp
private val BUTTON_WIDTH = 128.dp
private val BUTTON_HEIGHT = 44.dp
private val BUTTON_END_PADDING = 6.dp
private val BUTTON_BORDER_WIDTH = 1.dp
private val TITLE_FONT_SIZE = 18.sp
private val STAT_FONT_SIZE = 16.sp
private const val CD_SUBSCRIBERS = "Подписчики"
private const val CD_POSTS = "Публикации"

private val CARD_BASE_MODIFIER = Modifier
    .padding(horizontal = CARD_HORIZONTAL_PADDING)
    .fillMaxWidth()
    .height(CARD_HEIGHT)
    .clip(NICHE_CARD_SHAPE)
    .background(Theme.tabLevel3)

private val CARD_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val CARD_VERTICAL_ALIGNMENT = Alignment.CenterVertically

private val THUMBNAIL_BASE_MODIFIER = Modifier
    .padding(start = IMAGE_START_PADDING)
    .size(IMAGE_SIZE)
    .clip(NICHE_IMAGE_SHAPE)

private val CONTENT_COLUMN_BASE_MODIFIER = Modifier
    .padding(start = CONTENT_START_PADDING, top = CONTENT_VERTICAL_PADDING, bottom = CONTENT_VERTICAL_PADDING)
    .fillMaxWidth()
    .fillMaxHeight()

private val CONTENT_COLUMN_ARRANGEMENT = Arrangement.SpaceBetween

private val TITLE_TEXT_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(ROW_HEIGHT)

private val STATS_ROW_CONTAINER_MODIFIER = Modifier
    .fillMaxWidth()
    .fillMaxHeight()

private val STATS_ROW_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val STATS_ROW_VERTICAL_ALIGNMENT = Alignment.Top

private val STAT_ITEM_ROW_MODIFIER = Modifier.height(ROW_HEIGHT)
private val STAT_ITEM_VERTICAL_ALIGNMENT = Alignment.CenterVertically

private val STAT_ICON_MODIFIER = Modifier.size(STAT_ICON_SIZE)
private val STAT_TEXT_PADDING_MODIFIER = Modifier.padding(start = STAT_SPACER_PADDING)

private val BUTTON_BASE_MODIFIER = Modifier
    .padding(end = BUTTON_END_PADDING)
    .width(BUTTON_WIDTH)
    .height(BUTTON_HEIGHT)
    .clip(NICHE_BUTTON_SHAPE)

private val BUTTON_BORDER_MODIFIER = Modifier.border(BUTTON_BORDER_WIDTH, Color.White, NICHE_BUTTON_SHAPE)
private val BUTTON_FOLLOWED_BASE_MODIFIER = BUTTON_BASE_MODIFIER.then(BUTTON_BORDER_MODIFIER)
private val BUTTON_ALIGNMENT = Alignment.Center

private val COLOR_LIGHT_GRAY = Color.LightGray
private val COLOR_WHITE = Color.White
private val COLOR_BLACK = Color.Black

@Composable
fun NichePreview2(
    niches: () -> Niche,
    savedRed: () -> SavedRed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val niche = niches()

    val isFollowed by remember(niche.id) {
        derivedStateOf {
            savedRed().niches.list.any { it.id == niche.id }
        }
    }

    val onFollowClick = remember(isFollowed, niche, savedRed) {
        {
            val red = savedRed()
            val nichesInfo = NichesInfo(
                id = niche.id,
                name = niche.name,
                subscribers = niche.subscribers,
                gifs = niche.gifs,
                thumbnail = niche.thumbnail,
            )

            if (isFollowed) red.niches.remove(nichesInfo) else red.niches.add(nichesInfo)
        }
    }

    NichePreview2Content(
        niche = niche,
        isFollowed = isFollowed,
        onFollowClick = onFollowClick,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun NichePreview2Content(
    niche: Niche,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subscribersText = remember(niche.subscribers) { niche.subscribers.toPrettyCountInt() }
    val gifsText = remember(niche.gifs) { niche.gifs.toPrettyCountInt() }
    val buttonText = if (isFollowed) BUTTON_UNFOLLOW_TEXT else BUTTON_FOLLOW_TEXT
    val buttonTextColor = if (isFollowed) COLOR_WHITE else COLOR_BLACK
    val buttonBgColor = if (isFollowed) Theme.tabLevel0 else Theme.R.colorYellow
    val currentButtonBase = if (isFollowed) BUTTON_FOLLOWED_BASE_MODIFIER else BUTTON_BASE_MODIFIER

    Row(
        modifier = modifier
            .then(CARD_BASE_MODIFIER)
            .clickable(onClick = onClick),
        verticalAlignment = CARD_VERTICAL_ALIGNMENT,
        horizontalArrangement = CARD_HORIZONTAL_ARRANGEMENT
    ) {

        UrlImage(
            niche.thumbnail,
            modifier = THUMBNAIL_BASE_MODIFIER
        )

        Column(
            modifier = CONTENT_COLUMN_BASE_MODIFIER,
            verticalArrangement = CONTENT_COLUMN_ARRANGEMENT
        ) {

            Text(
                text = niche.name,
                modifier = TITLE_TEXT_BASE_MODIFIER,
                color = COLOR_WHITE,
                fontSize = TITLE_FONT_SIZE,
                fontFamily = Theme.R.fontFamilyDMsanss
            )

            Row(
                modifier = STATS_ROW_CONTAINER_MODIFIER,
                horizontalArrangement = STATS_ROW_HORIZONTAL_ARRANGEMENT,
                verticalAlignment = STATS_ROW_VERTICAL_ALIGNMENT
            ) {

                Column {
                    Row(
                        modifier = STAT_ITEM_ROW_MODIFIER,
                        verticalAlignment = STAT_ITEM_VERTICAL_ALIGNMENT
                    ) {
                        Icon(
                            painterResource(R.drawable.members),
                            contentDescription = CD_SUBSCRIBERS,
                            modifier = STAT_ICON_MODIFIER,
                            tint = COLOR_LIGHT_GRAY,
                        )
                        Text(
                            text = subscribersText,
                            modifier = STAT_TEXT_PADDING_MODIFIER,
                            color = COLOR_LIGHT_GRAY,
                            fontSize = STAT_FONT_SIZE,
                            fontFamily = Theme.R.fontFamilyDMsanss
                        )
                    }
                    Row(
                        modifier = STAT_ITEM_ROW_MODIFIER,
                        verticalAlignment = STAT_ITEM_VERTICAL_ALIGNMENT
                    ) {
                        Icon(
                            painterResource(R.drawable.posts),
                            contentDescription = CD_POSTS,
                            modifier = STAT_ICON_MODIFIER,
                            tint = COLOR_LIGHT_GRAY,
                        )
                        Text(
                            text = gifsText,
                            modifier = STAT_TEXT_PADDING_MODIFIER,
                            color = COLOR_LIGHT_GRAY,
                            fontSize = STAT_FONT_SIZE,
                            fontFamily = Theme.R.fontFamilyDMsanss
                        )
                    }
                }

                Box(
                    modifier = currentButtonBase
                        .background(buttonBgColor)
                        .clickable(onClick = onFollowClick),
                    contentAlignment = BUTTON_ALIGNMENT
                ) {
                    Text(
                        text = buttonText,
                        color = buttonTextColor
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NichePreview2Preview() {
    XvideosTheme {
        NichePreview2Content(
            niche = Niche(
                id = "female-backs",
                name = "Female Backs",
                gifs = 245,
                subscribers = 914,
                thumbnail = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
                previews = emptyList()
            ),
            isFollowed = false,
            onFollowClick = {},
            onClick = {}
        )
    }
}

@Preview
@Composable
fun NichePreview2FollowedPreview() {
    XvideosTheme {
        NichePreview2Content(
            niche = Niche(
                id = "female-backs",
                name = "Female Backs",
                gifs = 245,
                subscribers = 914,
                thumbnail = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
                previews = emptyList()
            ),
            isFollowed = true,
            onFollowClick = {},
            onClick = {}
        )
    }
}
