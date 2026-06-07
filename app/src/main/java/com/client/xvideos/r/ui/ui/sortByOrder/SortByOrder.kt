package com.client.xvideos.r.ui.ui.sortByOrder

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.model.Order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortByOrder(
    list: List<Order>,
    selected: Order,
    onSelect: (Order) -> Unit,
    containerColor: Color = Color.Transparent,
    circle : Boolean = false
) {
    if (!list.any { it == selected }) { onSelect(Order.LATEST) }

    var expanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            expanded = it
        },
        modifier = Modifier
            .clip(if (!circle) RoundedCornerShape(8.dp) else RoundedCornerShape(50))
            .background(containerColor)

    )
    {

        Row(
            modifier = Modifier
                .width(100.dp)
                .height(46.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable)

                .border(
                    1.dp, Color(0xFF3A3A3A), if (!circle) RoundedCornerShape(8.dp) else RoundedCornerShape(50))

                .clickable(onClick = { expanded = true }),
            horizontalArrangement = Arrangement.SpaceAround
        ) {

            val text = when (selected) {
                Order.TOP -> "Top"
                Order.LATEST -> "Latest"
                Order.TRENDING -> "Trending"
                Order.FORCE_TEMP -> "Refresh"
                Order.OLDEST -> "Oldest"
                Order.RECENT -> "Recent"
                Order.BEST -> "Best"
                Order.TOP28 -> "Top28"
                //Order.NEW -> "New"
                Order.TOP_WEEK -> "Week"
                Order.TOP_MONTH -> "Month"
                Order.TOP_ALLTIME -> "All time"
                Order.NICHES_SUBSCRIBERS_D -> "Subscribers↓"
                Order.NICHES_POST_D -> "Post↓"
                Order.NICHES_SUBSCRIBERS_A -> "Subscribers↑"
                Order.NICHES_POST_A -> "Post↑"
                Order.NICHES_NAME_A_Z -> "Name A-Z"
                Order.NICHES_NAME_Z_A -> "Name Z-A"
            }

            BasicText(
                text,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                style = TextStyle(
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyDMsanss,
                    fontSize = 18.sp
                ),
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp),
                maxLines = 1
            )

//            Icon(
//                painter = painterResource(R.drawable.arrow_down),
//                contentDescription = "",
//                tint = Color.White,
//                modifier = Modifier
//                    .padding(start = 4.dp, end = 8.dp)
//                    .align(Alignment.CenterVertically)
//                    .size(12.dp)
//                    .rotate(if (expanded) 180f else 0f)
//            )

        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min), containerColor = Color(0xFF090909),
            shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Theme.R.colorBorderGray)
        ) {
            list.forEach { option ->

                DropdownMenuItem(
                    text = {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (option == selected) Color(0xFF222222) else Color.Transparent)
                            //.padding(start = 8.dp)
                            , contentAlignment = Alignment.CenterStart
                        ) {

                            val text = when (option) {
                                Order.TOP -> "Top"
                                Order.LATEST -> "Latest"
                                Order.TRENDING -> "Trending"
                                Order.FORCE_TEMP -> "Refresh"
                                Order.OLDEST -> "Oldest"
                                Order.RECENT -> "Recent"
                                Order.BEST -> "Best"
                                Order.TOP28 -> "Top28"
                                //Order.NEW -> " New"
                                Order.TOP_WEEK -> "Week"
                                Order.TOP_MONTH -> "Month"
                                Order.TOP_ALLTIME -> "All time"
                                Order.NICHES_SUBSCRIBERS_D -> "Subscribers↓"
                                Order.NICHES_POST_D -> "Post↓"
                                Order.NICHES_SUBSCRIBERS_A -> "Subscribers↑"
                                Order.NICHES_POST_A -> "Post↑"
                                Order.NICHES_NAME_A_Z -> "Name A-Z"
                                Order.NICHES_NAME_Z_A -> "Name Z-A"
                            }

//                            Text(
//                                text,
//                                style = TextStyle(
//                                    color = Color.White,
//                                    fontFamily = Theme.R.fontFamilyDMsanss,
//                                    fontSize = 16.sp
//                                ),
//                                modifier = Modifier
//                            )

                            BasicText(
                                text,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = TextStyle(
                                    color = Color.White,
                                    fontFamily = Theme.R.fontFamilyDMsanss,
                                    fontSize = 18.sp
                                ),
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 12.sp,
                                    maxFontSize = 18.sp
                                ),
                                maxLines = 1
                            )

                        }
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSelect(option)
                        expanded = false
                    },

                    )
            }
        }

    }

}