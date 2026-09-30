package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.CreatorMetric

@Composable
fun CreatorListItem(
    item: UserInfo,
    onClick: (String) -> Unit,
    onDelete: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = remember(item.name, item.username) { item.name.ifBlank { item.username } }
    val handleItemClick = remember(item.username, onClick) { { onClick(item.username) } }
    val handleDeleteClick = remember(item, onDelete) { { onDelete(item) } }

    Row(
        modifier = modifier
            .padding(vertical = 2.dp, horizontal = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .background(Theme.tabLevel3)
            .clickable(onClick = handleItemClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (item.profileImageUrl != null) {
            UrlImage(
                item.profileImageUrl,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.DarkGray),
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
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = Theme.R.fontFamilyDMsanss,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (displayName != item.username) {
                Text(
                    "@${item.username}",
                    color = Color(0xFF9E9DA9),
                    fontSize = 12.sp,
                    fontFamily = Theme.R.fontFamilyDMsanss,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CreatorMetric(
                    label = "Подписчики",
                    value = item.followers,
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = "Просмотры",
                    value = item.views,
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = "Посты",
                    value = item.publishedGifs,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        IconButton(
            onClick = handleDeleteClick,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить автора",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(24.dp)
            )
        }
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
