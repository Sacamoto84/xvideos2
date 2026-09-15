package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse

@Composable
fun AlbumFilterGenresDialog(
    genresPlus: List<FilterGenre>,
    genresMinus: List<FilterGenre>,
    selectableGenres: List<FilterGenre>,
    genreCounts: List<AlbumListFilterGenreCountResponse>?,
    onAddPlus: (FilterGenre) -> Unit,
    onAddMinus: (FilterGenre) -> Unit,
    onRemovePlus: (FilterGenre) -> Unit,
    onRemoveMinus: (FilterGenre) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp.coerceIn(240.dp, 520.dp)

    val genreCountByTitle = remember(genreCounts) {
        genreCounts?.associate { it.term to it.count } ?: emptyMap()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                .background(palette.surface)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Genres",
                        color = palette.textPrimary,
                        style = Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textSecondary
                        )
                    }
                }

                // Active selections (chips bar)
                if (genresPlus.isNotEmpty() || genresMinus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GenreSelectedChipsBar(
                        genresPlus = genresPlus,
                        genresMinus = genresMinus,
                        onRemovePlus = onRemovePlus,
                        onRemoveMinus = onRemoveMinus
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Available genres list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .background(palette.panelBlack)
                        .padding(vertical = 4.dp)
                ) {
                    if (selectableGenres.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All genres selected",
                                    color = palette.textSecondary,
                                    style = Theme.L.Type.rowTitle
                                )
                            }
                        }
                    }

                    items(selectableGenres, key = { it.title }) { item ->
                        SelectableGenreRow(
                            item = item,
                            count = genreCountByTitle[item.title],
                            onAddPlus = { onAddPlus(item) },
                            onAddMinus = { onAddMinus(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreSelectedChipsBar(
    genresPlus: List<FilterGenre>,
    genresMinus: List<FilterGenre>,
    onRemovePlus: (FilterGenre) -> Unit,
    onRemoveMinus: (FilterGenre) -> Unit
) {
    val palette = StyleGenresTags.Palette
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(genresPlus, key = { "plus_${it.title}" }) { genre ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.selectedBorder, RoundedCornerShape(6.dp))
                    .background(palette.selected)
                    .clickable { onRemovePlus(genre) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ ${genre.title}",
                    color = palette.selectedText,
                    style = Theme.L.Type.rowValue.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = palette.selectedBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        items(genresMinus, key = { "minus_${it.title}" }) { genre ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.excludedBorder, RoundedCornerShape(6.dp))
                    .background(palette.excluded)
                    .clickable { onRemoveMinus(genre) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val s = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) { append("NOT") }
                    append(" ${genre.title}")
                }
                Text(
                    text = s,
                    color = palette.excludedText,
                    style = Theme.L.Type.rowValue.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = palette.excludedBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectableGenreRow(
    item: FilterGenre,
    count: Int?,
    onAddPlus: () -> Unit,
    onAddMinus: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Include ${item.title}",
                tint = palette.selectedBorder,
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.selectedBorder, RoundedCornerShape(6.dp))
                    .background(palette.field)
                    .clickable { onAddPlus() }
                    .padding(6.dp)
            )

            Spacer(Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Exclude ${item.title}",
                tint = palette.excludedBorder,
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.excludedBorder, RoundedCornerShape(6.dp))
                    .background(palette.field)
                    .clickable { onAddMinus() }
                    .padding(6.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = item.title,
                color = palette.textPrimary,
                style = Theme.L.Type.rowTitle.copy(
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (count != null) {
            Text(
                text = count.toString(),
                color = palette.textSecondary,
                style = Theme.L.Type.rowTitle.copy(
                    color = palette.textSecondary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}
