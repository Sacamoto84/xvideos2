package com.client.xvideos.r.ui.explorer.tab.niches.molecule

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCountInt
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.model.Niche
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun NichePreview2Content(
    niche: Niche,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subscribersText = remember(niche.subscribers) { niche.subscribers.toPrettyCountInt() }
    val gifsText = remember(niche.gifs) { niche.gifs.toPrettyCountInt() }
    val buttonText = if (isFollowed) "Выйти" else "Подписаться"
    val buttonTextColor = if (isFollowed) Color.White else Color.Black
    val buttonBgColor = if (isFollowed) Theme.tabLevel0 else Theme.R.colorYellow
    val buttonShape = RoundedCornerShape(10.dp)
    val buttonStyledModifier = remember(isFollowed, buttonBgColor) {
        Modifier
            .padding(end = 6.dp)
            .width(128.dp)
            .height(44.dp)
            .clip(buttonShape)
            .background(buttonBgColor)
            .then(if (isFollowed) Modifier.border(1.dp, Color.White, buttonShape) else Modifier)
    }

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.tabLevel3)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        UrlImage(
            niche.thumbnail,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(70.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = niche.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(23.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = Theme.R.fontFamilyDMsanss
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        modifier = Modifier.height(23.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.members),
                            contentDescription = "Подписчики",
                            modifier = Modifier.size(18.dp),
                            tint = Color.LightGray,
                        )
                        Text(
                            text = subscribersText,
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            fontFamily = Theme.R.fontFamilyDMsanss
                        )
                    }
                    Row(
                        modifier = Modifier.height(23.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.posts),
                            contentDescription = "Публикации",
                            modifier = Modifier.size(18.dp),
                            tint = Color.LightGray,
                        )
                        Text(
                            text = gifsText,
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            fontFamily = Theme.R.fontFamilyDMsanss
                        )
                    }
                }

                Box(
                    modifier = buttonStyledModifier
                        .clickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
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
private fun NichePreview2ContentPreview() {
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
private fun NichePreview2ContentFollowedPreview() {
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
