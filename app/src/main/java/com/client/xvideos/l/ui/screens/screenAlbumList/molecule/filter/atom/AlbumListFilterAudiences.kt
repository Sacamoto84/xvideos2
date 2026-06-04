package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AudiencesType
import com.client.xvideos.l.net.graphQl.Audience
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import com.client.xvideos.l.theme.ThemeL

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

    Column(modifier = Modifier.fillMaxWidth().background(palette.surface)) {
        AudienceSelectionChip(
            title = if (isAllSelected) "All audiences" else audiences
                .filter { it.id in selectedIds }
                .joinToString { it.title },
            selected = true,
            onClick = {
                onChange(filter.copy(audienceIds = encodeAudienceIds(allIds, audiences)))
            }
        )

        DisclosureLayout("Audiences") {
            LazyColumn(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                    .background(palette.panelBlack)
                    .padding(vertical = 4.dp)
            ) {
                item {
                    AudienceOptionRow(
                        title = "All audiences",
                        selected = isAllSelected,
                        onClick = {
                            onChange(filter.copy(audienceIds = encodeAudienceIds(allIds, audiences)))
                        }
                    )
                }

                items(audiences) { item ->
                    val selected = !isAllSelected && item.id in selectedIds
                    AudienceOptionRow(
                        title = item.title,
                        selected = selected,
                        onClick = {
                            val nextIds = when {
                                isAllSelected -> setOf(item.id)
                                item.id in selectedIds && selectedIds.size > 1 -> selectedIds - item.id
                                item.id in selectedIds -> allIds
                                else -> selectedIds + item.id
                            }
                            onChange(filter.copy(audienceIds = encodeAudienceIds(nextIds, audiences)))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudienceSelectionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    Text(
        title,
        color = if (selected) palette.selectedText else palette.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = ThemeL.Type.bodyLarge.copy(
            color = if (selected) palette.selectedText else palette.textPrimary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
            .then(StyleGenresTags.modifierSelectTextItem)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun AudienceOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val borderColor = if (selected) palette.selectedBorder else palette.border
    val backgroundColor = if (selected) palette.selected else palette.panelBlack
    val textColor = if (selected) palette.selectedText else palette.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ThemeL.Type.rowTitle.copy(color = textColor, fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = palette.selectedBorder
            )
        }
    }
}

private fun parseAudienceIds(value: String): Set<String> {
    return Regex("""\+([^+-]+)""")
        .findAll(value)
        .map { it.groupValues[1] }
        .toSet()
}

private fun encodeAudienceIds(ids: Set<String>, audiences: List<Audience>): String {
    return audiences
        .map { it.id }
        .filter { it in ids }
        .joinToString(separator = "") { "+$it" }
}

private fun fallbackAudiences(): List<Audience> {
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
