package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.network.api.AccountNicheList
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.ui.explorer.tab.niches.molecule.NichePreview2Content
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountMembershipMenu
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSignInPrompt
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

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            SavedTabHeader(
                title = "Группы RedGIFs",
                topCutout = getTopInsetDp(),
                navigation = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                trailing = {
                    if (state.content != AccountNichesContent.SignedOut) {
                        IconButton(onClick = vm::refresh, enabled = state.content != AccountNichesContent.Loading) {
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
                            onClick = { vm.select(list) },
                            text = { Text(if (list == AccountNicheList.FOLLOWING) "Участвую" else "Созданные") },
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (val content = state.content) {
                    AccountNichesContent.Loading -> CircularProgressIndicator()
                    AccountNichesContent.SignedOut -> AccountSignInPrompt()
                    is AccountNichesContent.Failed -> RListLoadError(message = content.message, onRetry = vm::refresh)
                    is AccountNichesContent.Loaded -> if (content.niches.isEmpty()) {
                        AccountNichesMessage(
                            if (state.list == AccountNicheList.FOLLOWING) "Вы пока не вступили ни в одну группу RedGIFs"
                            else "У вас пока нет созданных групп RedGIFs"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
                        ) {
                            items(content.niches, key = { it.id }) { niche ->
                                NichePreview2Content(
                                    niche = niche,
                                    isFollowed = state.list == AccountNicheList.FOLLOWING,
                                    onClick = { navigator.push(R_ScreenNiche(niche.id)) },
                                    modifier = Modifier.adaptiveContentWidth(),
                                    trailing = {
                                        if (state.list == AccountNicheList.FOLLOWING) {
                                            AccountMembershipMenu(AccountMembership.NICHE, niche.id, knownMember = true)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountNichesMessage(message: String) {
    Text(message, modifier = Modifier.padding(horizontal = 20.dp), color = Color.White, textAlign = TextAlign.Center)
}
