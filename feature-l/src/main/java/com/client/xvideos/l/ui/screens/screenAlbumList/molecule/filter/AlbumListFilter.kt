package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumFilterDisplay
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumFilterPresetManager
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumFilterSaveDialog
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumFilterSavedPresetsDialog
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterAlbumType
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterAudiences
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterContentType
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterGenres
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterSize
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumListFilterTags
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.StyleGenresTags

private val CARD_SHAPE = RoundedCornerShape(8.dp)
private val BUTTON_SHAPE = RoundedCornerShape(6.dp)
private val BORDER_WIDTH = 1.dp
private val CARD_TOP_PADDING = 8.dp
private val CARD_INNER_PADDING = 8.dp
private val BOTTOM_SPACER_HEIGHT = 8.dp
private val HEADER_HEIGHT = 56.dp
private val SEARCH_HEADER_HEIGHT = 48.dp
private val ICON_SIZE_16 = 16.dp
private val ICON_SIZE_24 = 24.dp
private val CLOSE_BUTTON_SIZE = 36.dp
private val SPACER_WIDTH_4 = 4.dp
private val SPACER_WIDTH_6 = 6.dp
private val ROW_SPACED_BY_6 = Arrangement.spacedBy(6.dp)
private val ROW_SPACE_BETWEEN = Arrangement.SpaceBetween
private val ROW_ABSOLUTE_SPACE_BETWEEN = Arrangement.Absolute.SpaceBetween
private val ALIGNMENT_CENTER_VERTICALLY = Alignment.CenterVertically
private val COLOR_GRAY = Color.Gray

/** Общий фон-«карточка» секции фильтра: отступ сверху, скругление, фон, опц. рамка. */
private fun Modifier.filterCard(
    border: Boolean = true,
): Modifier {
    val palette = StyleGenresTags.Palette
    return this
        .padding(top = CARD_TOP_PADDING)
        .clip(CARD_SHAPE)
        .background(palette.surface)
        .then(if (border) Modifier.border(BORDER_WIDTH, palette.border, CARD_SHAPE) else Modifier)
}

@Composable
fun AlbumListFilter(
    filter: AlbumListFilter,
    filterGCount: List<AlbumListFilterGenreCountResponse>?,
    filterTagsCount: List<AlbumListFilterGenreCountResponse>?,
    onClose: () -> Unit,
    onFilterApply: (AlbumListFilter) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AlbumFilterPresetManager.init(context)
    }
    val presets by AlbumFilterPresetManager.presets.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSavedPresetsDialog by remember { mutableStateOf(false) }
    val palette = StyleGenresTags.Palette

    val animatedTextStyle = remember {
        Theme.L.Type.bodyLarge.copy(
            color = StyleGenresTags.colorSelectTextItem,
            fontWeight = FontWeight.Bold
        )
    }
    val checkboxColors = CheckboxDefaults.colors(uncheckedBorderColor = COLOR_GRAY)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.screen)
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AlbumListFilterHeader(
            presetsCount = presets.size,
            onSaveClick = { showSaveDialog = true },
            onSavedPresetsClick = { showSavedPresetsDialog = true },
            onClose = onClose
        )

        if (filter.searchQuery.isNotBlank()) {
            AlbumFilterSearchHeader(filter.searchQuery)
        } else {
            Box(modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)) {
                AlbumFilterDisplay(filter.display, onRequestApply = { onFilterApply(filter.copy(display = it)) })
            }
        }

        Box(modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)) {
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

        Box(modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)) {
            AlbumListFilterContentType(filter.content_id) { onFilterApply(filter.copy(content_id = it)) }
        }

        Box(
            modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)
        ) { AlbumListFilterAudiences(filter) { onFilterApply(it) } }

        Box(
            modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)
        ) { AlbumListFilterSize(filter.picture_count_rank) { onFilterApply(filter.copy(picture_count_rank = it)) } }

        Box(
            modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)
        ) { AlbumListFilterGenres(filter, filterGCount) { onFilterApply(it) } }

        Box(
            modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)
        ) { AlbumListFilterTags(filter, filterTagsCount) { onFilterApply(it) } }

        Row(
            modifier = Modifier.fillMaxWidth().filterCard(border = true).padding(start = CARD_INNER_PADDING),
            verticalAlignment = ALIGNMENT_CENTER_VERTICALLY,
            horizontalArrangement = ROW_ABSOLUTE_SPACE_BETWEEN
        ) {
            Text("Animated", style = animatedTextStyle)
            Checkbox(
                checked = filter.selection == "animated",
                onCheckedChange = { onFilterApply(filter.copy(selection = if (it) "animated" else "all")) },
                colors = checkboxColors
            )
        }

        Spacer(Modifier.height(BOTTOM_SPACER_HEIGHT))

    }

    if (showSaveDialog) {
        AlbumFilterSaveDialog(
            filter = filter,
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showSavedPresetsDialog) {
        AlbumFilterSavedPresetsDialog(
            onSelectPreset = { presetFilter ->
                onFilterApply(presetFilter)
            },
            onDismiss = { showSavedPresetsDialog = false }
        )
    }
}

