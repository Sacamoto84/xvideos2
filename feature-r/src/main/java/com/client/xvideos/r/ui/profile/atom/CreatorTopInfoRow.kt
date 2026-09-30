package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Верхняя строка информации о креаторе: аватар, имя, бейдж верификации и кнопка подписки.
 */
@Composable
fun CreatorTopInfoRow(
    item: UserInfo,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followButtonText = if (isFollow) "Unfollow" else "Follow"
    val followButtonTextColor = if (isFollow) Color.White else Color.Black
    val followButtonBgColor = if (isFollow) Theme.tabLevel1 else Theme.R.colorYellow
    val followButtonShape = RoundedCornerShape(8.dp)
    val followButtonStyledModifier = remember(isFollow, followButtonBgColor) {
        Modifier
            .padding(start = 8.dp, end = 64.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(followButtonShape)
            .background(followButtonBgColor)
            .then(if (isFollow) Modifier.border(1.dp, Color.White, followButtonShape) else Modifier)
    }

    Row(
        modifier = modifier.padding(top = 2.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.profileImageUrl != null) {
            UrlImage(
                item.profileImageUrl,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .size(96.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .size(96.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = item.username,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Row(
                modifier = Modifier.height(48.dp),
                verticalAlignment = Alignment.Top
            ) {
                Spacer(Modifier.width(8.dp))
                Text(
                    item.username,
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsMedium,
                    fontSize = 28.sp,
                    modifier = Modifier
                )
                if (item.verified) {
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.verificed),
                        contentDescription = "Verified Creator",
                        modifier = Modifier
                            .size(26.dp)
                            .offset(y = 8.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .then(followButtonStyledModifier)
                    .clickable(onClick = onFollowClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    followButtonText,
                    color = followButtonTextColor,
                    fontFamily = Theme.R.fontFamilyDMsanss,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview
@Composable
private fun CreatorTopInfoRowPreview() {
    XvideosTheme {
        CreatorTopInfoRow(
            item = UserInfo(
                username = "test_creator",
                verified = true
            ),
            isFollow = false,
            onFollowClick = {}
        )
    }
}
