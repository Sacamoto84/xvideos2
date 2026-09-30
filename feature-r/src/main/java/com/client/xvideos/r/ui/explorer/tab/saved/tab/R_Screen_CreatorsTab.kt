package com.client.xvideos.r.ui.explorer.tab.saved.tab

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
import androidx.compose.ui.text.style.TextAlign
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
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.CreatorListItem
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.DeleteCreatorDialog
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.ui.theme.XvideosTheme

object R_Screen_CreatorsTab : Screen {

    private fun readResolve(): Any = R_Screen_CreatorsTab

    override val key: ScreenKey = "R_Screen_CreatorsTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedCreatorSM>()
        val state = rememberLazyListState()
        val savedRed = vm.savedRed

        var itemPendingDelete by remember { mutableStateOf<UserInfo?>(null) }
        val onDismissDelete = remember { { itemPendingDelete = null } }

        BackHandler(enabled = itemPendingDelete != null, onBack = onDismissDelete)

        val onCreatorClick = remember(navigator) {
            { username: String -> navigator.push(ScreenRedProfile(username)) }
        }
        val onDeleteRequest = remember {
            { user: UserInfo -> itemPendingDelete = user }
        }
        val onConfirmDelete = remember(savedRed) {
            { pending: UserInfo ->
                savedRed.creators.remove(pending.username)
                itemPendingDelete = null
            }
        }

        DeleteCreatorDialog(
            item = itemPendingDelete,
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )

        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
            gridState = state, itemsToIgnore = 0
        )
        val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

        CreatorsTabContent(
            creators = savedRed.creators.list,
            state = state,
            topInset = getTopInsetDp(),
            scrollPercentProvider = scrollPercentProvider,
            onCreatorClick = onCreatorClick,
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
fun CreatorsTabContent(
    creators: List<UserInfo>,
    state: LazyListState,
    topInset: Dp,
    scrollPercentProvider: () -> Pair<Float, Float>,
    onCreatorClick: (String) -> Unit,
    onDeleteRequest: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Theme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset)
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Авторы",
                    modifier = Modifier,
                    color = Theme.R.colorYellow,
                    fontSize = 18.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    textAlign = TextAlign.Center
                )
            }
        }
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
                items(
                    items = creators,
                    key = { it.username },
                    contentType = { "creator_item" }
                ) { item ->
                    CreatorListItem(
                        item = item,
                        onClick = onCreatorClick,
                        onDelete = onDeleteRequest
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .width(2.dp)
            ) {
                VerticalScrollbar(scrollPercentProvider)
            }
        }
    }
}

@Preview
@Composable
private fun CreatorsTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        val sampleUser = UserInfo(
            name = "Sample Creator",
            username = "samplecreator",
            profileImageUrl = "https://via.placeholder.com/96",
            followers = 21_193,
            views = 32_986_108,
            publishedGifs = 2_176,
            url = "https://example.com/samplecreator"
        )
        CreatorsTabContent(
            creators = listOf(sampleUser),
            state = rememberLazyListState(),
            topInset = 24.dp,
            scrollPercentProvider = { 0f to 1f },
            onCreatorClick = {},
            onDeleteRequest = {}
        )
    }
}
