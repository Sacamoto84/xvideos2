package com.client.xvideos.r.ui.profile.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.r.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.ui.theme.XvideosTheme

private val PROFILE_AVATAR_SHAPE = RoundedCornerShape(8.dp)
private val PROFILE_FOLLOW_BUTTON_SHAPE = RoundedCornerShape(8.dp)
private val PROFILE_STAT_LABEL_COLOR = Color(0xFF9E9DA9)
private val PROFILE_STAT_DIVIDER_COLOR = Color(0xFF3D3C53)

private val AVATAR_SIZE = 96.dp
private val STAT_DIVIDER_WIDTH = 1.dp
private val STAT_DIVIDER_HEIGHT = 24.dp
private val PERSON_ICON_SIZE = 24.dp
private val VERIFIED_BADGE_SIZE = 26.dp
private val ROW_HEADER_HEIGHT = 48.dp
private val BUTTON_HEIGHT = 48.dp
private val BUTTON_BORDER_WIDTH = 1.dp
private val FOLLOW_BUTTON_BORDER_MODIFIER = Modifier.border(BUTTON_BORDER_WIDTH, Color.White, PROFILE_FOLLOW_BUTTON_SHAPE)
private val BUTTON_START_PADDING = 8.dp
private val BUTTON_END_PADDING = 64.dp
private val USERNAME_SPACER_WIDTH = 8.dp
private val VERIFIED_OFFSET_Y = 8.dp

private val ROOT_HORIZONTAL_PADDING = 4.dp
private val TOP_ROW_PADDING_TOP = 2.dp
private val STATS_ROW_VERTICAL_PADDING = 8.dp
private val DESCRIPTION_SPACER_HEIGHT = 4.dp
private val BOTTOM_SPACER_HEIGHT = 8.dp

private val USERNAME_FONT_SIZE = 28.sp
private val BUTTON_FONT_SIZE = 18.sp
private val DESCRIPTION_FONT_SIZE = 14.sp

private const val TEXT_FOLLOW = "Follow"
private const val TEXT_UNFOLLOW = "Unfollow"
private const val TEXT_SUBSCRIBERS = "Подписчиков"
private const val TEXT_VIEWS = "Просмотров"
private const val TEXT_POSTS = "Постов"
private const val CD_VERIFIED_CREATOR = "Verified Creator"
private const val ABOUT_TITLE_PREFIX = "About "
private const val ABOUT_TITLE_SUFFIX = ":"

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
    val aboutTitle = remember(item.username) { "$ABOUT_TITLE_PREFIX${item.username}$ABOUT_TITLE_SUFFIX" }
    val descriptionTrimmed = remember(item.description) { item.description?.trimMargin() }

    Column(modifier = modifier.padding(horizontal = ROOT_HORIZONTAL_PADDING).fillMaxWidth()) {
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
                fontSize = DESCRIPTION_FONT_SIZE,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )

            Spacer(Modifier.height(DESCRIPTION_SPACER_HEIGHT))

            Text(
                descriptionTrimmed,
                color = Color.White,
                fontSize = DESCRIPTION_FONT_SIZE,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        }

        Spacer(Modifier.height(BOTTOM_SPACER_HEIGHT))
    }
}

@Composable
private fun CreatorTopInfoRow(
    item: UserInfo,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followButtonText = if (isFollow) TEXT_UNFOLLOW else TEXT_FOLLOW
    val followButtonTextColor = if (isFollow) Color.White else Color.Black
    val followButtonBgColor = if (isFollow) Theme.tabLevel1 else Theme.R.colorYellow
    val followButtonBorderModifier = if (isFollow) FOLLOW_BUTTON_BORDER_MODIFIER else Modifier

    Row(
        modifier = modifier.padding(top = TOP_ROW_PADDING_TOP).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.profileImageUrl != null) {
            UrlImage(
                item.profileImageUrl,
                modifier = Modifier.clip(PROFILE_AVATAR_SHAPE).size(AVATAR_SIZE)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(PROFILE_AVATAR_SHAPE)
                    .size(AVATAR_SIZE)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(PERSON_ICON_SIZE),
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().height(AVATAR_SIZE),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Row(
                modifier = Modifier.height(ROW_HEADER_HEIGHT),
                verticalAlignment = Alignment.Top
            ) {
                Spacer(Modifier.width(USERNAME_SPACER_WIDTH))
                Text(
                    item.username,
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsMedium,
                    fontSize = USERNAME_FONT_SIZE,
                    modifier = Modifier
                )
                if (item.verified) {
                    Spacer(Modifier.width(USERNAME_SPACER_WIDTH))
                    Image(
                        painter = painterResource(id = R.drawable.verificed),
                        contentDescription = CD_VERIFIED_CREATOR,
                        modifier = Modifier.size(VERIFIED_BADGE_SIZE).offset(y = VERIFIED_OFFSET_Y)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = BUTTON_START_PADDING, end = BUTTON_END_PADDING)
                    .fillMaxWidth()
                    .height(BUTTON_HEIGHT)
                    .clip(PROFILE_FOLLOW_BUTTON_SHAPE)
                    .background(followButtonBgColor)
                    .then(followButtonBorderModifier)
                    .clickable(onClick = onFollowClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    followButtonText,
                    color = followButtonTextColor,
                    fontFamily = Theme.R.fontFamilyDMsanss,
                    fontSize = BUTTON_FONT_SIZE,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CreatorStatsRow(
    followersPretty: String,
    viewsPretty: String,
    publishedGifsPretty: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = STATS_ROW_VERTICAL_PADDING).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(
            count = followersPretty,
            label = TEXT_SUBSCRIBERS,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        StatDivider()

        StatItem(
            count = viewsPretty,
            label = TEXT_VIEWS,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        StatDivider()

        StatItem(
            count = publishedGifsPretty,
            label = TEXT_POSTS,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@Composable
private fun StatItem(
    count: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(count, color = Color.White, fontFamily = Theme.R.fontFamilyPopinsMedium)
        Text(label, color = PROFILE_STAT_LABEL_COLOR, fontFamily = Theme.R.fontFamilyPopinsRegular)
    }
}

@Composable
private fun StatDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(STAT_DIVIDER_WIDTH)
            .height(STAT_DIVIDER_HEIGHT)
            .background(PROFILE_STAT_DIVIDER_COLOR)
    )
}

@Preview
@Composable
fun RedProfileCreaterInfoPreview() {
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
