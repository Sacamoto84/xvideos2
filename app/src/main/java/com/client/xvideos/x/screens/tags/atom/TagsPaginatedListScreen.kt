package com.client.xvideos.x.screens.tags.atom

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.common.urlVideoImage.UrlVideoImageAndLongClickX

@Composable
fun TagsPaginatedListScreen(pageIndex: Int) {

    val l = remember { mutableStateListOf<ItemsX>() }

    LaunchedEffect(pageIndex) {
//        withContext(Dispatchers.IO) {
//            l.clear()
//            l.addAll(openNew(pageIndex).filter { !it.link.contains("THUMBNUM") })
//        }
    }


    val itemsPerRow = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(l.chunked(itemsPerRow))
        { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { index, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(352f / 198f)
                            .padding(1.dp)
                            .background(Color.DarkGray)
                    ) {

                        UrlVideoImageAndLongClickX(cell, onLongClick = {
                            //vm.openItem(urlStart + cell.link, navigator)
                        }, onDoubleClick = {})

                    }
                }
                // Если элементов в строке меньше, чем itemsPerRow, добавляем пустые ячейки
                if (row.size < itemsPerRow) {
                    repeat(itemsPerRow - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
