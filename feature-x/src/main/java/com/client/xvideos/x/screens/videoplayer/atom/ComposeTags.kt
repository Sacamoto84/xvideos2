package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel

internal const val DEFAULT_COLLAPSED_TAGS_LIMIT = 2
internal const val DEFAULT_TAGS_EXPAND_THRESHOLD = 3

private val TAG_CHANNEL_COLOR = Color(0xFF1E88E5)
private val TAG_PORNSTAR_COLOR = Color(0xFFDE2600)
private val TAG_BG_COLOR = Color(0xCC26262B)
private val TAG_BORDER_COLOR = Color(0x33FFFFFF)
private val TAG_ACTION_BG_COLOR = Color(0xDD34343B)
private val TAG_ACTION_BORDER_COLOR = Color(0x55FFFFFF)
private val TAG_CONTAINER_EXPANDED_BG = Color(0xE6141418)

sealed interface TagItem {
    val name: String

    data class Channel(val model: TagsMainUploaderPornstar) : TagItem {
        override val name: String get() = model.name
    }

    data class Pornstar(val model: TagsMainUploaderPornstar) : TagItem {
        override val name: String get() = model.name
    }

    data class Keyword(val tag: String) : TagItem {
        override val name: String get() = tag
    }
}

data class VisibleTagsState(
    val visibleItems: List<TagItem>,
    val hiddenCount: Int,
    val canToggle: Boolean,
    val isExpanded: Boolean,
)

/**
 * Вычисляет состояние отображения тегов для свернутого и развернутого режимов.
 */
fun computeVisibleTags(
    tags: TagsModel,
    isExpanded: Boolean,
    collapsedLimit: Int = DEFAULT_COLLAPSED_TAGS_LIMIT,
    expandThreshold: Int = DEFAULT_TAGS_EXPAND_THRESHOLD,
): VisibleTagsState {
    val allItems = ArrayList<TagItem>()
    for (channel in tags.mainUploader) {
        if (channel.name.isNotBlank()) {
            allItems.add(TagItem.Channel(channel))
        }
    }
    for (star in tags.pornstars) {
        if (star.name.isNotBlank()) {
            allItems.add(TagItem.Pornstar(star))
        }
    }
    val existingNames = allItems.map { it.name.trim().lowercase() }.toSet()
    val sortedTags = tags.tags
        .map { it.trim() }
        .filter { it.isNotBlank() && it.lowercase() !in existingNames }
        .distinct()
        .sorted()
    for (tag in sortedTags) {
        allItems.add(TagItem.Keyword(tag))
    }

    val total = allItems.size
    val threshold = expandThreshold.coerceAtLeast(0)
    val canToggle = total > threshold
    val limit = collapsedLimit.coerceAtLeast(1)

    return if (!canToggle) {
        VisibleTagsState(
            visibleItems = allItems,
            hiddenCount = 0,
            canToggle = false,
            isExpanded = isExpanded,
        )
    } else if (isExpanded) {
        VisibleTagsState(
            visibleItems = allItems,
            hiddenCount = 0,
            canToggle = true,
            isExpanded = true,
        )
    } else {
        val visible = allItems.take(limit)
        val hidden = (total - limit).coerceAtLeast(0)
        VisibleTagsState(
            visibleItems = visible,
            hiddenCount = hidden,
            canToggle = true,
            isExpanded = false,
        )
    }
}

/**
 * Отобразить список каналов, порноактрис и тегов с поддержкой сворачивания/разворачивания.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeTags(
    tags: TagsModel,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
) {
    var isExpanded by rememberSaveable(tags) { mutableStateOf(false) }

    val tagsState = remember(tags, isExpanded) {
        computeVisibleTags(tags, isExpanded)
    }

    if (tagsState.visibleItems.isEmpty()) return

    Box(
        modifier = modifier
            .animateContentSize()
            .then(
                if (tagsState.isExpanded) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TAG_CONTAINER_EXPANDED_BG)
                        .border(1.dp, TAG_BORDER_COLOR, RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
            )
    ) {
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Start,
        ) {
            tagsState.visibleItems.forEach { item ->
                when (item) {
                    is TagItem.Channel -> {
                        ScreenItemTagsModelPornostars(
                            text = item.model.name,
                            color = TAG_CHANNEL_COLOR,
                            count = item.model.count,
                            onClick = { onClick(item.model.name) },
                        )
                    }
                    is TagItem.Pornstar -> {
                        ScreenItemTagsModelPornostars(
                            text = item.model.name,
                            color = TAG_PORNSTAR_COLOR,
                            count = item.model.count,
                            onClick = { onClick(item.model.name) },
                        )
                    }
                    is TagItem.Keyword -> {
                        TagChip(
                            text = item.tag,
                            onClick = { onClick(item.tag) },
                        )
                    }
                }
            }

            if (tagsState.canToggle) {
                if (!tagsState.isExpanded) {
                    TagToggleChip(
                        text = "+${tagsState.hiddenCount}",
                        isExpanded = false,
                        contentDescription = "Развернуть теги",
                        onClick = { isExpanded = true },
                    )
                } else {
                    TagToggleChip(
                        text = "Свернуть",
                        isExpanded = true,
                        contentDescription = "Свернуть теги",
                        onClick = { isExpanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TAG_BG_COLOR)
            .border(1.dp, TAG_BORDER_COLOR, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TagToggleChip(
    text: String,
    isExpanded: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TAG_ACTION_BG_COLOR)
            .border(1.dp, TAG_ACTION_BORDER_COLOR, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .rotate(if (isExpanded) 180f else 0f),
        )
    }
}
