package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter

import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.GenreSelectedChipsBar
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.SelectableGenreRow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

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
    val dialogShape = RoundedCornerShape(16.dp)

    val genreCountByTitle = remember(genreCounts) {
        genreCounts?.associate { it.term to it.count } ?: emptyMap()
    }
    val headerStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .clip(dialogShape)
                .border(1.dp, palette.border, dialogShape)
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
                        style = headerStyle,
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

                    items(selectableGenres, key = { it.id.ifBlank { it.title } }) { item ->
                        SelectableGenreRow(
                            item = item,
                            count = genreCountByTitle[item.title],
                            onAddPlus = onAddPlus,
                            onAddMinus = onAddMinus
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AlbumFilterGenresDialogPreview() {
    AlbumFilterGenresDialog(
        genresPlus = listOf(FilterGenre(id = "1", title = "Action")),
        genresMinus = emptyList(),
        selectableGenres = listOf(FilterGenre(id = "2", title = "Comedy")),
        genreCounts = null,
        onAddPlus = {},
        onAddMinus = {},
        onRemovePlus = {},
        onRemoveMinus = {},
        onDismiss = {}
    )
}
