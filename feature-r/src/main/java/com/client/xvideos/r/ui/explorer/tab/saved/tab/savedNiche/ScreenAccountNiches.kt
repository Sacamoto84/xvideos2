package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.network.api.AccountNicheList
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountSignInPrompt
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.molecule.AccountNichesList
import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError

internal object ScreenAccountNiches : Screen {
    override val key: ScreenKey = "RedAccountNiches"

    private fun readResolve(): Any = ScreenAccountNiches

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenAccountNichesSM>()
        val state by vm.state.collectAsStateWithLifecycle()

        ScreenAccountNichesContent(
            state = state,
            onBack = { navigator.pop() },
            onRefresh = vm::refresh,
            onSelectList = vm::select,
            onOpenNiche = { navigator.push(R_ScreenNiche(it)) },
        )
    }
}

@Composable
internal fun ScreenAccountNichesContent(
    state: AccountNichesState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectList: (AccountNicheList) -> Unit,
    onOpenNiche: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Theme.background)) {
        SavedTabHeader(
            title = "Группы R",
            topCutout = getTopInsetDp(),
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            trailing = {
                if (state.content != AccountNichesContent.SignedOut) {
                    IconButton(onClick = onRefresh, enabled = state.content != AccountNichesContent.Loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить список групп")
                    }
                }
            },
        )

        if (state.content != AccountNichesContent.SignedOut) {
            PrimaryTabRow(selectedTabIndex = state.list.ordinal, containerColor = Theme.tabLevel1) {
                AccountNicheList.entries.forEach { list ->
                    Tab(
                        selected = list == state.list,
                        onClick = { onSelectList(list) },
                        text = { Text(if (list == AccountNicheList.FOLLOWING) "Участвую" else "Созданные") },
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (val content = state.content) {
                AccountNichesContent.Loading -> CircularProgressIndicator()
                AccountNichesContent.SignedOut -> AccountSignInPrompt()
                is AccountNichesContent.Failed -> RListLoadError(message = content.message, onRetry = onRefresh)
                is AccountNichesContent.Loaded -> AccountNichesList(
                    niches = content.niches,
                    list = state.list,
                    onOpenNiche = onOpenNiche,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScreenAccountNichesPreview() {
    ScreenAccountNichesContent(
        state = AccountNichesState(AccountNicheList.FOLLOWING, AccountNichesContent.SignedOut),
        onBack = {},
        onRefresh = {},
        onSelectList = {},
        onOpenNiche = {},
    )
}
