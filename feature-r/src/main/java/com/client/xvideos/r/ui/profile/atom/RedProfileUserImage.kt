package com.client.xvideos.r.ui.profile.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.r.R
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text

@Composable
fun RedProfileCreaterInfo(item: UserInfo, savedRed: () -> SavedRed) {
    val isFollow = savedRed().creators.list.any { it.username == item.username }
    RedProfileCreaterInfo(
        item = item,
        isFollow = isFollow,
        onFollowClick = {
            if (isFollow) savedRed().creators.remove(item.username) else savedRed().creators.add(item)
        }
    )
}

@Composable
fun RedProfileCreaterInfo(
    item: UserInfo,
    isFollow: Boolean,
    onFollowClick: () -> Unit
) {

    Column( modifier = Modifier.systemBarsPadding().displayCutoutPadding().padding(horizontal = 4.dp).fillMaxWidth() )
    {

        //Top info
        Row( modifier = Modifier.padding(top = 2.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically )
        {

            if (item.profileImageUrl != null) {
                UrlImage(item.profileImageUrl, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(96.dp))
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .size(96.dp)
                        .background(Color.DarkGray), contentAlignment = Alignment.Center
                )
                {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }

            Column( modifier = Modifier.fillMaxWidth().height(96.dp), verticalArrangement = Arrangement.SpaceAround )
            {
                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Spacer(Modifier.width(8.dp))
                    Text( item.username, color = Color.White, fontFamily = Theme.R.fontFamilyPopinsMedium, fontSize = 28.sp, modifier = Modifier )
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.verificed),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp).offset(y = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 8.dp, end = 64.dp)
                        //.width(96.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFollow) Theme.tabLevel1 else Theme.R.colorYellow)
                        .border(
                            1.dp,
                            if (isFollow) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onFollowClick()
                        }, contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isFollow) "Unfollow" else "Follow",
                        color = if (isFollow) Color.White else Color.Black,
                        fontFamily = Theme.R.fontFamilyDMsanss,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

            }


        }


        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        )
        {

            Column( horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().weight(1f) )
            {
                Text( item.followers.toPrettyCount().toString(), color = Color.White, fontFamily = Theme.R.fontFamilyPopinsMedium )
                Text( "Подписчиков", color = Color(0xFF9E9DA9), fontFamily = Theme.R.fontFamilyPopinsRegular )
            }

            Box( Modifier.width(1.dp).height(24.dp).background(Color(0xFF3D3C53)) )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    item.views.toPrettyCount(),
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsMedium
                )

                Text(
                    "Просмотров",
                    color = Color(0xFF9E9DA9),
                    fontFamily = Theme.R.fontFamilyPopinsRegular
                )
            }

            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFF3D3C53))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                Text(
                    item.publishedGifs.toPrettyCount(),
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsMedium
                )

                Text(
                    "Постов",
                    color = Color(0xFF9E9DA9),
                    fontFamily = Theme.R.fontFamilyPopinsRegular
                )

            }
        }

        if (item.description != null) {
            Text(
                "About ${item.username}:",
                color = Theme.R.colorTextGray,
                fontSize = 14.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )

            Spacer(Modifier.height(4.dp))

            Text(
                item.description.toString().trimMargin(),
                color = Color.White,
                fontSize = 14.sp, fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        }

        Spacer(Modifier.height(8.dp))

    }

}

@Preview
@Composable
fun RedProfileCreaterInfoPreview() {
    val sampleUserInfo = UserInfo(
        username = "lilijunex",
        profileImageUrl = "https://userpic.redgifs.com/4/8c/48cc3668e114f878aafcc6dfd0a3d4f2.png",
        followers = 68214,
        views = 123194825,
        publishedGifs = 421,
        description = "Collared sub addicted to XL horse dildos",
        url = "https://www.redgifs.com/users/lilijunex"
    )

    XvideosTheme {
        Box(modifier = Modifier.background(Theme.R.colorCommonBackground)) {
            RedProfileCreaterInfo(
                item = sampleUserInfo,
                isFollow = false,
                onFollowClick = {}
            )
        }
    }
}
