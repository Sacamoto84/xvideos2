package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow

@Composable
fun AlbumListFilterGenres(
    filter: AlbumListFilter,
    filterGenreStateCount: List<AlbumListFilterGenreCountResponse>?,
    onChange: (AlbumListFilter) -> Unit
) {
    val mediaCategories = mediaCategoriesFlow.collectAsStateWithLifecycle().value

    val filterTerms = remember(filterGenreStateCount) {
        filterGenreStateCount?.map { it.term }?.toSet()
    }

    val genresPlus = remember(filter.genresPlus) { filter.genresPlus.distinctBy { it.title } }
    val genresMinus = remember(filter.genresMinus) { filter.genresMinus.distinctBy { it.title } }

    val allGenres = mediaCategories?.genres ?: emptyList()

    val genresPlusCorrect = remember(allGenres, genresPlus, genresMinus, filterTerms) {
        allGenres.minus(genresPlus.toSet()).minus(genresMinus.toSet())
            .filter { filterTerms?.contains(it.title) == true }
    }
    val palette = StyleGenresTags.Palette

    var showDialog by remember { mutableStateOf(false) }

    val totalSelected = genresPlus.size + genresMinus.size
    val selectorText = if (totalSelected == 0) "Any" else "$totalSelected selected"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        // Trigger row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Genres",
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
                    text = selectorText,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Theme.L.Type.rowTitle.copy(
                        color = if (totalSelected == 0) palette.textPrimary else palette.selectedText,
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

        // Active selection chips in main filter card
        if (genresPlus.isNotEmpty() || genresMinus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                genresPlus.forEach { item ->
                    Row(
                        modifier = Modifier
                            .then(StyleGenresTags.modifierSelectTextItem)
                            .clickable {
                                val nextPlus = genresPlus.toMutableList().apply { remove(item) }
                                onChange(filter.copy(genresPlus = nextPlus))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            color = StyleGenresTags.colorSelectTextItem,
                            style = Theme.L.Type.bodyLarge.copy(
                                color = StyleGenresTags.colorSelectTextItem,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = palette.selectedBorder,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                genresMinus.forEach { item ->
                    val s = buildAnnotatedString {
                        withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) {
                            append("NOT")
                        }
                        append(" ${item.title}")
                    }
                    Row(
                        modifier = Modifier
                            .then(StyleGenresTags.modifierExcludedTextItem)
                            .clickable {
                                val nextMinus = genresMinus.toMutableList().apply { remove(item) }
                                onChange(filter.copy(genresMinus = nextMinus))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = s,
                            color = StyleGenresTags.colorExcludedTextItem,
                            style = Theme.L.Type.bodyLarge.copy(
                                color = StyleGenresTags.colorExcludedTextItem,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = palette.excludedBorder,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlbumFilterGenresDialog(
            genresPlus = genresPlus,
            genresMinus = genresMinus,
            selectableGenres = genresPlusCorrect,
            genreCounts = filterGenreStateCount,
            onAddPlus = { item ->
                onChange(filter.copy(genresPlus = genresPlus + item))
            },
            onAddMinus = { item ->
                onChange(filter.copy(genresMinus = genresMinus + item))
            },
            onRemovePlus = { item ->
                onChange(filter.copy(genresPlus = genresPlus - item))
            },
            onRemoveMinus = { item ->
                onChange(filter.copy(genresMinus = genresMinus - item))
            },
            onDismiss = { showDialog = false }
        )
    }
}
