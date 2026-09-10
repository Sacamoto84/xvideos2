package com.client.xvideos.r.ui.fullscreen


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.core.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideoTags
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.ScreenRedExplorer
import com.client.xvideos.r.ui.profile.ScreenRedProfile


@Composable
internal fun RedFullScreenOverlay(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    downloadList: List<GifsInfo>,
    haptic: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(onClick = { navigator.push(ScreenRedProfile(item.userName)) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val user = UsersRed.listAllUsers.firstOrNull { it.username == item.userName }
            if (user?.profileImageUrl != null) {
                UrlImage(
                    user.profileImageUrl,
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            if (vm.savedRed.collections.collectionList.any { collection -> collection.items.any { it.id == item.id } }) {
                Icon(
                    painter = painterResource(R.drawable.collection_multi_input_svgrepo_com),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(18.dp)
                )
            }

            if (vm.savedRed.creators.list.any { it.username == item.userName }) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(22.dp)
                )
            }

            if (vm.savedRed.likes.list.any { it.id == item.id }) {
                Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(22.dp)
                )
            }

            if (downloadList.any { it.id == item.id }) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(20.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    vm.autoRotate = !vm.autoRotate
                    haptic()
                }
            ) {
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
                onClick = { tag ->
                    vm.search.searchText.value = TextFieldValue(text = tag, selection = TextRange(tag.length))
                    vm.search.searchTextDone.value = tag
                    ScreenRedExplorer.screenType = 0
                    navigator.pop()
                },
            )

            ExpandMenuVideo(
                item = item,
                modifier = Modifier,
                onClick = {},
                haptic = haptic,
                onRunLike = {},
                onRefresh = {},
                isCollection = false,
                block = { vm.block },
                redApi = { vm.redApi },
                savedRed = { vm.savedRed },
                downloadRed = { vm.downloadRed }
            )
        }
    }
}
