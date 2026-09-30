package com.client.xvideos.r.ui.niche.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.ui.theme.XvideosTheme

private const val DEFAULT_PLACEHOLDER_ID = "id"

/**
 * Блок информации о профиле ниши (обложка, название, счетчики и кнопка подписки).
 */
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
            .padding(start = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UrlImage(
            currentNiche.thumbnail,
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .height(128.dp)
                .weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentNiche.id != DEFAULT_PLACEHOLDER_ID) {
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
                    style = TextStyle(
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                    )
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
                    style = TextStyle(
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                    )
                )
            }

            if (currentNiche.id != DEFAULT_PLACEHOLDER_ID) {
                ButtonFollowContent(isFollowed = isFollowed, onClick = onFollowClick)
            }
        }
    }
}

@Preview
@Composable
private fun NicheProfileContentPreview() {
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
