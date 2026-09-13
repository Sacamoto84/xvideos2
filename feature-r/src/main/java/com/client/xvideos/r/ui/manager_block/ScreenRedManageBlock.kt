package com.client.xvideos.r.ui.manager_block

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.ui.manager_block.bottomr_bar.BottomrBar

class ScreenRedManageBlock() : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val vm: ScreenRedManageBlockSM = getScreenModel()

        val blockList = vm.blockList.collectAsStateWithLifecycle().value

        Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = { BottomrBar() }) { padding ->

            val state = rememberLazyListState()

            LazyColumn(
                state = state,
                modifier = Modifier
                    .padding(bottom = padding.calculateBottomPadding())
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
                            Text(item.userName, style = Theme.R.Type.bodyBold, color = Theme.R.colorWhite)
                            Text(item.id, style = Theme.R.Type.caption, color = Theme.R.colorSecondaryWhite)
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
