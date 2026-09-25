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
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import androidx.compose.runtime.key
import androidx.compose.ui.tooling.preview.Preview

private val DROPDOWN_CORNER = 6.dp
private val DROPDOWN_SHAPE = RoundedCornerShape(DROPDOWN_CORNER)
private val DROPDOWN_MIN_WIDTH = 160.dp
private val DROPDOWN_MAX_WIDTH = 220.dp
private val DROPDOWN_HEIGHT = 43.dp
private val DROPDOWN_BORDER_WIDTH = 1.dp
private val DROPDOWN_HORIZONTAL_PADDING = 8.dp
private val ROW_HORIZONTAL_PADDING = 4.dp
private val CHIPS_TOP_SPACING = 6.dp
private val REMOVE_ICON_SIZE = 16.dp
private const val TITLE_GENRES = "Genres"
private const val TEXT_ANY = "Any"
private const val TEXT_SELECTED_SUFFIX = " selected"
private const val CD_REMOVE = "Remove"
private const val PREFIX_NOT = "NOT"

@Composable
fun AlbumListFilterGenres(
    filter: AlbumListFilter,
    filterGenreStateCount: List<AlbumListFilterGenreCountResponse>?,
    modifier: Modifier = Modifier,
    onChange: (AlbumListFilter) -> Unit
) {
    val mediaCategories by mediaCategoriesFlow.collectAsStateWithLifecycle()

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
    val selectorText = if (totalSelected == 0) TEXT_ANY else "$totalSelected$TEXT_SELECTED_SUFFIX"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        GenresTriggerRow(
            selectorText = selectorText,
            totalSelected = totalSelected,
            onClick = { showDialog = true }
        )

        if (genresPlus.isNotEmpty() || genresMinus.isNotEmpty()) {
            ActiveGenresChips(
                genresPlus = genresPlus,
                genresMinus = genresMinus,
                onRemovePlus = { item ->
                    val nextPlus = genresPlus.toMutableList().apply { remove(item) }
                    onChange(filter.copy(genresPlus = nextPlus))
                },
                onRemoveMinus = { item ->
                    val nextMinus = genresMinus.toMutableList().apply { remove(item) }
                    onChange(filter.copy(genresMinus = nextMinus))
                }
            )
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

@Composable
private fun GenresTriggerRow(
    selectorText: String,
    totalSelected: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val titleStyle = remember(Theme.L.Type.rowTitle, palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
    }
    val normalValueStyle = remember(Theme.L.Type.rowTitle, palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
    val selectedValueStyle = remember(Theme.L.Type.rowTitle, palette.selectedText) {
        Theme.L.Type.rowTitle.copy(
            color = palette.selectedText,
            fontWeight = FontWeight.Bold
        )
    }
    val dropdownBoxModifier = remember(palette.border, palette.field) {
        Modifier
            .widthIn(min = DROPDOWN_MIN_WIDTH, max = DROPDOWN_MAX_WIDTH)
            .height(DROPDOWN_HEIGHT)
            .clip(DROPDOWN_SHAPE)
            .border(DROPDOWN_BORDER_WIDTH, palette.border, DROPDOWN_SHAPE)
            .background(palette.field)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = TITLE_GENRES,
            style = titleStyle
        )

        Row(
            modifier = dropdownBoxModifier
                .clickable { onClick() }
                .padding(horizontal = DROPDOWN_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectorText,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (totalSelected == 0) normalValueStyle else selectedValueStyle
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

@Composable
private fun ActiveGenresChips(
    genresPlus: List<FilterGenre>,
    genresMinus: List<FilterGenre>,
    onRemovePlus: (FilterGenre) -> Unit,
    onRemoveMinus: (FilterGenre) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val chipTextStyle = remember(Theme.L.Type.bodyLarge) {
        Theme.L.Type.bodyLarge.copy(
            color = StyleGenresTags.colorSelectTextItem,
            fontWeight = FontWeight.Bold
        )
    }
    val excludedChipTextStyle = remember(Theme.L.Type.bodyLarge) {
        Theme.L.Type.bodyLarge.copy(
            color = StyleGenresTags.colorExcludedTextItem,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(CHIPS_TOP_SPACING))
    Column(modifier = modifier.fillMaxWidth()) {
        genresPlus.forEach { item ->
            key(item.title) {
                Row(
                    modifier = Modifier
                        .then(StyleGenresTags.modifierSelectTextItem)
                        .clickable { onRemovePlus(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        color = StyleGenresTags.colorSelectTextItem,
                        style = chipTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = CD_REMOVE,
                        tint = palette.selectedBorder,
                        modifier = Modifier.size(REMOVE_ICON_SIZE)
                    )
                }
            }
        }

        genresMinus.forEach { item ->
            key(item.title) {
                val annotatedText = remember(item.title, palette.excludedBorder) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) {
                            append(PREFIX_NOT)
                        }
                        append(" ${item.title}")
                    }
                }
                Row(
                    modifier = Modifier
                        .then(StyleGenresTags.modifierExcludedTextItem)
                        .clickable { onRemoveMinus(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = annotatedText,
                        color = StyleGenresTags.colorExcludedTextItem,
                        style = excludedChipTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = CD_REMOVE,
                        tint = palette.excludedBorder,
                        modifier = Modifier.size(REMOVE_ICON_SIZE)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun AlbumListFilterGenresPreview() {
    AlbumListFilterGenres(
        filter = AlbumListFilter(),
        filterGenreStateCount = emptyList(),
        onChange = {}
    )
}

