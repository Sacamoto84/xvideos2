package com.client.xvideos.r.ui.profile.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.profile.atom.CreatorStatsRow
import com.client.xvideos.r.ui.profile.atom.CreatorTopInfoRow
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Блок полной информации о профиле креатора в шапке экрана профиля.
 */
@Composable
fun RedProfileCreaterInfo(
    item: UserInfo,
    savedRed: () -> SavedRed,
    modifier: Modifier = Modifier,
) {
    val isFollow by remember(item.username) {
        derivedStateOf {
            savedRed().creators.list.any { it.username == item.username }
        }
    }
    val onFollowClick = remember(isFollow, item, savedRed) {
        {
            if (isFollow) {
                savedRed().creators.remove(item.username)
            } else {
                savedRed().creators.add(item)
            }
        }
    }
    RedProfileCreaterInfo(
        item = item,
        isFollow = isFollow,
        onFollowClick = onFollowClick,
        modifier = modifier,
    )
}

@Composable
fun RedProfileCreaterInfo(
    item: UserInfo,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followersPretty = remember(item.followers) { item.followers.toPrettyCount() }
    val viewsPretty = remember(item.views) { item.views.toPrettyCount() }
    val publishedGifsPretty = remember(item.publishedGifs) { item.publishedGifs.toPrettyCount() }
    val aboutTitle = remember(item.username) { "About ${item.username}:" }
    val descriptionTrimmed = remember(item.description) { item.description?.trimMargin() }

    Column(modifier = modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
        CreatorTopInfoRow(
            item = item,
            isFollow = isFollow,
            onFollowClick = onFollowClick
        )

        CreatorStatsRow(
            followersPretty = followersPretty,
            viewsPretty = viewsPretty,
            publishedGifsPretty = publishedGifsPretty
        )

        if (descriptionTrimmed != null) {
            Text(
                aboutTitle,
                color = Theme.R.colorTextGray,
                fontSize = 14.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )

            Spacer(Modifier.height(4.dp))

            Text(
                descriptionTrimmed,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun RedProfileCreaterInfoPreview() {
    val sampleUserInfo = UserInfo(
        username = "lilijunex",
        profileImageUrl = "https://userpic.redgifs.com/4/8c/48cc3668e114f878aafcc6dfd0a3d4f2.png",
        followers = 68214,
        views = 123194825,
        publishedGifs = 421,
        description = "Collared sub addicted to XL horse dildos",
        url = "https://www.redgifs.com/users/lilijunex"
    )

    XvideosTheme {
        Box(modifier = Modifier.background(Theme.R.colorCommonBackground)) {
            RedProfileCreaterInfo(
                item = sampleUserInfo,
                isFollow = false,
                onFollowClick = {}
            )
        }
    }
}
