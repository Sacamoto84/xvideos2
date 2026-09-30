package com.client.xvideos.l.ui.screens.explorer.tab.saved.likes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.molecule.LikesFilterSegmentedRow
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

fun L_ScreenSavedLikesTab_AddColumn() {
    ColumnSelect_AddColumn(Settings.l_likesTab_column_current_count, Settings.l_likesTab_G_0_4)
}

val LIKES_FILTER_OPTIONS: ImmutableList<String> = persistentListOf("All", "Image", "Gif")

object L_ScreenSavedLikesTab : Screen {

    private fun readResolve(): Any = L_ScreenSavedLikesTab

    override val key: ScreenKey = "L_ScreenSavedLikesTab"

    @Composable
    override fun Content() {

        val vm: ScreenSavedLLikesSM = getScreenModel()

        val column by Settings.l_likesTab_column_current_count.field.collectAsStateWithLifecycle()

        val topInset = getTopInsetDp()

        LaunchedEffect(column) {
            if (column != 0) {
                vm.host.columns = column
            }
        }

        var selectedIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(vm.original.size, selectedIndex) {
            vm.filterSelect(selectedIndex)
        }

        val onSelectFilterIndex = remember { { index: Int -> selectedIndex = index } }

        SavedLikesTabContent(
            host = vm.host,
            selectedIndex = selectedIndex,
            onSelectFilterIndex = onSelectFilterIndex,
            topInset = topInset
        )

    }

}

@Composable
fun SavedLikesTabContent(
    host: LazyRowPictureDetailsHost,
    selectedIndex: Int,
    onSelectFilterIndex: (Int) -> Unit,
    topInset: Dp,
    modifier: Modifier = Modifier
) {
    val options = LIKES_FILTER_OPTIONS

    val onBackToAll = remember(onSelectFilterIndex) { { onSelectFilterIndex(0) } }
    BackHandler(enabled = selectedIndex != 0, onBack = onBackToAll)

    val renderItemBefore: @Composable () -> Unit = remember(options, selectedIndex, onSelectFilterIndex, topInset) {
        {
            LikesFilterSegmentedRow(
                options = options,
                selectedIndex = selectedIndex,
                onSelectIndex = onSelectFilterIndex,
                topInset = topInset
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Theme.background)) {
        L_LazyRowPictureDetails(
            host = host,
            expandMenu = ExpandMenuType.LIKES,
            tag = "lLikes",
            itemBefore = renderItemBefore
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SavedLikesTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        SavedLikesTabContent(
            host = remember { LazyRowPictureDetailsHost("preview_likes") },
            selectedIndex = 0,
            onSelectFilterIndex = {},
            topInset = Dp(24f)
        )
    }
}
