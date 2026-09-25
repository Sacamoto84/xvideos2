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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

private val TAG_CONTAINER_SHAPE = RoundedCornerShape(12.dp)
private val TAG_CHIP_SHAPE = RoundedCornerShape(6.dp)

private val TAG_BORDER_WIDTH = 1.dp
private val TAG_CONTAINER_PADDING = 6.dp
private val TAG_CONTAINER_MAX_HEIGHT = 160.dp
private val TAG_CHIP_HEIGHT = 28.dp
private val TAG_CHIP_HORIZONTAL_PADDING = 3.dp
private val TAG_CHIP_VERTICAL_PADDING = 2.dp
private val TAG_CHIP_CONTENT_PADDING = 8.dp
private val TAG_TOGGLE_START_PADDING = 8.dp
private val TAG_TOGGLE_END_PADDING = 4.dp
private val TAG_TOGGLE_ICON_SIZE = 18.dp

private val TAG_FONT_SIZE = 13.sp
private val TAG_TOGGLE_FONT_SIZE = 12.sp

private val COLOR_WHITE = Color.White
private val ARROW_DROP_DOWN_ICON = Icons.Default.ArrowDropDown
private val ALIGNMENT_CENTER = Alignment.Center
private val ALIGNMENT_CENTER_VERTICALLY = Alignment.CenterVertically

private val TAG_CHIP_TEXT_STYLE = TextStyle(
    color = COLOR_WHITE,
    fontSize = TAG_FONT_SIZE,
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
)

private val TAG_TOGGLE_TEXT_STYLE = TextStyle(
    color = COLOR_WHITE,
    fontSize = TAG_TOGGLE_FONT_SIZE,
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
)

private const val ROTATION_COLLAPSED = 0f
private const val ROTATION_EXPANDED = 180f

private val TAG_CHIP_BASE_MODIFIER = Modifier
    .padding(horizontal = TAG_CHIP_HORIZONTAL_PADDING, vertical = TAG_CHIP_VERTICAL_PADDING)
    .height(TAG_CHIP_HEIGHT)
    .clip(TAG_CHIP_SHAPE)
    .background(TAG_BG_COLOR)
    .border(TAG_BORDER_WIDTH, TAG_BORDER_COLOR, TAG_CHIP_SHAPE)

private val TAG_CHIP_FULL_MODIFIER = TAG_CHIP_BASE_MODIFIER
    .padding(horizontal = TAG_CHIP_CONTENT_PADDING)

private val TAG_TOGGLE_BASE_MODIFIER = Modifier
    .padding(horizontal = TAG_CHIP_HORIZONTAL_PADDING, vertical = TAG_CHIP_VERTICAL_PADDING)
    .height(TAG_CHIP_HEIGHT)
    .clip(TAG_CHIP_SHAPE)
    .background(TAG_ACTION_BG_COLOR)
    .border(TAG_BORDER_WIDTH, TAG_ACTION_BORDER_COLOR, TAG_CHIP_SHAPE)

private val TAG_TOGGLE_FULL_MODIFIER = TAG_TOGGLE_BASE_MODIFIER
    .padding(start = TAG_TOGGLE_START_PADDING, end = TAG_TOGGLE_END_PADDING)

private val TAG_CONTAINER_EXPANDED_MODIFIER = Modifier
    .clip(TAG_CONTAINER_SHAPE)
    .background(TAG_CONTAINER_EXPANDED_BG)
    .border(TAG_BORDER_WIDTH, TAG_BORDER_COLOR, TAG_CONTAINER_SHAPE)
    .padding(horizontal = TAG_CONTAINER_PADDING, vertical = TAG_CONTAINER_PADDING)
    .heightIn(max = TAG_CONTAINER_MAX_HEIGHT)

private val TAG_TOGGLE_ICON_MODIFIER = Modifier.size(TAG_TOGGLE_ICON_SIZE)
private val TAG_TOGGLE_ICON_ROTATED_MODIFIER = TAG_TOGGLE_ICON_MODIFIER.rotate(ROTATION_EXPANDED)
private val TAG_TOGGLE_ICON_DEFAULT_MODIFIER = TAG_TOGGLE_ICON_MODIFIER.rotate(ROTATION_COLLAPSED)
private val FLOW_ROW_VERTICAL_ARRANGEMENT = Arrangement.Center
private val FLOW_ROW_HORIZONTAL_ARRANGEMENT = Arrangement.Start

