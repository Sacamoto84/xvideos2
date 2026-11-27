package com.client.xvideos.r.ui.niche.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.core.R
import com.client.xvideos.r.model.NichesInfo

/**
 * Блок информации о профиле ниши (обложка, название, счетчики и кнопка подписки).
 */
@Composable
fun NicheProfileContent(
    niche: () -> NichesInfo,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val currentNiche = niche()

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
            Text(currentNiche.displayName, color = Color.White, fontFamily = Theme.R.fontFamilyDMsanss)
            if (currentNiche.subscribers >= 0) {
                NicheCounter(currentNiche.subscribers, R.drawable.members, "Подписчики")
            }
            if (currentNiche.gifs >= 0) {
                NicheCounter(currentNiche.gifs, R.drawable.posts, "Публикации")
            }
            Row(verticalAlignment = Alignment.Bottom) {
                ButtonFollowContent(isFollowed = isFollowed, onClick = onFollowClick)
                trailing?.invoke()
            }
        }
    }
}

@Preview
@Composable
private fun NicheProfileContentPreview() {
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
