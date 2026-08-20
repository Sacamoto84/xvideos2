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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Audience

@Composable
fun AlbumInfoAudiences(
    parsed: AlbumDetails,
    onAudienceClick: (Audience) -> Unit = {}
) {
    FlowRow(
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Audiences: ",
            color = Theme.L.textColor,
            style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        parsed.audiences.forEachIndexed { index, item ->
            Text(
                text = buildString {
                    append(item.title)
                    //if (index != parsed.audiences.lastIndex) append(",")
                },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .padding(vertical = 2.dp)
                    .border(1.dp, Theme.L.secondaryColor, RoundedCornerShape(4.dp))
                    .clickable(onClick = { onAudienceClick(item) })
                    .padding(4.dp),
                color = Theme.L.primaryColor,
                style = Theme.L.Type.rowValue.copy(color = Theme.L.primaryColor, fontSize = 14.sp),
            )

        }
    }
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
