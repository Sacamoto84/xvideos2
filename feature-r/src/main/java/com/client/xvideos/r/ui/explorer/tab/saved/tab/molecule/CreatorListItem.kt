package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.core.R
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.CreatorMetric
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu

@Composable
fun CreatorListItem(
    item: UserInfo,
    onClick: (String) -> Unit,
    onDelete: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleItemClick = remember(item.username, onClick) { { onClick(item.username) } }
    val handleDeleteClick = remember(item, onDelete) { { onDelete(item) } }
    val avatarModifier = Modifier
        .padding(start = 3.dp)
        .size(72.dp)
        .clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp))

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.tabLevel3)
            .clickable(onClick = handleItemClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!item.profileImageUrl.isNullOrBlank()) {
            UrlImage(
                item.profileImageUrl,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = avatarModifier.background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = item.displayName,
                modifier = Modifier.height(23.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = Theme.R.fontFamilyDMsanss,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(modifier = Modifier.height(23.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "@${item.username.removePrefix("@")}",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    fontFamily = Theme.R.fontFamilyDMsanss,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(23.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CreatorMetric(
                    label = "Подписчики",
                    value = item.followers,
                    icon = painterResource(R.drawable.members),
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = "Просмотры",
                    value = item.views,
                    icon = rememberVectorPainter(Icons.Outlined.Visibility),
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = "Посты",
                    value = item.publishedGifs,
                    icon = painterResource(R.drawable.posts),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SavedItemActionsMenu(actionIcon = Icons.Filled.Delete, actionLabel = "Удалить", onAction = handleDeleteClick)
    }
}

@Preview
@Composable
private fun CreatorListItemPreview() {
    val sampleUser = UserInfo(
        name = "Sample Creator",
        username = "samplecreator",
        profileImageUrl = "https://via.placeholder.com/96",
        followers = 21_193,
        views = 32_986_108,
        publishedGifs = 2_176,
        url = "https://example.com/samplecreator"
    )
    CreatorListItem(
        item = sampleUser,
        onClick = {},
        onDelete = {}
    )
}
