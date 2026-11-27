package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.icons.IconCollection18
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideoTags
import com.client.xvideos.r.ui.top_this_week.ProfileInfo1

@Composable
internal fun LazyRow123GridItemContentStateless(
    item: GifsInfo,
    visibleProfileInfo: Boolean,
    columns: Int,
    onClickOpenProfile: (String) -> Unit,
    onTagClick: (String) -> Unit,
    videoImage: @Composable ((Boolean) -> Unit) -> Unit,
    expandMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icons: @Composable BoxScope.() -> Unit,
) {
    var isVideo by remember(item.id) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        videoImage { isVideo = it }

        Column(modifier = Modifier.align(Alignment.TopEnd)) {
            expandMenu()

            if (item.tags.isNotEmpty()) { ExpandMenuVideoTags(item = item, onClick = onTagClick) }
        }

        AnimatedVisibility(
            visible = !isVideo,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (visibleProfileInfo) {
                    ProfileInfo1(
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp).align(Alignment.BottomStart),
                        onClick = { onClickOpenProfile(item.userName) },
                        videoItem = item,
                        // Поиск по ключу, а не listAllUsers: тот на каждую ячейку и
                        // каждую перекомпозицию копировал весь кэш авторов.
                        avatarUrl = UsersRed.findUser(item.userName)?.profileImageUrl,
                        visibleUserName = columns <= 2,
                        sizeIcon = 36.dp,
                        cornerRadius = 8.dp,
                        verticalAlignment = Alignment.Top
                    )
                }
                icons()
            }
        }
    }
}

@Preview
@Composable
private fun LazyRow123GridItemContentStatelessPreview() {
    val sampleItem = GifsInfo(
        id = "sample_id",
        userName = "SampleUser",
        tags = listOf("Tag1", "Tag2"),
        urls = URL1(thumbnail = "")
    )
    LazyRow123GridItemContentStateless(
        item = sampleItem,
        visibleProfileInfo = true,
        columns = 2,
        onClickOpenProfile = {},
        onTagClick = {},
        videoImage = {
            Box(Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                Text("Video Image Placeholder", color = Color.White)
            }
        },
        expandMenu = {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
        },
        icons = {
            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                IconCollection18(Modifier.padding(bottom = 6.dp, end = 6.dp))
                IconSave18(Modifier.padding(bottom = 6.dp, end = 6.dp))
            }
        }
    )
}
