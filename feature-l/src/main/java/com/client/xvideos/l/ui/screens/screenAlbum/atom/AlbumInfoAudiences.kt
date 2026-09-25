package com.client.xvideos.l.ui.screens.screenAlbum.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Audience

private val AUDIENCE_CHIP_CORNER = 4.dp
private val AUDIENCE_CHIP_SHAPE = RoundedCornerShape(AUDIENCE_CHIP_CORNER)
private val CHIP_BORDER_WIDTH = 1.dp
private val CHIP_HORIZONTAL_PADDING = 2.dp
private val CHIP_VERTICAL_PADDING = 2.dp
private val CHIP_CONTENT_PADDING = 4.dp
private val HEADER_VERTICAL_PADDING = 4.dp
private val HEADER_FONT_SIZE = 16.sp
private val AUDIENCE_FONT_SIZE = 14.sp
private const val LABEL_AUDIENCES = "Audiences: "
private val HEADER_FONT_WEIGHT = FontWeight.ExtraBold

private val AUDIENCE_CHIP_BASE_MODIFIER = Modifier
    .padding(horizontal = CHIP_HORIZONTAL_PADDING, vertical = CHIP_VERTICAL_PADDING)
    .clip(AUDIENCE_CHIP_SHAPE)

private val AUDIENCE_CHIP_CONTENT_PADDING_MODIFIER = Modifier.padding(CHIP_CONTENT_PADDING)
private val HEADER_TEXT_MODIFIER = Modifier.padding(vertical = HEADER_VERTICAL_PADDING)
private val FLOW_ROW_VERTICAL_ARRANGEMENT = Arrangement.Center

@Composable
fun AlbumInfoAudiences(
    parsed: AlbumDetails,
    onAudienceClick: (Audience) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val headerStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(fontWeight = HEADER_FONT_WEIGHT, fontSize = HEADER_FONT_SIZE)
    }
    val audienceTextStyle = remember(Theme.L.Type.rowValue, Theme.L.primaryColor) {
        Theme.L.Type.rowValue.copy(color = Theme.L.primaryColor, fontSize = AUDIENCE_FONT_SIZE)
    }

    FlowRow(
        modifier = modifier,
        verticalArrangement = FLOW_ROW_VERTICAL_ARRANGEMENT
    ) {
        Text(
            text = LABEL_AUDIENCES,
            color = Theme.L.textColor,
            style = headerStyle,
            modifier = HEADER_TEXT_MODIFIER
        )
        parsed.audiences.forEach { item ->
            key(item.id) {
                AudienceChip(
                    item = item,
                    onClick = onAudienceClick,
                    textStyle = audienceTextStyle
                )
            }
        }
    }
}

@Composable
private fun AudienceChip(
    item: Audience,
    onClick: (Audience) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val handleClick = remember(item, onClick) { { onClick(item) } }
    Text(
        text = item.title,
        modifier = modifier
            .then(AUDIENCE_CHIP_BASE_MODIFIER)
            .border(CHIP_BORDER_WIDTH, Theme.L.secondaryColor, AUDIENCE_CHIP_SHAPE)
            .clickable(onClick = handleClick)
            .then(AUDIENCE_CHIP_CONTENT_PADDING_MODIFIER),
        color = Theme.L.primaryColor,
        style = textStyle
    )
}

@Preview
@Composable
fun AlbumInfoAudiencesPreview() {
    val parsed = AlbumDetails(
        created = 1678886400.0,
        modified = 1678886400.0,
        id = "123",
        title = "Sample Album",
        tags = emptyList(),
        is_manga = false,
        content = com.client.xvideos.l.model.Content(id = "c1", title = "Content 1", url = "url_content"),
        genres = emptyList(),
        cover = com.client.xvideos.l.model.Cover(width = 100, height = 100, size = "small", url = "url_cover"),
        description = "This is a sample album description.",
        audiences = listOf(
            Audience(id = "a1", title = "Audience 1", url = "url1"),
            Audience(id = "a2", title = "Audience 2", url = "url2"),
            Audience(id = "a3", title = "Audience 3", url = "url3")
        ),
        number_of_pictures = 10,
        number_of_animated_pictures = 2,
        url = "album_url",
        download_url = "download_album_url"
    )
    AlbumInfoAudiences(parsed = parsed)
}
