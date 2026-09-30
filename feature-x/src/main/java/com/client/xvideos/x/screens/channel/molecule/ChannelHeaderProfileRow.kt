package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.core.R
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ProfileType
import com.client.xvideos.x.screens.channel.atom.CountryFlagTag
import com.client.xvideos.x.screens.channel.atom.SubscribeHeaderButton

@Composable
fun ChannelHeaderProfileRow(
    header: ChannelHeaderModel,
    isSubscribed: Boolean,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .offset(y = (-24).dp)
                .size(68.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            if (header.hasAvatar) {
                UrlImage(
                    url = header.avatarUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (header.isModel) {
                Text(
                    text = "\uE9B8",
                    style = TextStyle(
                        color = Color(0xFFDE2600),
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(R.font.iconfont))
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .offset(y = (-12).dp)
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (header.hasFlag) {
                    CountryFlagTag(flagEmoji = header.flagEmoji)
                }
                Text(
                    text = header.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (header.isModel && header.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = header.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFBBBBBB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (header.isModel) Color(0xFFDE2600) else Color(0xFF1E88E5))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (header.isModel) "\uE9B8" else "\uE956",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily(Font(R.font.iconfont))
                        )
                    )
                    Text(
                        text = if (header.isModel) "Модель" else "Канал",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                SubscribeHeaderButton(
                    isSubscribed = isSubscribed,
                    isModel = header.isModel,
                    onClick = onToggleSubscription
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChannelHeaderProfileRowPreview() {
    ChannelHeaderProfileRow(
        header = ChannelHeaderModel(
            name = "Jane Doe",
            countryCode = "br",
            profileType = ProfileType.MODEL,
        ),
        isSubscribed = false,
        onToggleSubscription = {}
    )
}
