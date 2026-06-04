package com.client.xvideos.l.ui.screens.screenAlbumList.bottomBar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.ui.screens.screenAlbumList.atom.AlbumListPageSelector
import com.client.xvideos.r.common.ThemeRed

@Composable
fun AlbumListBottomBar(
    onClickVisibleFilter: () -> Unit,
    currentPage: Int,
    totalPages: Int,
    onChange: (Int) -> Unit = {}
) {
    Column {
        HorizontalDivider()

        Row(
            modifier = Modifier
                .padding(start = 4.dp)
                .fillMaxWidth()
                .height(48.dp)
                .background(ThemeRed.colorTabLevel1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(46.dp)
                    .height(46.dp)
                    .border(2.dp, Color(0xFF434343), RoundedCornerShape(4.dp))
                    .background(Color(0xFF414141))
                    .clickable(onClick = { onClickVisibleFilter() }),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
            }

            Box(modifier = Modifier.weight(1f)) {
                AlbumListPageSelector(currentPage, totalPages) { onChange(it) }
            }

//            Row {
//                ButtonRev(onClick = onClickPrev)
//                Spacer(Modifier.width(4.dp))
//                ButtonNext(onClick = onClickNext)
//                Spacer(Modifier.width(4.dp))
//            }

            Box(
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp)
                    .width(46.dp)
                    .height(46.dp)
                    .border(2.dp, Color(0xFF434343), RoundedCornerShape(4.dp))
                    .background(Color(0xFF414141))
                    .clickable(onClick = { onClickVisibleFilter() }),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
            }

        }



        HorizontalDivider()
    }
}

@Preview
@Composable
fun AlbumListBottomBarPreview() {
    AlbumListBottomBar(
        onClickVisibleFilter = {},
        currentPage = 1,
        totalPages = 10,
        onChange = {}
    )
}