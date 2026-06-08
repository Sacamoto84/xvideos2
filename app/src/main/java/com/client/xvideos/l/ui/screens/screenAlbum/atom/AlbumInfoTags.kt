package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.capitalizeEachWord
import com.client.xvideos.l.model.Tag

@Composable
fun AlbumInfoTags(tags: () -> (List<Tag>), onClick: (String) -> Unit) {
    FlowRow(verticalArrangement = Arrangement.Center) {
        //parsed.tags.reversed().filter{it.count>0}.forEach {
        tags().forEach {
            Text(
                "${it.text.capitalizeEachWord()} (${it.count})",
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .padding(vertical = 2.dp)
                    .border(1.dp, Theme.L.secondaryColor, RoundedCornerShape(4.dp))
                    .padding(4.dp)
                    .clickable(onClick = { onClick(it.text) }),
                color = Theme.L.textColor,
                style = Theme.L.Type.caption.copy(color = Theme.L.textColor, fontSize = 14.sp)
            )
        }
    }
}

//@Preview
//@Composable
//fun AlbumInfoTagsPreview() {
//    val parsed = AlbumDetails(
//        created = 1678886400.0,
//        modified = 1678886400.0,
//        id = "album123",
//        title = "Sample Album Title",
//        tags = listOf(
//            Tag(
//                id = "tag1",
//                category = "general",
//                text = "nature photography",
//                url = "url1",
//                count = 150
//            ),
//            Tag(
//                id = "tag2",
//                category = "location",
//                text = "mountain view",
//                url = "url2",
//                count = 75
//            )
//        ),
//        is_manga = false,
//        content = Content(id = "content1", title = "Album Content", url = "content_url"),
//        genres = listOf(
//            Genre(id = "genre1", title = "Landscape", actsAsWarning = false, url = "genre_url1"),
//            Genre(id = "genre2", title = "Adventure", actsAsWarning = false, url = "genre_url2")
//        ),
//        cover = Cover(
//            width = 1920,
//            height = 1080,
//            size = "large",
//            url = "cover_url"
//        ),
//        description = "This is a sample album description detailing the contents and theme of the album.",
//        audiences = listOf(
//            Audience(id = "audience1", title = "General Audience", url = "audience_url1"),
//            Audience(id = "audience2", title = "Photography Enthusiasts", url = "audience_url2")
//        ),
//        number_of_pictures = 100,
//        number_of_animated_pictures = 5,
//        url = "album_url",
//        download_url = "download_album_url"
//    )
//    AlbumInfoTags( { parsed.tags.reversed().filter{it.count>0} }, {})
//}
