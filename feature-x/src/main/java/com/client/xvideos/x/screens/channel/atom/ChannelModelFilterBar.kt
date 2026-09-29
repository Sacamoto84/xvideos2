package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem

/**
 * Строка фильтрации и поиска по моделям/актрисам канала (или каналам модели).
 *
 * Отображает счётчик доступных видеороликов слева и компактное поле поиска с выпадающим
 * списком моделей, иконками пола и счётчиками справа.
 */
@Composable
fun ChannelModelFilterBar(
    header: ChannelHeaderModel,
    selectedModel: ChannelModelFilterItem?,
    filteredModels: List<ChannelModelFilterItem>,
    searchQuery: String,
    isExpanded: Boolean,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    totalVideos: Int,
    modifier: Modifier = Modifier,
) {
    val isModelProfile = header.isModel
    val placeholderText = if (isModelProfile) "Поиск канала..." else "Поиск модели..."

    val summaryText = if (selectedModel != null) {
        val count = selectedModel.countText.ifBlank { totalVideos.toString() }
        "$count видео с ${selectedModel.displayName}"
    } else {
        val count = header.videoCount.takeIf { it > 0 } ?: totalVideos
        "$count видео"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Текст со счётчиком видео
        Text(
            text = summaryText,
            color = if (selectedModel != null) Color.White else Color(0xFF9E9EA4),
            fontSize = 13.sp,
            fontWeight = if (selectedModel != null) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        // Поле-триггер выпадающего поиска модели
        Box {
            ChannelModelTriggerButton(
                selectedModel = selectedModel,
                isExpanded = isExpanded,
                placeholderText = placeholderText,
                onExpandedChange = onExpandedChange,
                onSelectModel = onSelectModel
            )

            ChannelModelDropdownMenu(
                isExpanded = isExpanded,
                searchQuery = searchQuery,
                placeholderText = placeholderText,
                selectedModel = selectedModel,
                filteredModels = filteredModels,
                onQueryChange = onQueryChange,
                onExpandedChange = onExpandedChange,
                onSelectModel = onSelectModel
            )
        }
    }
}

@Composable
private fun ChannelModelTriggerButton(
    selectedModel: ChannelModelFilterItem?,
    isExpanded: Boolean,
    placeholderText: String,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF16161A))
            .border(
                1.dp,
                if (isExpanded || selectedModel != null) Color(0xFFDE2600) else Color(0xFF333338),
                RoundedCornerShape(6.dp)
            )
            .clickable { onExpandedChange(!isExpanded) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (selectedModel != null) Color(0xFFDE2600) else Color(0xFF888888),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = selectedModel?.displayName ?: placeholderText,
            fontSize = 12.sp,
            color = if (selectedModel != null) Color.White else Color(0xFF8E8E93),
            fontWeight = if (selectedModel != null) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        if (selectedModel != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Сбросить фильтр",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onSelectModel(null) }
            )
        } else {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ChannelModelDropdownMenu(
    isExpanded: Boolean,
    searchQuery: String,
    placeholderText: String,
    selectedModel: ChannelModelFilterItem?,
    filteredModels: List<ChannelModelFilterItem>,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { onExpandedChange(false) },
        modifier = Modifier
            .widthIn(min = 270.dp, max = 320.dp)
            .heightIn(max = 380.dp)
            .background(Color(0xFF18181C))
            .border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(8.dp))
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(placeholderText, fontSize = 12.sp, color = Color(0xFF7E7E86))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFDE2600),
                unfocusedBorderColor = Color(0xFF333338),
                cursorColor = Color(0xFFDE2600),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(48.dp)
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = "Показать все видео",
                    fontSize = 13.sp,
                    fontWeight = if (selectedModel == null) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedModel == null) Color(0xFFDE2600) else Color.White
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (selectedModel == null) Color(0xFFDE2600) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = {
                onSelectModel(null)
                onExpandedChange(false)
            }
        )

        HorizontalDivider(color = Color(0xFF26262B))

        if (filteredModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Ничего не найдено", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            filteredModels.take(60).forEach { model ->
                ChannelModelDropdownItem(
                    model = model,
                    isSelected = selectedModel?.idUser == model.idUser,
                    onSelectModel = onSelectModel,
                    onExpandedChange = onExpandedChange,
                )
            }
        }
    }
}

@Composable
private fun ChannelModelDropdownItem(
    model: ChannelModelFilterItem,
    isSelected: Boolean,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    val iconText = when {
        model.isWoman -> "♀"
        model.isMan -> "♂"
        else -> "📺"
    }
    val iconColor = when {
        model.isWoman -> Color(0xFFE91E63)
        model.isMan -> Color(0xFF2196F3)
        else -> Color(0xFF1E88E5)
    }

    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.displayName,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFFDE2600) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (model.countText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${model.countText} видео)",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        },
        leadingIcon = {
            Text(
                text = iconText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        },
        onClick = {
            onSelectModel(model)
            onExpandedChange(false)
        },
        modifier = if (isSelected) Modifier.background(Color(0xFF281418)) else Modifier
    )
}