@Composable
private fun AlbumListFilterHeader(
    presetsCount: Int,
    onSaveClick: () -> Unit,
    onSavedPresetsClick: () -> Unit,
    onClose: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val headerTitleStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }
    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .padding(horizontal = 4.dp),
        verticalAlignment = ALIGNMENT_CENTER_VERTICALLY,
        horizontalArrangement = ROW_SPACE_BETWEEN
    ) {
        Text(
            "Filters",
            color = palette.textPrimary,
            style = headerTitleStyle
        )

        Row(
            verticalAlignment = ALIGNMENT_CENTER_VERTICALLY,
            horizontalArrangement = ROW_SPACED_BY_6
        ) {
            // Save button
            Row(
                modifier = Modifier
                    .clip(BUTTON_SHAPE)
                    .border(BORDER_WIDTH, palette.border, BUTTON_SHAPE)
                    .background(palette.field)
                    .clickable { onSaveClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = ALIGNMENT_CENTER_VERTICALLY
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save filter preset",
                    tint = palette.accent,
                    modifier = Modifier.size(ICON_SIZE_16)
                )
                Spacer(Modifier.width(SPACER_WIDTH_4))
                Text(
                    "Save",
                    color = palette.textPrimary,
                    style = Theme.L.Type.button
                )
            }

            // Saved presets button
            Row(
                modifier = Modifier
                    .clip(BUTTON_SHAPE)
                    .border(BORDER_WIDTH, palette.border, BUTTON_SHAPE)
                    .background(palette.field)
                    .clickable { onSavedPresetsClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = ALIGNMENT_CENTER_VERTICALLY
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Saved presets",
                    tint = palette.selectedBorder,
                    modifier = Modifier.size(ICON_SIZE_16)
                )
                Spacer(Modifier.width(SPACER_WIDTH_4))
                Text(
                    "Saved ($presetsCount)",
                    color = palette.textPrimary,
                    style = Theme.L.Type.button
                )
            }

            // Close button (X)
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(CLOSE_BUTTON_SIZE)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close filters",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(ICON_SIZE_24)
                )
            }
        }
    }
}

@Composable
private fun AlbumFilterSearchHeader(
    searchQuery: String
) {
    val palette = StyleGenresTags.Palette
    Box(modifier = Modifier.filterCard(border = true).padding(CARD_INNER_PADDING)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SEARCH_HEADER_HEIGHT)
                .clip(BUTTON_SHAPE)
                .border(BORDER_WIDTH, palette.border, BUTTON_SHAPE)
                .background(palette.field)
                .padding(horizontal = 8.dp),
            verticalAlignment = ALIGNMENT_CENTER_VERTICALLY
        ) {
            Text(
                "Search:",
                color = palette.textSecondary,
                style = Theme.L.Type.rowSubtitle
            )
            Spacer(Modifier.width(SPACER_WIDTH_6))
            Text(
                searchQuery,
                color = palette.textPrimary,
                style = Theme.L.Type.rowValue,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C, widthDp = 390)
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
    return AlbumListFilter(
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
): FilterGenre {
    return FilterGenre(
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
