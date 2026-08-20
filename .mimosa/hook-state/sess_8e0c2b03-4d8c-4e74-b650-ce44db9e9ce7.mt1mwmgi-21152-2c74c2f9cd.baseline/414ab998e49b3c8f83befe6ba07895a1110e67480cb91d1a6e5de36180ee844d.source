package com.client.xvideos.r.ui.niche.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.model.TopCreator
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun NicheTopCreator(creator : TopCreator, onClick: () -> Unit) {

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick.invoke() }) {
        UrlImage(
            creator.profileImageUrl,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .size(96.dp)
                .innerShadow(
                    shape = RoundedCornerShape(8.dp),
                    block = {
                        radius = 3f
                        spread = 0f
//                        brush = Brush.verticalGradient(
//                        colors = listOf(Transparent, Color.Black)
//                        )
                    }
                )) {

        }
    }

}

@Preview
@Composable
private fun NicheTopCreatorPreview() {
    XvideosTheme {
        NicheTopCreator(
            creator = TopCreator(
                creationtime = 0,
                description = "description",
                followers = 1,
                gifs = 1,
                name = "name",
                profileImageUrl = "",
                username = "username",
                verified = false,
                studio = false,
                views = 1
            ),
            onClick = {}
        )
    }
}
