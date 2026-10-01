package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

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
    val selectorText = if (totalSelected == 0) "Any" else "$totalSelected selected"

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

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun AlbumListFilterGenresPreview() {
    AlbumListFilterGenres(
        filter = AlbumListFilter(),
        filterGenreStateCount = emptyList(),
        onChange = {}
    )
}