private const val CD_EXPAND_TAGS = "Развернуть теги"
private const val CD_COLLAPSE_TAGS = "Свернуть теги"
private const val TEXT_COLLAPSE = "Свернуть"
private const val PREFIX_PLUS = "+"

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
    val estimatedCapacity = tags.mainUploader.size + tags.pornstars.size + tags.tags.size
    val allItems = ArrayList<TagItem>(estimatedCapacity)
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

    val onExpandTags = remember { { isExpanded = true } }
    val onCollapseTags = remember { { isExpanded = false } }

    val scrollState = rememberScrollState()
    val containerModifier = if (tagsState.isExpanded) {
        remember(scrollState) { TAG_CONTAINER_EXPANDED_MODIFIER.verticalScroll(scrollState) }
    } else {
        Modifier
    }

    val baseModifier = if (modifier == Modifier) containerModifier else modifier.then(containerModifier)

    Box(
        modifier = baseModifier.animateContentSize()
    ) {
        FlowRow(
            verticalArrangement = FLOW_ROW_VERTICAL_ARRANGEMENT,
            horizontalArrangement = FLOW_ROW_HORIZONTAL_ARRANGEMENT,
        ) {
            tagsState.visibleItems.forEach { item ->
                key("${item::class.simpleName}_${item.name}") {
                    when (item) {
                        is TagItem.Channel -> {
                            val handleChannelClick = remember(item.model.name, onClick) {
                                { onClick(item.model.name) }
                            }
                            ScreenItemTagsModelPornostars(
                                text = item.model.name,
                                color = TAG_CHANNEL_COLOR,
                                count = item.model.count,
                                onClick = handleChannelClick,
                            )
                        }
                        is TagItem.Pornstar -> {
                            val handlePornstarClick = remember(item.model.name, onClick) {
                                { onClick(item.model.name) }
                            }
                            ScreenItemTagsModelPornostars(
                                text = item.model.name,
                                color = TAG_PORNSTAR_COLOR,
                                count = item.model.count,
                                onClick = handlePornstarClick,
                            )
                        }
                        is TagItem.Keyword -> {
                            val handleKeywordClick = remember(item.tag, onClick) {
                                { onClick(item.tag) }
                            }
                            TagChip(
                                text = item.tag,
                                onClick = handleKeywordClick,
                            )
                        }
                    }
                }
            }

            if (tagsState.canToggle) {
                if (!tagsState.isExpanded) {
                    TagToggleChip(
                        text = "$PREFIX_PLUS${tagsState.hiddenCount}",
                        isExpanded = false,
                        contentDescription = CD_EXPAND_TAGS,
                        onClick = onExpandTags,
                    )
                } else {
                    TagToggleChip(
                        text = TEXT_COLLAPSE,
                        isExpanded = true,
                        contentDescription = CD_COLLAPSE_TAGS,
                        onClick = onCollapseTags,
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
    modifier: Modifier = Modifier,
) {
    val baseModifier = if (modifier == Modifier) TAG_CHIP_FULL_MODIFIER else modifier.then(TAG_CHIP_FULL_MODIFIER)
    Box(
        modifier = baseModifier.clickable(onClick = onClick),
        contentAlignment = ALIGNMENT_CENTER,
    ) {
        Text(
            text = text,
            style = TAG_CHIP_TEXT_STYLE
        )
    }
}

@Composable
private fun TagToggleChip(
    text: String,
    isExpanded: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseModifier = if (modifier == Modifier) TAG_TOGGLE_FULL_MODIFIER else modifier.then(TAG_TOGGLE_FULL_MODIFIER)
    Row(
        modifier = baseModifier.clickable(onClick = onClick),
        verticalAlignment = ALIGNMENT_CENTER_VERTICALLY,
    ) {
        Text(
            text = text,
            style = TAG_TOGGLE_TEXT_STYLE
        )
        Icon(
            imageVector = ARROW_DROP_DOWN_ICON,
            contentDescription = contentDescription,
            tint = COLOR_WHITE,
            modifier = if (isExpanded) TAG_TOGGLE_ICON_ROTATED_MODIFIER else TAG_TOGGLE_ICON_DEFAULT_MODIFIER,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun TagChipPreview() {
    TagChip(text = "sample_tag", onClick = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun ComposeTagsPreview() {
    ComposeTags(
        tags = TagsModel(
            tags = listOf("tag1", "tag2", "tag3"),
            mainUploader = emptyList(),
            pornstars = emptyList()
        ),
        onClick = {}
    )
}
