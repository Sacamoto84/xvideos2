package com.client.xvideos.r.ui.niche.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.r.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme

private val NICHE_THUMBNAIL_SHAPE = RoundedCornerShape(8.dp)
private val NICHE_FOLLOW_BUTTON_SHAPE = RoundedCornerShape(8.dp)

private val NICHE_THUMBNAIL_SIZE = 128.dp
private val COLUMN_HEIGHT = 128.dp
private val STAT_ICON_SIZE = 16.dp
private val STAT_FONT_SIZE = 16.sp
private val FOLLOW_BUTTON_WIDTH = 128.dp
private val FOLLOW_BUTTON_HEIGHT = 44.dp
private val FOLLOW_BORDER_WIDTH = 1.dp
private val PADDING_XSMALL = 4.dp
private val PADDING_SMALL = 8.dp

private val COLOR_WHITE = Color.White
private val COLOR_BLACK = Color.Black
private val COLOR_TRANSPARENT = Color.Transparent

private val ALIGN_CENTER = Alignment.Center
private val ALIGN_CENTER_HORIZONTALLY = Alignment.CenterHorizontally
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val ROW_HORIZONTAL_ARRANGEMENT = Arrangement.Center
private val COLUMN_VERTICAL_ARRANGEMENT = Arrangement.SpaceBetween

private val STAT_ICON_SIZE_MODIFIER = Modifier.size(STAT_ICON_SIZE)
private val STAT_TEXT_MODIFIER = Modifier
    .padding(start = PADDING_XSMALL, end = PADDING_XSMALL)
    .wrapContentWidth(ALIGN_CENTER_HORIZONTALLY)
private val NICHE_THUMBNAIL_BASE_MODIFIER = Modifier
    .size(NICHE_THUMBNAIL_SIZE)
    .clip(NICHE_THUMBNAIL_SHAPE)
private val COLUMN_BASE_MODIFIER = Modifier
    .padding(start = PADDING_SMALL)
    .height(COLUMN_HEIGHT)
private val FOLLOW_BUTTON_BASE_MODIFIER = Modifier
    .padding(end = PADDING_XSMALL)
    .clip(NICHE_FOLLOW_BUTTON_SHAPE)
    .width(FOLLOW_BUTTON_WIDTH)
    .height(FOLLOW_BUTTON_HEIGHT)

private val STAT_TEXT_STYLE = TextStyle(
    color = COLOR_WHITE,
    textAlign = TextAlign.Center,
    fontSize = STAT_FONT_SIZE,
)

private const val TEXT_UNSUBSCRIBE = "Выйти"
private const val TEXT_SUBSCRIBE = "Подписаться"
private const val DEFAULT_PLACEHOLDER_ID = "id"

@Composable
fun NicheProfile(
    savedRed: () -> SavedRed,
    niche: NichesInfo,
    modifier: Modifier = Modifier,
) {
    val isFollowed = savedRed().niches.list.any { it.id == niche.id }

    val handleFollowClick = remember(niche, isFollowed, savedRed) {
        {
            val nichesInfo = NichesInfo(
                id = niche.id,
                name = niche.name,
                subscribers = niche.subscribers,
                gifs = niche.gifs,
                thumbnail = niche.thumbnail,
            )

            if (isFollowed) {
                savedRed().niches.remove(nichesInfo)
            } else {
                savedRed().niches.add(nichesInfo)
            }
        }
    }

    NicheProfileContent(
        niche = { niche },
        isFollowed = isFollowed,
        onFollowClick = handleFollowClick,
        modifier = modifier
    )
}

@Composable
fun NicheProfileContent(
    niche: () -> NichesInfo,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentNiche = niche()
    val subscribersText = remember(currentNiche.subscribers) { currentNiche.subscribers.toPrettyCount() }
    val gifsText = remember(currentNiche.gifs) { currentNiche.gifs.toPrettyCount() }

    Row(
        modifier = modifier
            .padding(start = PADDING_XSMALL)
            .fillMaxWidth(),
        horizontalArrangement = ROW_HORIZONTAL_ARRANGEMENT,
        verticalAlignment = ROW_VERTICAL_ALIGNMENT
    ) {
        UrlImage(
            currentNiche.thumbnail,
            modifier = NICHE_THUMBNAIL_BASE_MODIFIER
        )

        Column(
            modifier = COLUMN_BASE_MODIFIER.weight(1f),
            verticalArrangement = COLUMN_VERTICAL_ARRANGEMENT
        ) {
            if (currentNiche.id != DEFAULT_PLACEHOLDER_ID) {
                Text(currentNiche.name, color = COLOR_WHITE, fontFamily = Theme.R.fontFamilyDMsanss)
            }

            Row(verticalAlignment = ROW_VERTICAL_ALIGNMENT) {
                Icon(
                    painter = painterResource(R.drawable.members),
                    contentDescription = null,
                    tint = COLOR_WHITE,
                    modifier = STAT_ICON_SIZE_MODIFIER
                )
                Text(
                    text = subscribersText,
                    modifier = STAT_TEXT_MODIFIER,
                    style = STAT_TEXT_STYLE
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
                    modifier = STAT_TEXT_MODIFIER,
                    style = STAT_TEXT_STYLE
                )
            }

            if (currentNiche.id != DEFAULT_PLACEHOLDER_ID) {
                ButtonFollowContent(isFollowed = isFollowed, onClick = onFollowClick)
            }
        }
    }
}

@Composable
private fun ButtonFollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonText = remember(isFollowed) { if (isFollowed) TEXT_UNSUBSCRIBE else TEXT_SUBSCRIBE }
    val buttonTextColor = remember(isFollowed) { if (isFollowed) COLOR_WHITE else COLOR_BLACK }
    val buttonBgColor = remember(isFollowed) { if (isFollowed) Theme.tabLevel1 else Theme.R.colorYellow }
    val buttonBorderColor = remember(isFollowed) { if (isFollowed) COLOR_WHITE else COLOR_TRANSPARENT }

    Box(
        modifier = modifier
            .then(FOLLOW_BUTTON_BASE_MODIFIER)
            .border(FOLLOW_BORDER_WIDTH, buttonBorderColor, NICHE_FOLLOW_BUTTON_SHAPE)
            .background(buttonBgColor)
            .clickable(onClick = onClick),
        contentAlignment = ALIGN_CENTER
    ) {
        Text(
            text = buttonText,
            color = buttonTextColor
        )
    }
}

@Preview
@Composable
fun NicheProfilePreview() {
    XvideosTheme {
        NicheProfileContent(
            niche = {
                NichesInfo(
                id = "female-backs",
                name = "Female Backs",
                subscribers = 914,
                gifs = 245,
                thumbnail = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg"
                )
            },
            isFollowed = false,
            onFollowClick = {}
        )
    }
}

@Preview
@Composable
fun ButtonFollowPreview() {
    XvideosTheme {
        ButtonFollowContent(
            isFollowed = false,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun ButtonFollowFollowedPreview() {
    XvideosTheme {
        ButtonFollowContent(
            isFollowed = true,
            onClick = {}
        )
    }
}
