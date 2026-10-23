package com.client.xvideos.x.screens.actresses.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.common.ui.excludedFromAutofill

private val AccentColor = Color(0xFFDE2600)

/**
 * Поле поиска в выпадающем списке фильтра каталога.
 *
 * Свой `BasicTextField`, а не Material3 `TextField`: у того минимальная высота
 * 56dp и отступы по 16dp сверху и снизу, и в строке высотой 44dp текст и
 * подсказка обрезались по вертикали.
 */
@Composable
fun DropdownOptionsSearchField(
    groupType: ActressesIndexDropdownType,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) Color(0xFF1F1F24) else Color(0xFF1A1A1E))
            .drawBehind {
                if (isFocused) {
                    val line = 2.dp.toPx()
                    drawRect(AccentColor, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                }
            }
            .padding(start = 12.dp, end = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = if (groupType == ActressesIndexDropdownType.GEO) "Поиск страны..." else "Поиск типа профиля...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(AccentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .excludedFromAutofill()
            )
        }
        if (searchQuery.isNotBlank()) {
            IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Очистить",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun DropdownOptionsSearchFieldPreview() {
    DropdownOptionsSearchField(
        groupType = ActressesIndexDropdownType.GEO,
        searchQuery = "",
        onSearchQueryChange = {}
    )
}
