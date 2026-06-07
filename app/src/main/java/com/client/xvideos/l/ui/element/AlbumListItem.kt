package com.client.xvideos.l.ui.element

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage

//.aspectRatio(640f/935)
@Composable
fun AlbumListItem(
    title: String,
    coverUrl: String,
    numberOfAnimatedPictures: Int,
    numberOfPictures: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Theme.L.grey3, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {

        UrlImage(
            coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(137f / 200)//.width(137.dp).height(200.dp)
            ,
            contentScale = ContentScale.Crop
        )

        Column( Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x80000000)) )
        {

            Text(
                title.removePrefix(" "),
                modifier = Modifier.padding(horizontal = 4.dp),
                color = Color.White,
                style = Theme.L.Type.rowTitle.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row( Modifier.padding(start = 4.dp).fillMaxWidth() )
            {
                val str = StringBuilder()
                if (numberOfAnimatedPictures > 0) {
                    str.append("$numberOfAnimatedPictures gifs")
                    if (numberOfPictures > 0) str.append(" / ")
                }
                if (numberOfPictures > 0) {
                    str.append(numberOfPictures.toString())
                    if (numberOfAnimatedPictures == 0) str.append(" pictures")
                }
                Text(
                    str.toString(),
                    modifier = Modifier,
                    color = Theme.L.textColor,
                    style = Theme.L.Type.rowSubtitle
                )
            }
        }

    }

}

@Preview
@Composable
fun AlbumListItemPreview() {
    AlbumListItem(
        title = "Album Title",
        coverUrl = "https://i.pinimg.com/1200x/2c/86/8d/2c868d9ab0c4d4f3a76631c1b0077058.jpg",
        numberOfAnimatedPictures = 5,
        numberOfPictures = 10,
        onClick = {}
    )
}
