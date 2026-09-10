package com.client.xvideos.r.ui.niche.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.r.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Preview as NichePreviewModel
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun NichePreview(niches: () -> Niche, onClick: () -> Unit) {

    Column(modifier = Modifier.height(80.dp).padding(horizontal = 4.dp)
        .shadow(10.dp, RoundedCornerShape(8.dp))
        .clip(RoundedCornerShape(8.dp))
        .background(Theme.tabLevel3)
        .clickable{onClick()}
    )
    {

        Row(modifier = Modifier) {
            UrlImage(niches().thumbnail, modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(6.dp)).size(72.dp))

           Column(verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.Start, modifier = Modifier.padding(vertical = 4.dp).fillMaxHeight()) {
               Text(text = niches().name, modifier = Modifier.padding(end = 4.dp), color = Color.White, textAlign = TextAlign.Start, )

               Row(verticalAlignment = Alignment.CenterVertically) {
                   Icon(painter = painterResource(R.drawable.members), contentDescription = null, tint = Color.White,modifier = Modifier.size(16.dp))
                   Text(text = niches().subscribers.toPrettyCount(), modifier = Modifier.padding(start = 4.dp, end = 4.dp).wrapContentWidth(Alignment.CenterHorizontally), color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
               }
               Row(verticalAlignment = Alignment.CenterVertically) {
                   Icon( painter = painterResource(R.drawable.posts), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp) )
                   Text( text = niches().gifs.toPrettyCount(), modifier = Modifier.padding(start = 4.dp, end = 4.dp).wrapContentWidth(Alignment.CenterHorizontally), color = Color.White , textAlign = TextAlign.Center, fontSize = 16.sp)
               }
           }

        }

    }

}



@Preview
@Composable
fun NichePreviewPreview() {
    XvideosTheme {
        NichePreview(
            niches = {
                Niche(
                    id = "female-backs",
                    name = "Female Backs",
                    gifs = 245,
                    subscribers = 914,
                    thumbnail = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
                    previews = listOf(
                        NichePreviewModel(
                            id = "dangerouswanmice",
                            thumbnail = "https://media.redgifs.com/DangerousWanMice-mobile.jpg"
                        ),
                        NichePreviewModel(
                            id = "weirddaringbovine",
                            thumbnail = "https://media.redgifs.com/WeirdDaringBovine-mobile.jpg"
                        ),
                        NichePreviewModel(
                            id = "unsteadyphonywren",
                            thumbnail = "https://media.redgifs.com/UnsteadyPhonyWren-mobile.jpg"
                        )
                    )
                )
            },
            onClick = {}
        )
    }
}
