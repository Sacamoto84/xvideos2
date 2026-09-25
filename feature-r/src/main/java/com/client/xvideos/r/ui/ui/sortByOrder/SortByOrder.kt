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

private val SHAPE_8 = RoundedCornerShape(8.dp)
private val SHAPE_16 = RoundedCornerShape(16.dp)
private val SHAPE_CIRCLE = RoundedCornerShape(50)
private val BORDER_STROKE_1DP = 1.dp
private val BORDER_COLOR_DEFAULT = Color(0xFF3A3A3A)
private val MENU_CONTAINER_COLOR = Color(0xFF090909)
private val SELECTED_ITEM_BG = Color(0xFF222222)
private val COLOR_WHITE = Color.White
private val COLOR_TRANSPARENT = Color.Transparent
private val ROW_WIDTH = 100.dp
private val ROW_HEIGHT = 46.dp
private val ITEM_HEIGHT = 32.dp
private val PADDING_HORIZONTAL_8 = 8.dp

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

    val boxShape = if (!circle) SHAPE_8 else SHAPE_CIRCLE
    val textStyle = remember {
        TextStyle(
            color = COLOR_WHITE,
            fontFamily = Theme.R.fontFamilyDMsanss,
            fontSize = 18.sp
        )
    }
    val menuBorder = remember { BorderStroke(BORDER_STROKE_1DP, Theme.R.colorBorderGray) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier
            .clip(boxShape)
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .width(ROW_WIDTH)
                .height(ROW_HEIGHT)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable)
                .border(BORDER_STROKE_1DP, BORDER_COLOR_DEFAULT, boxShape)
                .clickable(onClick = onTriggerClick),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BasicText(
                selected.toDisplayName(),
                modifier = Modifier
                    .padding(horizontal = PADDING_HORIZONTAL_8)
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
            containerColor = MENU_CONTAINER_COLOR,
            shape = SHAPE_16,
            border = menuBorder
        ) {
            list.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ITEM_HEIGHT)
                                .clip(SHAPE_8)
                                .background(if (isSelected) SELECTED_ITEM_BG else COLOR_TRANSPARENT),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicText(
                                option.toDisplayName(),
                                modifier = Modifier.padding(horizontal = PADDING_HORIZONTAL_8),
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
