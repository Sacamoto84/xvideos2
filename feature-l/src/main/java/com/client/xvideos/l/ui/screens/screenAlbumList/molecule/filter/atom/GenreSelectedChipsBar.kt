package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun GenreSelectedChipsBar(
    genresPlus: List<FilterGenre>,
    genresMinus: List<FilterGenre>,
    onRemovePlus: (FilterGenre) -> Unit,
    onRemoveMinus: (FilterGenre) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(genresPlus, key = { "plus_${it.id.ifBlank { it.title }}" }) { genre ->
            val annotatedTitle = remember(genre.title) {
                AnnotatedString("+ ${genre.title}")
            }
            FilterGenreChip(
                text = annotatedTitle,
                textColor = palette.selectedText,
                borderColor = palette.selectedBorder,
                backgroundColor = palette.selected,
                onClick = { onRemovePlus(genre) }
            )
        }
        items(genresMinus, key = { "minus_${it.id.ifBlank { it.title }}" }) { genre ->
            val annotatedGenre = remember(genre.title, palette.excludedBorder) {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) { append("NOT") }
                    append(" ${genre.title}")
                }
            }
            FilterGenreChip(
                text = annotatedGenre,
                textColor = palette.excludedText,
                borderColor = palette.excludedBorder,
                backgroundColor = palette.excluded,
                onClick = { onRemoveMinus(genre) }
            )
        }
    }
}

@Preview
@Composable
private fun GenreSelectedChipsBarPreview() {
    GenreSelectedChipsBar(
        genresPlus = listOf(FilterGenre(id = "1", title = "Action")),
        genresMinus = listOf(FilterGenre(id = "2", title = "Horror")),
        onRemovePlus = {},
        onRemoveMinus = {}
    )
}
