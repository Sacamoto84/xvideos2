package com.client.xvideos.r.ui.manager_block.molecule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.manager_block.atom.BlockedEmptyState
import com.client.xvideos.r.ui.manager_block.atom.BlockedUserRow

@Composable
fun BlockedUsersList(
    blockList: List<GifsInfo>,
    onUnblock: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    if (blockList.isEmpty()) {
        BlockedEmptyState(modifier = modifier)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            items(blockList, key = { it.id }) { item ->
                BlockedUserRow(
                    item = item,
                    onUnblock = onUnblock
                )
            }
        }
    }
}

@Preview
@Composable
private fun BlockedUsersListPreview() {
    BlockedUsersList(
        blockList = emptyList(),
        onUnblock = {}
    )
}
