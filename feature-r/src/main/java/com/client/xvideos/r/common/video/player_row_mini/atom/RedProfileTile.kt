package com.client.xvideos.r.common.video.player_row_mini.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.common.util.toMinSec
import com.client.xvideos.common.util.toPrettyCount
import com.composables.core.Icon

@Composable
fun RedProfileTile(item: GifsInfo, index: Int, isVisibleView : Boolean = true, isVisibleDuration : Boolean = true) {

    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {

//        if (item.urls.poster != null) {
//            UrlImage( url = item.urls.poster!!, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize() )
//        }
//        else{
//            UrlImage( url = item.urls.thumbnail, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize() )
//        }
        //Индекс картинки
        Text(
            index.toString(),
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp).offset(1.dp, 1.dp),
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )

        //Нижний ряд с лайками и длительностью
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isVisibleView) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {


                    Box {
                        Icon(
                            painter = painterResource(R.drawable.rg_button),
                            contentDescription = null,
                            tint = Color.Black, modifier = Modifier.offset(1.dp, 1.dp)
                        )
                        Icon(
                            painter = painterResource(R.drawable.rg_button),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Box {
                        Text(
                            item.views?.toPrettyCount() ?: "-",
                            color = Color.Black,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .offset(1.dp, 1.dp),
                            fontFamily = Theme.R.fontFamilyPopinsMedium
                        )

                        Text(
                            item.views?.toPrettyCount() ?: "-",
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = 8.dp),
                            fontFamily = Theme.R.fontFamilyPopinsMedium
                        )
                    }

                }
            }

            if (isVisibleDuration) {
                Box {

                    Text(
                        item.duration?.toMinSec() ?: "-",
                        color = Color.Black,
                        modifier = Modifier
                            .padding(8.dp)
                            .offset(1.dp, 1.dp),
                        fontFamily = Theme.R.fontFamilyPopinsMedium
                    )

                    Text(
                        item.duration?.toMinSec() ?: "-",
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp),
                        fontFamily = Theme.R.fontFamilyPopinsMedium
                    )
                }
            }

        }



    }

}












