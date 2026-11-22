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
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.nearestIn

private val SORT_TRIGGER_HEIGHT = 46.dp
private val SORT_ORDER_TEXT_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp)

private fun Order.toDisplayName(): String = when (this) {
    Order.TOP -> "Top"
    Order.LATEST -> "Latest"
    Order.TRENDING -> "Trending"
    Order.HOT -> "Hot"
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

private fun Order.toIconLabel(): String = when (this) {
    Order.TOP_WEEK -> "Топ недели"
    Order.TOP_MONTH, Order.TOP28 -> "Топ месяца"
    Order.TOP -> "Топ за всё время"
    Order.TRENDING -> "В тренде"
    Order.HOT -> "Горячее"
    Order.LATEST -> "Новые"
    Order.OLDEST -> "Старые"
    Order.RELEVANT -> "По релевантности"
    Order.NICHES_SUBSCRIBERS_D -> "Больше подписчиков"
    Order.NICHES_SUBSCRIBERS_A -> "Меньше подписчиков"
    Order.NICHES_POST_D -> "Больше публикаций"
    Order.NICHES_POST_A -> "Меньше публикаций"
    Order.NICHES_NAME_A_Z -> "Название A–Z"
    Order.NICHES_NAME_Z_A -> "Название Z–A"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortByOrder(
    list: List<Order>,
    selected: Order,
    onSelect: (Order) -> Unit,
    containerColor: Color = Color.Transparent,
    circle: Boolean = false,
    orderIcon: (@Composable (Order) -> Unit)? = null,
    showSelectedLabel: Boolean = false,
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
    val menuBorder = if (orderIcon != null) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.26f))
    } else {
        BorderStroke(1.dp, Theme.R.colorBorderGray)
    }

    val sortHint = if (orderIcon != null) "Сортировка · ${selected.toIconLabel()}" else "Изменить сортировку"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier
            .clip(boxShape)
            .background(containerColor)
    ) {
        HintTooltip(text = sortHint) {
            if (orderIcon != null) {
                Row(
                    modifier = Modifier
                        .height(SORT_TRIGGER_HEIGHT)
                        .then(
                            if (showSelectedLabel) Modifier else Modifier
                                .width(SORT_TRIGGER_HEIGHT)
                                .semantics { contentDescription = sortHint }
                        )
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .border(1.dp, Color(0xFF3A3A3A), boxShape)
                        .padding(horizontal = if (showSelectedLabel) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    orderIcon(selected)
                    if (showSelectedLabel) {
                        BasicText(
                            selected.toIconLabel(),
                            style = textStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .width(100.dp)
                        .height(SORT_TRIGGER_HEIGHT)
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
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = if (orderIcon == null) Modifier.width(IntrinsicSize.Min) else Modifier,
            matchAnchorWidth = orderIcon == null,
            containerColor = if (orderIcon != null) Theme.tabLevel2 else Color(0xFF090909),
            shape = RoundedCornerShape(16.dp),
            border = menuBorder,
            shadowElevation = if (orderIcon != null) 8.dp else MenuDefaults.ShadowElevation
        ) {
            list.forEach { option ->
                DropdownMenuItem(
                    text = {
                        SortOrderOptionLabel(option, option == selected, orderIcon != null, textStyle)
                    },
                    leadingIcon = if (orderIcon == null) null else { { orderIcon(option) } },
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

@Composable
private fun SortOrderOptionLabel(order: Order, selected: Boolean, withIcon: Boolean, textStyle: TextStyle) {
    if (withIcon) {
        BasicText(
            order.toIconLabel(),
            style = textStyle.copy(
                color = if (selected) Color(0xFFFF6A87) else Color.White,
                fontSize = 14.sp
            )
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color(0xFF222222) else Color.Transparent),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicText(
                order.toDisplayName(),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = textStyle,
                autoSize = SORT_ORDER_TEXT_AUTO_SIZE,
                maxLines = 1
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SortByOrderPreview() {
    SortByOrder(
        list = listOf(Order.TOP, Order.LATEST),
        selected = Order.TOP,
        onSelect = {}
    )
}
