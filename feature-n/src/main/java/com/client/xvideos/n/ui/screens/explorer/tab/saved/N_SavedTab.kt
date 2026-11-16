package com.client.xvideos.n.ui.screens.explorer.tab.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка сохранённой манги (локальные закладки).
 */
object N_SavedTab : Screen {

    private fun readResolve(): Any = N_SavedTab

    override val key: ScreenKey = "N_SavedTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_SavedTabSM>()

        val columns by vm.columns.collectAsStateWithLifecycle()
        val favorites = vm.favorites

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        N_SavedTabContent(
            favorites = favorites,
            columns = columns,
            onGalleryClick = onGalleryClick
        )
    }
}

@Composable
fun N_SavedTabContent(
    favorites: List<NGalleryPreview>,
    columns: Int,
    onGalleryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            if (favorites.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "В закладках пусто",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Нажмите на значок закладки на экране манги, чтобы сохранить её сюда.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                NGalleryGrid(
                    items = favorites,
                    columns = columns,
                    onItemClick = onGalleryClick,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview
@Composable
private fun N_SavedTabContentPreview() {
    N_SavedTabContent(
        favorites = emptyList(),
        columns = 2,
        onGalleryClick = {}
    )
}
