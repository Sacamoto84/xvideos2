package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideoTags
import com.client.xvideos.r.ui.fullscreen.atom.UserAvatarWithBadges

@Composable
fun RedFullScreenOverlay(
    item: GifsInfo,
    profileImageUrl: String?,
    isInCollection: Boolean,
    isCreatorFollowed: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onAvatarClick: () -> Unit,
    onBack: () -> Unit,
    onToggleRotate: () -> Unit,
    onTagClick: (String) -> Unit,
    actionsMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            UserAvatarWithBadges(
                profileImageUrl = profileImageUrl,
                isInCollection = isInCollection,
                isCreatorFollowed = isCreatorFollowed,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                onClick = onAvatarClick
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleRotate) {
                Icon(
                    Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            ExpandMenuVideoTags(
                item = item,
                modifier = Modifier,
                onClick = onTagClick,
            )

            actionsMenu()
        }
    }
}

@Preview
@Composable
private fun RedFullScreenOverlayPreview() {
    RedFullScreenOverlay(
        item = GifsInfo(id = "test", userName = "TestCreator"),
        profileImageUrl = null,
        isInCollection = false,
        isCreatorFollowed = true,
        isLiked = true,
        isDownloaded = false,
        onAvatarClick = {},
        onBack = {},
        onToggleRotate = {},
        onTagClick = {},
        actionsMenu = {}
    )
}
