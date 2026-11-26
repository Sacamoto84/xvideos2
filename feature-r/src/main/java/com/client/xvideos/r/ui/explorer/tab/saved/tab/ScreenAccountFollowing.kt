package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountSignInPrompt
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.AccountFollowingList
import com.client.xvideos.r.ui.profile.ScreenRedProfile

internal object ScreenAccountFollowing : Screen {
    override val key: ScreenKey = "RedAccountFollowing"

    private fun readResolve(): Any = ScreenAccountFollowing

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenAccountFollowingSM>()
        val state by vm.state.collectAsStateWithLifecycle()
        val ready = state as? AccountFollowingState.Ready
        val creators = ready?.let { key(it) { it.creators.collectAsLazyPagingItems() } }
        val onOpenProfile: (String) -> Unit = remember(navigator) { { navigator.push(ScreenRedProfile(it)) } }

        ScreenAccountFollowingContent(
            signedOut = state == AccountFollowingState.SignedOut,
            creators = creators,
            readyKey = ready,
            onBack = { navigator.pop() },
            onOpenProfile = onOpenProfile,
        )
    }
}

@Composable
internal fun ScreenAccountFollowingContent(
    signedOut: Boolean,
    creators: LazyPagingItems<UserInfo>?,
    readyKey: Any?,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Theme.background)) {
        SavedTabHeader(
            title = "Подписки R",
            topCutout = getTopInsetDp(),
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            trailing = {
                if (creators != null) {
                    IconButton(onClick = creators::refresh, enabled = creators.loadState.refresh !is LoadState.Loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить список авторов")
                    }
                }
            },
        )

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                signedOut -> AccountSignInPrompt()
                creators == null -> CircularProgressIndicator()
                else -> key(readyKey) { AccountFollowingList(creators, onOpenProfile) }
            }
        }
    }
}

@Preview
@Composable
private fun ScreenAccountFollowingPreview() {
    ScreenAccountFollowingContent(
        signedOut = true,
        creators = null,
        readyKey = null,
        onBack = {},
        onOpenProfile = {},
    )
}
