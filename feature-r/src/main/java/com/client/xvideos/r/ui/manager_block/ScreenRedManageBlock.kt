package com.client.xvideos.r.ui.manager_block

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.manager_block.bottomr_bar.BottomrBar

class ScreenRedManageBlock : Screen {

    override val key: ScreenKey = "ScreenRedManageBlock"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        BackHandler { navigator.pop() }

        val vm: ScreenRedManageBlockSM = getScreenModel()
        val blockList = vm.blockList.collectAsStateWithLifecycle().value

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Заблокированные",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = Theme.R.fontFamilyPopinsMedium
                    )
                }
            },
            bottomBar = { BottomrBar() }
        ) { padding ->
            if (blockList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Список заблокированных пуст",
                        color = Theme.Text.secondary,
                        fontSize = 14.sp,
                        fontFamily = Theme.R.fontFamilyDMsanss
                    )
                }
            } else {
                val state = rememberLazyListState()
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    items(blockList, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .fillMaxWidth()
                                .height(128.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Theme.R.colorBorderGray, RoundedCornerShape(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UrlImage(
                                item.urls.thumbnail,
                                modifier = Modifier.aspectRatio(1f),
                                contentScale = ContentScale.Fit
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = item.userName,
                                    color = Theme.Text.primary,
                                    fontSize = 16.sp,
                                    fontFamily = Theme.R.fontFamilyPopinsMedium
                                )
                                Text(
                                    text = item.id,
                                    color = Theme.Text.secondary,
                                    fontSize = 13.sp,
                                    fontFamily = Theme.R.fontFamilyDMsanss
                                )
                            }

                            IconButton(
                                onClick = { vm.unblock(item) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Разблокировать",
                                    tint = Color(0xFFFF7A7A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

