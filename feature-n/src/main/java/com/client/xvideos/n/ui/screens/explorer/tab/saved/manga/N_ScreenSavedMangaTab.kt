package com.client.xvideos.n.ui.screens.explorer.tab.saved.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.screens.explorer.tab.saved.N_SavedTabSM
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

fun N_ScreenSavedMangaTab_AddColumn() {
    ColumnSelect_AddColumn(Settings.n_saved_column_current_count, Settings.n_galleriesTab_G_0_4)
}

/**
 * Вкладка сохранённой манги целиком (локальные закладки альбомов).
 */
object N_ScreenSavedMangaTab : Screen {

    private fun readResolve(): Any = N_ScreenSavedMangaTab

    override val key: ScreenKey = "N_ScreenSavedMangaTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_SavedTabSM>()

        val columns by vm.columns.collectAsStateWithLifecycle()
        val favorites = vm.favorites

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        N_ScreenSavedMangaTabContent(
            favorites = favorites,
            columns = columns,
            onGalleryClick = onGalleryClick
        )
    }
}

@Composable
fun N_ScreenSavedMangaTabContent(
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Нажмите на значок закладки на экране манги, чтобы сохранить её сюда.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp
                    )
                }
            } else {
                NGalleryGrid(
                    items = favorites,
                    columns = columns,
                    onItemClick = onGalleryClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview
@Composable
private fun N_ScreenSavedMangaTabContentPreview() {
    N_ScreenSavedMangaTabContent(
        favorites = emptyList(),
        columns = 2,
        onGalleryClick = {}
    )
}
