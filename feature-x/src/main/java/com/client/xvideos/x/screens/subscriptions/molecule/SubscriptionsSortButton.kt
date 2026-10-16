package com.client.xvideos.x.screens.subscriptions.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.screens.subscriptions.XSubscriptionsOrder

/**
 * Значок сортировки списка подписок с меню порядков — в стиле меню фильтра
 * моделей канала X. Значок красный, пока выбран не порядок по умолчанию.
 */
@Composable
fun SubscriptionsSortButton(
    order: XSubscriptionsOrder,
    onOrderChange: (XSubscriptionsOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Порядок: ${order.title}",
                tint = if (order == XSubscriptionsOrder.NEWEST) Color(0xFFBDBDBD) else Color(0xFFDE2600)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF18181C))
                .border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(8.dp))
        ) {
            XSubscriptionsOrder.entries.forEach { item ->
                key(item.name) {
                    val selected = item == order
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item.title,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFFDE2600) else Color.White
                            )
                        },
                        onClick = {
                            expanded = false
                            onOrderChange(item)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SubscriptionsSortButtonPreview() {
    SubscriptionsSortButton(order = XSubscriptionsOrder.NEWEST, onOrderChange = {})
}
