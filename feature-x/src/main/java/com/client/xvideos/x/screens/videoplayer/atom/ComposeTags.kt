package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel

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
    collapsedLimit: Int = 2,
    expandThreshold: Int = 3,
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

@Composable
fun ComposeTags(
    tags: TagsModel,
    modifier: Modifier = Modifier,
    onChannelClick: ((TagsMainUploaderPornstar) -> Unit)? = null,
    onPornstarClick: ((TagsMainUploaderPornstar) -> Unit)? = null,
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
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6141418))
            //.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            //.padding(6.dp)
            .padding(bottom = 2.dp)
            //.heightIn(max = 160.dp)
            .verticalScroll(scrollState)
    } else {
        Modifier
    }

    val baseModifier = if (modifier == Modifier) containerModifier else modifier.then(containerModifier)

    Box(
        modifier = baseModifier.animateContentSize()
    ) {
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Start,
        ) {
            tagsState.visibleItems.forEach { item ->
                key("${item::class.simpleName}_${item.name}") {
                    when (item) {
                        is TagItem.Channel -> {
                            val handleChannelClick = remember(item.model, onChannelClick, onClick) {
                                {
                                    if (onChannelClick != null) {
                                        onChannelClick(item.model)
                                    } else {
                                        onClick(item.model.name)
                                    }
                                }
                            }
                            ScreenItemTagsModelPornostars(
                                icon = "\uE956",
                                text = item.model.name,
                                color = Color(0xFF1E88E5),
                                count = item.model.count,
                                onClick = handleChannelClick,
                            )
                        }
                        is TagItem.Pornstar -> {
                            val handlePornstarClick = remember(item.model, onPornstarClick, onClick) {
                                {
                                    if (onPornstarClick != null) {
                                        onPornstarClick(item.model)
                                    } else {
                                        onClick(item.model.name)
                                    }
                                }
                            }
                            ScreenItemTagsModelPornostars(
                                icon = "\uE9B8",
                                text = item.model.name,
                                color = Color(0xFFDE2600),
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
                        text = "+${tagsState.hiddenCount}",
                        isExpanded = false,
                        contentDescription = "Развернуть теги",
                        onClick = onExpandTags,
                    )
                } else {
                    TagToggleChip(
                        text = "Свернуть",
                        isExpanded = true,
                        contentDescription = "Свернуть теги",
                        onClick = onCollapseTags,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun ComposeTagsPreview() {
    ComposeTags(
        tags = TagsModel(
            tags = listOf("tag1", "tag2", "tag3"),
            mainUploader = listOf(TagsMainUploaderPornstar(name = "channel1", count = "100k")),
            pornstars = listOf(TagsMainUploaderPornstar(name = "pornstar1", count = "100k"))
        ),
        onClick = {}
    )
}
