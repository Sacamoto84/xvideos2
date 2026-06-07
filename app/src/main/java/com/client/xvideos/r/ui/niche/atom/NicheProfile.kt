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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme

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

        Row(
            modifier = Modifier
                .padding(start = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrlImage(
                niche().thumbnail,
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(128.dp)
                    .weight(1f), verticalArrangement = Arrangement.SpaceBetween
            ) {

                val color = if (niche().id == "id") Color.Transparent else Color.Gray

                if (niche().id != "id") {
                    Text(niche().name, color = Color.White, fontFamily = Theme.R.fontFamilyDMsanss)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.members),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = niche().subscribers.toPrettyCount(),
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
                        text = niche().gifs.toPrettyCount(),
                        modifier = Modifier
                            .padding(start = 4.dp, end = 4.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }

//                Text(
//                    niche.subscribers.toPrettyCount(),
//                    color = color,
//                    modifier = Modifier,
//                    fontFamily = Theme.R.fontFamilyDMsanss
//                )
//
//                Text(
//                    niche.gifs.toPrettyCount(),
//                    color = color,
//                    fontFamily = Theme.R.fontFamilyDMsanss
//                )

                if (niche().id != "id") {
                    ButtonFollowContent(isFollowed = isFollowed, onClick = onFollowClick)
                }

            }


        }
}

@Composable
private fun ButtonFollow(savedRed: () -> SavedRed, niche: NichesInfo) {
    val isFollowed = savedRed().niches.list.any { it.id == niche.id }

    ButtonFollowContent(
        isFollowed = isFollowed,
        onClick = {
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
private fun ButtonFollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .width(128.dp)
            .height(44.dp)
            .border(
                1.dp, if (isFollowed) Color.White else Color.Transparent, RoundedCornerShape(8.dp)
            )
            .background(if (isFollowed) Theme.tabLevel1 else Theme.R.colorYellow)
            .clickable(onClick = onClick), contentAlignment = Alignment.Center
    ) {
        Text(
            if (isFollowed) "Выйти" else "Подписаться",
            color = if (isFollowed) Color.White else Color.Black
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
