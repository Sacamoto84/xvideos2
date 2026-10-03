package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ProfileType

/**
 * Баннер шапки канала.
 *
 * @param topInset Вырез камеры: баннер выше на него и заходит под вырез.
 */
@Composable
fun ChannelHeaderBanner(
    header: ChannelHeaderModel,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp + topInset)
    ) {
        if (header.hasBanner) {
            UrlImage(
                url = header.bannerUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (header.isModel) {
                                listOf(Color(0xFF5A101C), Color(0xFF1F080C))
                            } else {
                                listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                            }
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF040404))
                    )
                )
        )
    }
}

@Preview
@Composable
private fun ChannelHeaderBannerPreview() {
    ChannelHeaderBanner(
        header = ChannelHeaderModel(name = "Sample Model", profileType = ProfileType.MODEL),
        topInset = 24.dp,
    )
}
