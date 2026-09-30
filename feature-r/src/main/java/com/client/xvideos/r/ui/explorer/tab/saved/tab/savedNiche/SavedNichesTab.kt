package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.molecule.DialogNicheDelete
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.molecule.SavedNicheRow
import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.ui.theme.XvideosTheme

object SavedNichesTab : Screen {

    private fun readResolve(): Any = SavedNichesTab

    override val key: ScreenKey = "SavedNichesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedNichesSM = getScreenModel()
        val state = rememberLazyListState()

        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
            gridState = state, itemsToIgnore = 0
        )

        var itemPendingDelete by remember { mutableStateOf<NichesInfo?>(null) }
        val onDismissDelete = remember { { itemPendingDelete = null } }
        val onConfirmDelete = remember(vm) {
            { pending: NichesInfo ->
                vm.savedRed.niches.remove(pending)
                itemPendingDelete = null
            }
        }

        BackHandler(enabled = itemPendingDelete != null, onBack = onDismissDelete)

        DialogNicheDelete(
            item = itemPendingDelete,
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )

        val onNicheClick: (NichesInfo) -> Unit = remember(navigator) {
            { item -> navigator.push(R_ScreenNiche(item.id)) }
        }
        val onDeleteClick: (NichesInfo) -> Unit = remember {
            { item -> itemPendingDelete = item }
        }
        val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

        SavedNichesTabContent(
            niches = vm.savedRed.niches.list,
            state = state,
            topInset = getTopInsetDp(),
            scrollPercentProvider = scrollPercentProvider,
            onNicheClick = onNicheClick,
            onDeleteClick = onDeleteClick
        )
    }
}

@Composable
fun SavedNichesTabContent(
    niches: List<NichesInfo>,
    state: LazyListState,
    topInset: Dp,
    scrollPercentProvider: () -> Pair<Float, Float>,
    onNicheClick: (NichesInfo) -> Unit,
    onDeleteClick: (NichesInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .padding(top = topInset)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Группы",
                    color = Theme.R.colorYellow,
                    fontSize = 18.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular
                )
            }
        },
        containerColor = Theme.background,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize()
            ) {
                items(niches, key = { it.id }, contentType = { "saved_niche" }) { item ->
                    SavedNicheRow(
                        item = item,
                        onClick = onNicheClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.CenterEnd)
            ) {
                VerticalScrollbar(scrollPercentProvider)
            }
        }
    }
}

@Preview
@Composable
private fun SavedNichesTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        SavedNichesTabContent(
            niches = listOf(
                NichesInfo(id = "1", name = "Sample Group", thumbnail = "")
            ),
            state = rememberLazyListState(),
            topInset = 24.dp,
            scrollPercentProvider = { 0f to 1f },
            onNicheClick = {},
            onDeleteClick = {}
        )
    }
}
