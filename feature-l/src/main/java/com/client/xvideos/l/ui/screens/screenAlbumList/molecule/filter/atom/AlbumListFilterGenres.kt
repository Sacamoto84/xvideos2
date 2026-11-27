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
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.model.isValid
import com.client.xvideos.l.model.lAlbumListLinkParams
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.AlbumFilterGenresDialog

@Composable
fun AlbumListFilterGenres(
    filter: AlbumListFilter,
    filterGenreStateCount: List<AlbumListFilterGenreCountResponse>?,
    modifier: Modifier = Modifier,
    onChange: (AlbumListFilter) -> Unit
) {
    val mediaCategories by mediaCategoriesFlow.collectAsStateWithLifecycle()

    val genresPlus = remember(filter.genresPlus) { filter.genresPlus.distinctBy { it.id } }
    val genresMinus = remember(filter.genresMinus) { filter.genresMinus.distinctBy { it.id } }

    val allGenres = mediaCategories?.genres ?: emptyList()

    val selectableGenres = remember(allGenres, genresPlus, genresMinus, filterGenreStateCount) {
        val selectedIds = (genresPlus + genresMinus).map { it.id }.toSet()
        selectableAlbumGenres(allGenres, filterGenreStateCount.orEmpty(), selectedIds)
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
            selectableGenres = selectableGenres,
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

internal fun selectableAlbumGenres(
    catalog: List<FilterGenre>,
    counts: List<AlbumListFilterGenreCountResponse>,
    selectedIds: Set<String>,
): List<FilterGenre> {
    val validGenres = catalog.filter { it.isValid }
    val genresById = validGenres.associateBy { it.id }
    val genresByTitle = validGenres.associateBy { it.title.trim().lowercase() }
    val countedGenres = counts.mapNotNull { count ->
        val term = count.term.trim()
        val id = genreIdFromFacetLink(count.urlToMustInclude, selectedIds)
        genresById[id] ?: genresByTitle[term.lowercase()]
            ?: id?.takeIf { term.isNotEmpty() }?.let { FilterGenre(id = it, title = term) }
    }

    return countedGenres.ifEmpty { validGenres }.distinctBy { it.id }.filterNot { it.id in selectedIds }
}

private val includedGenreId = Regex("""(?:^|[+ ])(\d+)(?=[+ -]|$)""")

private fun genreIdFromFacetLink(url: String, selectedIds: Set<String>): String? {
    val ids = try {
        lAlbumListLinkParams(url.substringBefore('#'))["genre_ids"]
    } catch (_: IllegalArgumentException) {
        null
    } ?: return null

    return includedGenreId.findAll(ids).map { it.groupValues[1] }
        .filter { it !in selectedIds && (it.toLongOrNull() ?: 0L) > 0L }
        .singleOrNull()
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
