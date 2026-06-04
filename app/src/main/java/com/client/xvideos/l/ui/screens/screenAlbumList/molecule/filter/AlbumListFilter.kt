package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.graphQl.Genre
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumFilterDisplay
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterAlbumType
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterAudiences
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterContentType
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterGenres
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterSize
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterTags
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.StyleGenresTags

@Composable
fun AlbumListFilter(
    filter: AlbumListFilter,
    filterGCount: List<AlbumListFilterGenreCountResponse>?,
    filterTagsCount: List<AlbumListFilterGenreCountResponse>?,
    onClose: () -> Unit, onFilterApply: (AlbumListFilter) -> Unit
) {



    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 2 / 4

    val palette = StyleGenresTags.Palette

    Column( modifier = Modifier.fillMaxHeight().background(palette.screen).padding(horizontal = 8.dp).verticalScroll( rememberScrollState()) )
    {

        Box( Modifier.fillMaxWidth().height(56.dp) )
        {

            Text("Filters",
                color = palette.textPrimary,
                style = ThemeL.Type.screenTitle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp, top = 4.dp)
            )

//            Row(
//                modifier = Modifier.padding(end = 0.dp).fillMaxWidth().height(46.dp)
//                    .align(Alignment.CenterEnd).clickable(onClick = { onClose() }),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Start
//            )
//            {
//                Spacer( modifier = Modifier.fillMaxHeight().fillMaxWidth().weight(1f) )
//                Spacer( modifier = Modifier.fillMaxHeight().width(4.dp) )
//                Spacer( modifier = Modifier.fillMaxHeight().fillMaxWidth().weight(1f) )
//                Spacer( modifier = Modifier.fillMaxHeight().width(4.dp) )
//                Box(
//                    modifier = Modifier.fillMaxHeight().fillMaxWidth().weight(1f)
//                        .border(0.5.dp, ThemeL.grey2, RoundedCornerShape(4.dp)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        "Close", textAlign = TextAlign.Center,
//                        color = ThemeL.textColor, fontFamily = ThemeL.fontFamilyKarla,
//                        fontSize = 20.sp, modifier = Modifier
//                    )
//                }
//            }
        }

        if (filter.searchQuery.isNotBlank()) {
            // Режим поиска: сортировка по релевантности, менять нельзя — показываем только название запроса
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(palette.surface).padding(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                        .background(palette.field)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Search:",
                        color = palette.textSecondary,
                        style = ThemeL.Type.rowSubtitle
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        filter.searchQuery,
                        color = palette.textPrimary,
                        style = ThemeL.Type.rowValue,
                        maxLines = 1
                    )
                }
            }
        } else {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(palette.surface).padding(6.dp)) {
                AlbumFilterDisplay( filter.display, onRequestApply = { onFilterApply(filter.copy(display = it)) })
            }
        }

        Box(modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).padding(4.dp)) {
            AlbumListFilterAlbumType(
                when (filter.album_type) {
                    AlbumType.All -> 0
                    AlbumType.Manga -> 1
                    AlbumType.Pictures -> 2
                }
            ) {
                val type = when (it) {
                    0 -> AlbumType.All
                    1 -> AlbumType.Manga
                    2 -> AlbumType.Pictures
                    else -> AlbumType.All
                }
                onFilterApply(filter.copy(album_type = type))
            }
        }

        Box(modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).padding(4.dp)) {
            AlbumListFilterContentType(filter.content_id) { onFilterApply(filter.copy(content_id = it)) }
        }

        Box(
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).border(1.dp, palette.border, RoundedCornerShape(8.dp)).sizeIn(maxHeight = maxHeight)
        ) { AlbumListFilterAudiences(filter) { onFilterApply(it) } }

        Box( modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).border(1.dp, palette.border, RoundedCornerShape(8.dp)).padding(vertical = 8.dp).padding(start = 8.dp, end = 8.dp)
        ) { AlbumListFilterSize(filter.picture_count_rank) { onFilterApply( filter.copy( picture_count_rank = it ) ) } }

        Box( modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).border(1.dp, palette.border, RoundedCornerShape(8.dp)).sizeIn(maxHeight = maxHeight)
        ) { AlbumListFilterGenres(filter, filterGCount) { onFilterApply(it) } }

        Box(
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(palette.surface).border(1.dp, palette.border, RoundedCornerShape(8.dp)).sizeIn(maxHeight = maxHeight)
        ) { AlbumListFilterTags(filter, filterTagsCount) { onFilterApply(it) } }


    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C, widthDp = 390, heightDp = 820)
@Composable
private fun AlbumListFilterPreview() {
    var filter by remember { mutableStateOf(albumListFilterPreviewFilter()) }

    AlbumListFilter(
        filter = filter,
        filterGCount = albumListFilterPreviewGenreCounts(),
        filterTagsCount = albumListFilterPreviewTagCounts(),
        onClose = {},
        onFilterApply = { filter = it }
    )
}

private fun albumListFilterPreviewFilter(): com.client.xvideos.l.model.AlbumListFilter {
    return com.client.xvideos.l.model.AlbumListFilter(
        display = "date_trending",
        album_type = AlbumType.Manga,
        content_id = ContentId.Hentai,
        audienceIds = "+2+3",
        picture_count_rank = PictureCountRank.C50_100,
        genresPlus = listOf(albumListFilterPreviewGenre("38", "Monsters & Tentacles")),
        genresMinus = listOf(albumListFilterPreviewGenre("23", "SFW")),
        tagPlus = listOf("monster girl", "demon"),
        tagMinus = listOf("ai generated")
    )
}

private fun albumListFilterPreviewGenreCounts(): List<AlbumListFilterGenreCountResponse> {
    return listOf(
        AlbumListFilterGenreCountResponse(count = 842, term = "Monsters & Tentacles", isActive = true),
        AlbumListFilterGenreCountResponse(count = 421, term = "Fantasy Girls", isActive = false),
        AlbumListFilterGenreCountResponse(count = 219, term = "Furries", isActive = false),
        AlbumListFilterGenreCountResponse(count = 73, term = "SFW", isActive = true)
    )
}

private fun albumListFilterPreviewTagCounts(): List<AlbumListFilterGenreCountResponse> {
    return listOf(
        AlbumListFilterGenreCountResponse(count = 1230, term = "monster girl", isActive = true),
        AlbumListFilterGenreCountResponse(count = 887, term = "demon", isActive = true),
        AlbumListFilterGenreCountResponse(count = 312, term = "fantasy", isActive = false),
        AlbumListFilterGenreCountResponse(count = 104, term = "ai generated", isActive = true)
    )
}

private fun albumListFilterPreviewGenre(
    id: String,
    title: String
): Genre {
    return Genre(
        id = id,
        title = title,
        slug = title.lowercase().replace(" ", "-"),
        description = "",
        uploadingRules = "",
        posterUrl = null,
        actsAsWarning = false,
        actsAsDefault = false,
        representsUncategorized = false,
        url = "/genres/${title.lowercase().replace(" ", "-")}_$id/",
        parent = null,
        onlyAllowsModel = null,
        onlyContent = null
    )
}
