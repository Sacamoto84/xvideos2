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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.composeunstyled.Icon
import com.composeunstyled.Text

@Composable
fun AlbumListFilterTags(
    filter: AlbumListFilter,
    filterTagStateCount: List<AlbumListFilterGenreCountResponse>?,
    onChange: (AlbumListFilter) -> Unit
) {

    val tagCountItems = filterTagStateCount.orEmpty()
    val filterTerms = tagCountItems.map { it.term }.toSet()

    val tagsPlus = filter.tagPlus
    val tagsMinus = filter.tagMinus

    val tagsCorrect = filterTerms.minus(tagsPlus.map{it}).minus(tagsMinus.map{it}).toList()
    val palette = StyleGenresTags.Palette

    Column(modifier = Modifier.fillMaxWidth().background(palette.surface))
    {

        //HorizontalDivider()

        LazyColumn {
            items(tagsPlus) {
                Text(
                    it,
                    color = StyleGenresTags.colorSelectTextItem,
                    modifier = Modifier
                        .then(StyleGenresTags.modifierSelectTextItem)
                        .clickable(onClick = {
                            val plus = mutableListOf<String>()
                            plus.addAll(tagsPlus)
                            plus.remove(it)
                            val filter1 = filter.copy(tagPlus = plus)
                            onChange(filter1)
                        }),
                    style = ThemeL.Type.bodyLarge.copy(color = StyleGenresTags.colorSelectTextItem, fontWeight = FontWeight.Bold)
                )
            }

            items(tagsMinus) {

                val s = buildAnnotatedString {
                    withStyle(SpanStyle( color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) { append("NOT") }
                    append(" $it")
                }

                Text(
                    s,
                    color = StyleGenresTags.colorExcludedTextItem,
                    modifier = Modifier
                        .then(StyleGenresTags.modifierExcludedTextItem)
                        .clickable(onClick = {
                            val minus = mutableListOf<String>()
                            minus.addAll(tagsMinus)
                            minus.remove(it)
                            val filter1 = filter.copy(tagMinus = minus)
                            onChange(filter1)
                        }),
                    style = ThemeL.Type.bodyLarge.copy(color = StyleGenresTags.colorExcludedTextItem, fontWeight = FontWeight.Bold)
                )
            }
        }

        DisclosureLayout("Tags") {
            Box(
                modifier = Modifier.padding(4.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()
                    .border( 1.dp, palette.border, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.panelBlack)
                ) {
                    item{ Spacer(Modifier.height(0.dp)) }
                    items(tagsCorrect.size) {
                        val item = tagsCorrect[it]

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
                                ) {
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
                                                val plus = mutableListOf<String>()
                                                plus.addAll(tagsPlus)
                                                plus.add(item)
                                                val filter1 = filter.copy(tagPlus = plus)
                                                onChange(filter1)
                                            })
                                    )



                                    Text(item, color = palette.textPrimary, style = ThemeL.Type.rowTitle.copy(color = palette.textPrimary, fontWeight = FontWeight.Bold))

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
                                                val minus = mutableListOf<String>()
                                                minus.addAll(tagsMinus)
                                                minus.add(item)
                                                val filter1 = filter.copy(tagMinus = minus)
                                                onChange(filter1)
                                            })
                                    )

                                }

                                val count = tagCountItems.find { it1 -> it1.term == item }?.count ?: -1
                                Text(count.toString(), color = palette.textSecondary, style = ThemeL.Type.rowTitle.copy(color = palette.textSecondary, fontWeight = FontWeight.Bold))

                            }

                    }
                    item{ Spacer(Modifier.height(4.dp)) }
                }
            }
        }

    }
}
