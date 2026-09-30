package com.client.xvideos.r.ui.fullscreen.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.core.R

@Composable
fun UserAvatarWithBadges(
    profileImageUrl: String?,
    isInCollection: Boolean,
    isCreatorFollowed: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(start = 4.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (profileImageUrl != null) {
            UrlImage(
                profileImageUrl,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .size(40.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (isInCollection) {
            Icon(
                painter = painterResource(R.drawable.collection_multi_input_svgrepo_com),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(18.dp)
            )
        }

        if (isCreatorFollowed) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(22.dp)
            )
        }

        if (isLiked) {
            Icon(
                Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(22.dp)
            )
        }

        if (isDownloaded) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(20.dp)
            )
        }
    }
}

@Preview
@Composable
private fun UserAvatarWithBadgesPreview() {
    UserAvatarWithBadges(
        profileImageUrl = null,
        isInCollection = true,
        isCreatorFollowed = true,
        isLiked = true,
        isDownloaded = false,
        onClick = {}
    )
}
