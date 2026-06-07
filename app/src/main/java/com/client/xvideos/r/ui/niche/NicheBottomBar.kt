package com.client.xvideos.r.ui.niche

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.atom.ButtonUpCircle
import com.client.xvideos.r.ui.ui.atom.TabBarPoints
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun NicheBottomBar(
    niche: NichesInfo,
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    columns: Int,
    onUpClick: () -> Unit
) {
    Column {
        //HorizontalDivider(color = Theme.R.colorBorderGray)
        Row(
            modifier = Modifier
                //.fillMaxWidth()
                //.clip(RoundedCornerShape(50f))
                .height(48.dp)
                //.background(Theme.tabLevel1)
                .padding(horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(2.dp))

                SortByOrder(
                    listOf(Order.TRENDING, Order.TOP, Order.LATEST),
                    currentSort,
                    onSelect = onSortChange,
                    containerColor = Theme.tabLevel0,
                    circle = true
                )

                Spacer(modifier = Modifier.width(4.dp))
//                UrlImage(
//                    niche.thumbnail,
//                    modifier = Modifier
//                        .size(45.dp)
//                        .clip(RoundedCornerShape(50))
//                )
            }
            Spacer(modifier = Modifier.width(4.dp))

//            BasicText(
//                niche.name,
//                modifier = Modifier
//                    .padding(horizontal = 4.dp)
//                    .weight(1f),
//                style = TextStyle(
//                    color = Color.LightGray,
//                    fontSize = 18.sp,
//                    fontFamily = Theme.R.fontFamilyDMsanss
//                ),
//                autoSize = TextAutoSize.StepBased(10.sp, 18.sp)
//            )

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .border(1.dp, Color.DarkGray, CircleShape)
                        .background(Theme.tabLevel0), contentAlignment = Alignment.Center
                ) {
                    TabBarPoints(columns, true)
                }

                Spacer(modifier = Modifier.width(4.dp))
                ButtonUpCircle(44.dp, onUpClick)
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}


@Preview
@Composable
fun NicheBottomBarPreview() {
    XvideosTheme {
        NicheBottomBar(
            niche = NichesInfo(id = "id", name = "Sample Niche", thumbnail = ""),
            currentSort = Order.TRENDING,
            onSortChange = {},
            columns = 2,
            onUpClick = {}
        )
    }
}
