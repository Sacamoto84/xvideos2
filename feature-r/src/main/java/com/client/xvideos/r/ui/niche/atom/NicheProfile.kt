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

private val nicheThumbnailShape = RoundedCornerShape(8.dp)
private val nicheFollowButtonShape = RoundedCornerShape(8.dp)

@Composable
fun NicheProfile(savedRed: () -> SavedRed, niche: NichesInfo) {
    val isFollowed = savedRed().niches.list.any { it.id == niche.id }

    NicheProfileContent(
        niche = { niche },
        isFollowed = isFollowed,
        onFollowClick = {
            val nichesInfo = NichesInfo(
                id = niche.id,
                name = niche.name,
                subscribers = niche.subscribers,
                gifs = niche.gifs,
                thumbnail = niche.thumbnail,
            )

            if (isFollowed)
                savedRed().niches.remove(nichesInfo)
            else
                savedRed().niches.add(nichesInfo)
        }
    )
}

@Composable
fun NicheProfileContent(
    niche: () -> NichesInfo,
    isFollowed: Boolean,
    onFollowClick: () -> Unit
) {
    val currentNiche = niche()
    val subscribersText = remember(currentNiche.subscribers) { currentNiche.subscribers.toPrettyCount() }
    val gifsText = remember(currentNiche.gifs) { currentNiche.gifs.toPrettyCount() }

    Row(
        modifier = Modifier
            .padding(start = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UrlImage(
            currentNiche.thumbnail,
            modifier = Modifier
                .size(128.dp)
                .clip(nicheThumbnailShape)
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .height(128.dp)
                .weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentNiche.id != "id") {
                Text(currentNiche.name, color = Color.White, fontFamily = Theme.R.fontFamilyDMsanss)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.members),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = subscribersText,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.posts),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = gifsText,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }

            if (currentNiche.id != "id") {
                ButtonFollowContent(isFollowed = isFollowed, onClick = onFollowClick)
            }
        }
    }
}

@Composable
private fun ButtonFollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit
) {
    val buttonText = remember(isFollowed) { if (isFollowed) "Выйти" else "Подписаться" }
    val buttonTextColor = remember(isFollowed) { if (isFollowed) Color.White else Color.Black }
    val buttonBgColor = remember(isFollowed) { if (isFollowed) Theme.tabLevel1 else Theme.R.colorYellow }
    val buttonBorderColor = remember(isFollowed) { if (isFollowed) Color.White else Color.Transparent }

    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(nicheFollowButtonShape)
            .width(128.dp)
            .height(44.dp)
            .border(1.dp, buttonBorderColor, nicheFollowButtonShape)
            .background(buttonBgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
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
