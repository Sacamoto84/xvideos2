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

@Composable
fun AlbumListFilterAudiences(
    filter: AlbumListFilter,
    onChange: (AlbumListFilter) -> Unit
) {
    val palette = StyleGenresTags.Palette
    val mediaCategories = mediaCategoriesFlow.collectAsStateWithLifecycle().value
    val audiences = mediaCategories?.audiences?.takeIf { it.isNotEmpty() } ?: fallbackAudiences()
    val allIds = audiences.map { it.id }.toSet()
    val selectedIds = parseAudienceIds(filter.audienceIds).filter { it in allIds }.toSet().ifEmpty { allIds }
    val isAllSelected = selectedIds.containsAll(allIds)

    var showDialog by remember { mutableStateOf(false) }

    val summaryText = if (isAllSelected) {
        "All audiences"
    } else {
        audiences.filter { it.id in selectedIds }.joinToString { it.title }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Audiences",
            style = Theme.L.Type.rowTitle.copy(
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        )

        Row(
            modifier = Modifier
                .widthIn(min = 160.dp, max = 220.dp)
                .height(43.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                .background(palette.field)
                .clickable { showDialog = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = summaryText,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Theme.L.Type.rowTitle.copy(
                    color = if (isAllSelected) palette.textPrimary else palette.selectedText,
                    fontWeight = FontWeight.Bold
                )
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

internal fun parseAudienceIds(value: String): Set<String> {
    return Regex("""\+([^+-]+)""")
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
