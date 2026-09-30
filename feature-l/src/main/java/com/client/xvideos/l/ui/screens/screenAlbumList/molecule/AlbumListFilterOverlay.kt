package com.client.xvideos.l.ui.screens.screenAlbumList.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.AlbumListFilter as LAlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.AlbumListFilter

@Composable
fun AlbumListFilterOverlay(
    visible: Boolean,
    filter: LAlbumListFilter,
    filterGCount: List<AlbumListFilterGenreCountResponse>?,
    filterTagsCount: List<AlbumListFilterGenreCountResponse>?,
    onClose: () -> Unit,
    onApply: (LAlbumListFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClose)
            )

            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = screenHeight * 0.9f)
                    .wrapContentHeight()
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFF171717),
                shadowElevation = 8.dp
            ) {
                AlbumListFilter(
                    filter = filter,
                    filterGCount = filterGCount,
                    filterTagsCount = filterTagsCount,
                    onClose = onClose,
                    onFilterApply = onApply
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlbumListFilterOverlayPreview() {
    AlbumListFilterOverlay(
        visible = true,
        filter = LAlbumListFilter(),
        filterGCount = emptyList(),
        filterTagsCount = emptyList(),
        onClose = {},
        onApply = {}
    )
}
