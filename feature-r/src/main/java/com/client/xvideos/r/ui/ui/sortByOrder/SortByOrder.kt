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
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.client.xvideos.r.model.nearestIn

private val SORT_ORDER_TEXT_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp)

private fun Order.toDisplayName(): String = when (this) {
    Order.TOP -> "Top"
    Order.LATEST -> "Latest"
    Order.TRENDING -> "Trending"
    Order.FORCE_TEMP -> "Refresh"
    Order.OLDEST -> "Oldest"
    Order.TOP28 -> "Top28"
    Order.RELEVANT -> "Relevant"
    Order.TOP_WEEK -> "Week"
    Order.TOP_MONTH -> "Month"
    Order.NICHES_SUBSCRIBERS_D -> "Subscribers↓"
    Order.NICHES_POST_D -> "Post↓"
    Order.NICHES_SUBSCRIBERS_A -> "Subscribers↑"
    Order.NICHES_POST_A -> "Post↑"
    Order.NICHES_NAME_A_Z -> "Name A-Z"
    Order.NICHES_NAME_Z_A -> "Name Z-A"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortByOrder(
    list: List<Order>,
    selected: Order,
    onSelect: (Order) -> Unit,
    containerColor: Color = Color.Transparent,
    circle : Boolean = false
) {
    LaunchedEffect(list, selected) {
        val nearest = selected.nearestIn(list)
        if (nearest != selected) onSelect(nearest)
    }

    var expanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val onExpandedChange: (Boolean) -> Unit = remember(haptic) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            expanded = it
        }
    }
    val onDismissRequest: () -> Unit = remember {
        { expanded = false }
    }
    val onTriggerClick: () -> Unit = remember {
        { expanded = true }
    }

    val boxShape = if (!circle) RoundedCornerShape(8.dp) else RoundedCornerShape(50)
    val textStyle = remember {
        TextStyle(
            color = Color.White,
            fontFamily = Theme.R.fontFamilyDMsanss,
            fontSize = 18.sp
        )
    }
    val menuBorder = remember { BorderStroke(1.dp, Theme.R.colorBorderGray) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier
            .clip(boxShape)
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .width(100.dp)
                .height(46.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable)
                .border(1.dp, Color(0xFF3A3A3A), boxShape)
                .clickable(onClick = onTriggerClick),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BasicText(
                selected.toDisplayName(),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                style = textStyle,
                autoSize = SORT_ORDER_TEXT_AUTO_SIZE,
                maxLines = 1
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Color(0xFF090909),
            shape = RoundedCornerShape(16.dp),
            border = menuBorder
        ) {
            list.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF222222) else Color.Transparent),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicText(
                                option.toDisplayName(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = textStyle,
                                autoSize = SORT_ORDER_TEXT_AUTO_SIZE,
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
