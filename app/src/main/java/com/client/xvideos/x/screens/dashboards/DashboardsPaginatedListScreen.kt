package com.client.xvideos.x.screens.dashboards

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.x.parcer.parserListVideo
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.client.xvideos.urlStart
import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.common.urlVideoImage.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.dashboards.vm.ScreenXDashBoardsScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private suspend fun openNew(numberScreen: Int = 0): SnapshotStateList<ItemsX> {
    val currentNumberScreen = numberScreen.coerceIn(0, 19999)
    val url = urlStart + if (currentNumberScreen == 0) "" else "/new/${currentNumberScreen}"
    Timber.i("!!! openNew numberScreen:$numberScreen url:$url")
    val html = readHtmlFromURLWebView(url)
    // X4: страну выставляет вызывающий, парсер остаётся чистым.
    parseSiteCountryFlag(html)?.let { CountryState.current = it }
    return parserListVideo(html).toMutableStateList()
}


/**
 *
 * ![Логотип Markdown](https://ah-img.luscious.net/Joking42/499900/sample_3941cb87cea03_01J9ZXQ9XTDKY6PQ01ZRWF1FFZ.1680x0.jpg)
 *
 *
 */
@Composable
fun DashboardsPaginatedListScreen(pageIndex: Int, vm: ScreenXDashBoardsScreenModel) {

    val l = remember { mutableStateListOf<ItemsX>() }

    LaunchedEffect(key1 = pageIndex, key2 = CountryState.updateTrigger) {
        withContext(Dispatchers.IO) {
            l.clear()
            l.addAll(openNew(pageIndex).filter { !it.href.contains("THUMBNUM") })
            l
        }
    }

    val navigator = LocalNavigator.currentOrThrow

    val itemsPerRow =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else if (vm.countRow.field.collectAsState().value) 2 else 1


    val count = vm.countRow.field.collectAsStateWithLifecycle().value

    LazyVerticalGrid(
        columns = GridCells.Fixed(itemsPerRow),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(l, key = { index, it -> "${it.id}_$index" })
        { index, cell ->

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(352f / 198f)
                    .padding(1.dp)
                    .background(Color.DarkGray)
            ) {
                //Отобразить карточку картинка видео
                UrlVideoImageAndLongClickX(
                    cell,
                    onLongClick = {
                        //Открыть экран плеера
                        vm.openVideoPlayer(urlStart + cell.href, navigator)
                    },
                    onDoubleClick = {
                        vm.openVideoPlayer(urlStart + cell.href, navigator)
                    }
                )
                {

                    Box(modifier = Modifier.fillMaxSize()) {
                        val offsetY = (-3).dp

                        //Продолжительность видео
                        Text(
                            text = cell.duration.dropLast(1),
                            modifier = Modifier.fillMaxWidth().offset(0.5.dp, offsetY + 0.5.dp),
                            textAlign = TextAlign.Right,
                            fontSize = 14.sp,
                            color = Color.Black
                        )

                        Text(
                            text = cell.duration.dropLast(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(0.dp, offsetY),
                            textAlign = TextAlign.Right,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                    }


                    //Название канала
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).background(Color(0x60000000)), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cell.channel,
                            modifier = Modifier.align(Alignment.Center), fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Row(modifier = Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.End) {
                        if (vm.saved.favorites.contains(cell.id)) {
                            //Индикатор что видео в фаворитах
                            Box(modifier = Modifier) { IconFavorite18(Modifier.padding(bottom = 6.dp, end = 6.dp)) }
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.TopEnd)) { DropMenu( cell, vm ) }

                }
            }
        }

    }
}

@Composable
private fun DropMenu(cell: ItemsX, vm: ScreenXDashBoardsScreenModel) {

    var expanded by remember { mutableStateOf(false) }

    val count = vm.countRow.field.collectAsStateWithLifecycle().value

    val size = if (count) 26.dp else 32.dp

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {

        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Localized description",
                tint = Color.Black, modifier = Modifier
                    .size(size)
                    .offset(0.5.dp, 0.5.dp)
            )

            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Localized description",
                tint = Color.White, modifier = Modifier
                    .size(size)
                    .offset(0.dp, 0.dp)
            )

        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFFF2EDF7),
            shadowElevation = 2.dp, tonalElevation = 16.dp
        )
        {

            val isFavorite = vm.isFavorite(cell.id)

            DropdownMenuItem(
                text = { Text("Избранное") },
                onClick = {
                    expanded = false
                    scope.launch {
                        delay(50)
                        when (isFavorite) {
                            true -> vm.removeFavorite(cell)
                            false -> {
                                vm.addFavorite(cell)
                            }
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("TODO") },
                onClick = { /* Handle settings! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Save,
                        contentDescription = null
                    )
                }
            )

        }

    }
}
