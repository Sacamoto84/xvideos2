package com.client.xvideos.x.screens.dashboards.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.x.screens.history.ScreenXHistory
import com.client.xvideos.x.screens.saved.X_SavedContent
import com.client.xvideos.x.screens.subscriptions.X_SubscriptionsContent

const val SAVED_TAB_FAVORITES = 0
const val SAVED_TAB_DOWNLOADS = 1
const val SAVED_TAB_HISTORY = 2
const val SAVED_TAB_CHANNELS = 3
const val SAVED_TAB_MODELS = 4

@Composable
fun SavedTabContent(
    savedTab: Int,
    favoritesScreen: ScreenFavorites,
    saved: SavedX? = null,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current || saved == null) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Saved Tab Preview", color = Color.White)
        }
        return
    }

    when (savedTab) {
        SAVED_TAB_FAVORITES -> favoritesScreen.Content()
        SAVED_TAB_DOWNLOADS -> X_SavedContent(saved, modifier = modifier)
        SAVED_TAB_HISTORY -> ScreenXHistory(saved, modifier = modifier)
        SAVED_TAB_CHANNELS -> X_SubscriptionsContent(saved = saved, isModel = false, modifier = modifier)
        SAVED_TAB_MODELS -> X_SubscriptionsContent(saved = saved, isModel = true, modifier = modifier)
        else -> favoritesScreen.Content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SavedTabContentPreview() {
    SavedTabContent(
        savedTab = SAVED_TAB_FAVORITES,
        favoritesScreen = ScreenFavorites(),
        saved = null
    )
}
