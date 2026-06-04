package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch


@Composable
fun ScrollToTopButton(
    staggeredGridState: LazyStaggeredGridState
) {
    val scope = rememberCoroutineScope()

    FloatingActionButton(
        onClick = {
            scope.launch {
                staggeredGridState.scrollToItem(0)
            }
        },
        content = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top"
            )
        }
    )
}

@Preview
@Composable
fun ScrollToTopButtonPreview() {
    val staggeredGridState = rememberLazyStaggeredGridState()
    ScrollToTopButton(staggeredGridState = staggeredGridState)
}