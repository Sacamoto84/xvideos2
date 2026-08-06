package com.client.xvideos.r.ui.search

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Stateful version of the helper menu.
 */
@Composable
fun ExpandMenuHelperContent(
    tags: List<TagInfo>,
    onTagClick: (TagInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExpandMenuHelperContentStateless(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        tags = tags,
        modifier = modifier,
        onTagClick = {
            onTagClick(it)
            expanded = false
        }
    )
}

/**
 * Stateless version for better optimization and previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandMenuHelperContentStateless(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tags: List<TagInfo>,
    modifier: Modifier = Modifier,
    onTagClick: (TagInfo) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        // Anchor part
        Box(
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier
                    .width(24.dp)
                    .height(46.dp)
            )
        }

        // Dropdown part
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .border(1.dp, Theme.tabLevel3, RoundedCornerShape(16.dp)),
            containerColor = Theme.tabLevel2,
            shape = RoundedCornerShape(16.dp)
        ) {
            TagGrid(
                tags = tags,
                onTagClick = onTagClick
            )
        }
    }
}

/**
 * Optimized grid of tags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagGrid(
    tags: List<TagInfo>,
    onTagClick: (TagInfo) -> Unit
) {
    val sortedTags = remember(tags) {
        tags.sortedByDescending { it.count }.take(200)
    }

    FlowRow(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxSize(),
        maxItemsInEachRow = 10
    ) {
        sortedTags.forEach { tag ->
            TagChip(
                tag = tag,
                onClick = { onTagClick(tag) }
            )
        }
    }
}

/**
 * Individual tag chip.
 */
@Composable
private fun TagChip(
    tag: TagInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(25))
            .border(1.dp, Theme.R.colorTextGray, RoundedCornerShape(25))
            .background(Theme.tabLevel1)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = tag.name,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

// --- PREVIEWS ---

@Preview(name = "Tag Chip", showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun PreviewTagChip() {
    XvideosTheme {
        Surface(color = Theme.R.colorCommonBackground, modifier = Modifier.padding(16.dp)) {
            TagChip(tag = TagInfo("Example Tag", 1000), onClick = {})
        }
    }
}

@Preview(name = "Collapsed Helper", showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun PreviewHelperCollapsed() {
    XvideosTheme {
        Surface(color = Theme.R.colorCommonBackground) {
            ExpandMenuHelperContentStateless(
                expanded = false,
                onExpandedChange = {},
                tags = emptyList(),
                onTagClick = {}
            )
        }
    }
}

@Preview(name = "Expanded Helper", showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun PreviewHelperExpanded() {
    val sampleTags = List(15) { TagInfo("Tag $it", (100 - it).toLong()) }
    XvideosTheme {
        Surface(color = Theme.R.colorCommonBackground) {
            Column(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                ExpandMenuHelperContentStateless(
                    expanded = true,
                    onExpandedChange = {},
                    tags = sampleTags,
                    onTagClick = {}
                )
            }
        }
    }
}
