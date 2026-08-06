package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import com.composeunstyled.Icon
import com.composeunstyled.Text

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

    val genresPlus = filter.genresPlus
    val genresMinus = filter.genresMinus

    val allGenres = mediaCategories?.genres ?: emptyList()

    // remember: два minus плюс filter по всему списку жанров на каждой рекомпозиции.
    val genresPlusCorrect = remember(allGenres, genresPlus, genresMinus, filterTerms) {
        allGenres.minus(genresPlus).minus(genresMinus)
            .filter { filterTerms?.contains(it.title) == true }
    }
    val palette = StyleGenresTags.Palette

    Column(modifier = Modifier.fillMaxWidth().background(palette.surface))
    {

        //HorizontalDivider()

        LazyColumn(
            modifier = Modifier.padding(top = 1.dp)
            //contentPadding= PaddingValues(4.dp)
        ) {
            items(genresPlus) {
                Text(
                    it.title,
                    color = StyleGenresTags.colorSelectTextItem,
                    modifier = Modifier.then(StyleGenresTags.modifierSelectTextItem)
                        .clickable(onClick = {
                            val plus = mutableListOf<FilterGenre>()
                            plus.addAll(genresPlus)
                            plus.remove(it)
                            val filter1 = filter.copy(genresPlus = plus)
                            onChange(filter1)
                        }),
                    style = Theme.L.Type.bodyLarge.copy(color = StyleGenresTags.colorSelectTextItem, fontWeight = FontWeight.Bold)
                )
            }

            items(genresMinus) {

                val s = buildAnnotatedString {
                    withStyle(SpanStyle( color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) { append("NOT") }
                    append(" "+it.title)
                }

                Text(
                    s,
                    color = StyleGenresTags.colorExcludedTextItem,
                    modifier = Modifier.then(StyleGenresTags.modifierExcludedTextItem)
                        .clickable(onClick = {
                            val minus = mutableListOf<FilterGenre>()
                            minus.addAll(genresMinus)
                            minus.remove(it)
                            val filter1 = filter.copy(genresMinus = minus)
                            onChange(filter1)
                        }),
                    style = Theme.L.Type.bodyLarge.copy(color = StyleGenresTags.colorExcludedTextItem, fontWeight = FontWeight.Bold)
                )
            }
        }

        DisclosureLayout("Genres") {

            Box(
                modifier = Modifier.padding(4.dp)
            ) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.panelBlack)
                ) {

                    item{ Spacer(Modifier.height(0.dp)) }

                    items(genresPlusCorrect.size) {
                        val item = genresPlusCorrect[it]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 2.dp, top = 4.dp,end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                )
                                {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = palette.selectedBorder,
                                        modifier = Modifier
                                            .padding(vertical = 2.dp)
                                            .padding(horizontal = 4.dp)
                                            .size(40.dp)
                                            .border(1.dp, palette.selectedBorder, RoundedCornerShape(4.dp))
                                            .clickable(onClick = {
                                                val plus = mutableListOf<FilterGenre>()
                                                plus.addAll(genresPlus)
                                                plus.add(item)
                                                val filter1 = filter.copy(genresPlus = plus)
                                                onChange(filter1)
                                            })
                                    )

                                    Text(
                                        item.title,
                                        color = palette.textPrimary,
                                        style = Theme.L.Type.rowTitle.copy(color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                    )

                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = null,
                                        tint = palette.excludedBorder,
                                        modifier = Modifier
                                            .padding(vertical = 2.dp)
                                            .padding(horizontal = 4.dp)
                                            .size(40.dp)
                                            .border(1.dp, palette.excludedBorder, RoundedCornerShape(4.dp))
                                            .clickable(onClick = {
                                                val minus = mutableListOf<FilterGenre>()
                                                minus.addAll(genresMinus)
                                                minus.add(item)
                                                val filter1 = filter.copy(genresMinus = minus)
                                                onChange(filter1)
                                            })
                                    )

                                }

                                val count =
                                    filterGenreStateCount?.find { it1 -> it1.term == item.title }?.count
                                Text(
                                    count.toString(),
                                    color = palette.textSecondary,
                                    style = Theme.L.Type.rowTitle.copy(color = palette.textSecondary, fontWeight = FontWeight.Bold)
                                )
                            }

                    }

                    item{ Spacer(Modifier.height(4.dp)) }
                }
            }
        }

    }
}
