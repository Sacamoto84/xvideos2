package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AudiencesType
import com.client.xvideos.l.net.graphQl.Audience
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow

private val DROPDOWN_SHAPE = RoundedCornerShape(6.dp)
private val DROPDOWN_MIN_WIDTH = 160.dp
private val DROPDOWN_MAX_WIDTH = 220.dp
private val DROPDOWN_HEIGHT = 43.dp
private val DROPDOWN_BORDER_WIDTH = 1.dp
private val DROPDOWN_HORIZONTAL_PADDING = 8.dp
private val ROW_HORIZONTAL_PADDING = 4.dp
private const val TITLE_AUDIENCES = "Audiences"
private const val TEXT_ALL_AUDIENCES = "All audiences"

@Composable
fun AlbumListFilterAudiences(
    filter: AlbumListFilter,
    modifier: Modifier = Modifier,
    onChange: (AlbumListFilter) -> Unit,
) {
    val palette = StyleGenresTags.Palette
    val mediaCategories by mediaCategoriesFlow.collectAsStateWithLifecycle()
    val audiences = mediaCategories?.audiences?.takeIf { it.isNotEmpty() } ?: fallbackAudiences()
    val allIds = remember(audiences) { audiences.map { it.id }.toSet() }
    val selectedIds = remember(filter.audienceIds, allIds) {
        parseAudienceIds(filter.audienceIds).filter { it in allIds }.toSet().ifEmpty { allIds }
    }
    val isAllSelected = remember(selectedIds, allIds) { selectedIds.containsAll(allIds) }

    var showDialog by remember { mutableStateOf(false) }

    val summaryText = remember(isAllSelected, audiences, selectedIds) {
        if (isAllSelected) {
            TEXT_ALL_AUDIENCES
        } else {
            audiences.filter { it.id in selectedIds }.joinToString { it.title }
        }
    }

    val titleStyle = remember(palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
    }
    val normalValueStyle = remember(palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
    val selectedValueStyle = remember(palette.selectedText) {
        Theme.L.Type.rowTitle.copy(
            color = palette.selectedText,
            fontWeight = FontWeight.Bold
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = TITLE_AUDIENCES,
            style = titleStyle
        )

        Row(
            modifier = Modifier
                .widthIn(min = DROPDOWN_MIN_WIDTH, max = DROPDOWN_MAX_WIDTH)
                .height(DROPDOWN_HEIGHT)
                .clip(DROPDOWN_SHAPE)
                .border(DROPDOWN_BORDER_WIDTH, palette.border, DROPDOWN_SHAPE)
                .background(palette.field)
                .clickable { showDialog = true }
                .padding(horizontal = DROPDOWN_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = summaryText,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isAllSelected) normalValueStyle else selectedValueStyle
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }

    if (showDialog) {
        AlbumFilterAudiencesDialog(
            audiences = audiences,
            selectedIds = selectedIds,
            isAllSelected = isAllSelected,
            onToggleAudience = { item ->
                val nextIds = when {
                    isAllSelected -> setOf(item.id)
                    item.id in selectedIds && selectedIds.size > 1 -> selectedIds - item.id
                    item.id in selectedIds -> allIds
                    else -> selectedIds + item.id
                }
                onChange(filter.copy(audienceIds = encodeAudienceIds(nextIds, audiences)))
            },
            onSelectAll = {
                onChange(filter.copy(audienceIds = encodeAudienceIds(allIds, audiences)))
            },
            onDismiss = { showDialog = false }
        )
    }
}

private val AUDIENCE_ID_REGEX = Regex("""\+([^+-]+)""")

internal fun parseAudienceIds(value: String): Set<String> {
    return AUDIENCE_ID_REGEX
        .findAll(value)
        .map { it.groupValues[1] }
        .toSet()
}

internal fun encodeAudienceIds(ids: Set<String>, audiences: List<Audience>): String {
    return audiences
        .map { it.id }
        .filter { it in ids }
        .joinToString(separator = "") { "+$it" }
}

internal fun fallbackAudiences(): List<Audience> {
    return AudiencesType.entries.map {
        Audience(
            id = it.id.toString(),
            title = it.title,
            description = it.description,
            posterUrl = it.posterUrl,
            url = it.url
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
fun AlbumListFilterAudiencesPreview() {
    var filter by remember { mutableStateOf(AlbumListFilter(audienceIds = "+2+3")) }
    AlbumListFilterAudiences(
        filter = filter,
        onChange = { filter = it }
    )
}
