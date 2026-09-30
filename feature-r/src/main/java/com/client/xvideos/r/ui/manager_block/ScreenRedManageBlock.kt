package com.client.xvideos.r.ui.manager_block

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.manager_block.atom.BottomrBar
import com.client.xvideos.r.ui.manager_block.atom.ManageBlockTopBar
import com.client.xvideos.r.ui.manager_block.molecule.BlockedUsersList

class ScreenRedManageBlock : Screen {

    override val key: ScreenKey = "ScreenRedManageBlock"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenRedManageBlockSM = getScreenModel()
        val blockList by vm.blockList.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        val onUnblock: (GifsInfo) -> Unit = remember(vm) { { vm.unblock(it) } }

        BackHandler(onBack = onBack)

        ScreenRedManageBlockContent(
            blockList = blockList,
            listState = listState,
            onBack = onBack,
            onUnblock = onUnblock
        )
    }
}

@Composable
fun ScreenRedManageBlockContent(
    blockList: List<GifsInfo>,
    listState: LazyListState,
    onBack: () -> Unit,
    onUnblock: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ManageBlockTopBar(onBack = onBack) },
        bottomBar = { BottomrBar() }
    ) { padding ->
        BlockedUsersList(
            blockList = blockList,
            onUnblock = onUnblock,
            listState = listState,
            modifier = Modifier.padding(padding)
        )
    }
}

@Preview
@Composable
private fun ScreenRedManageBlockPreview() {
    ScreenRedManageBlockContent(
        blockList = emptyList(),
        listState = rememberLazyListState(),
        onBack = {},
        onUnblock = {}
    )
}
