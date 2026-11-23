package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123

internal class ScreenAccountSavedMedia(
    private val collectionId: String = "",
    private val title: String = "Лайки RedGIFs",
) : Screen {
    override val key: ScreenKey = "RedAccountSavedMedia:$collectionId"

    @Composable
    override fun Content() {
        val vm = getScreenModel<ScreenAccountSavedMediaSM, ScreenAccountSavedMediaSM.Factory> { factory ->
            factory.create(collectionId)
        }
        val navigator = LocalNavigator.currentOrThrow
        val state by vm.state.collectAsStateWithLifecycle()
        val columnSetting = if (collectionId.isEmpty()) Settings.r_likesTab_column_current_count
        else Settings.r_collectionTab_column_current_count
        val columns by columnSetting.field.collectAsStateWithLifecycle()

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            AccountSavedHeader(title = title, onBack = { navigator.pop() })
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (val content = state) {
                    AccountSavedContent.Loading -> CircularProgressIndicator()
                    AccountSavedContent.SignedOut -> AccountSavedMessage("Войдите в RedGIFs в настройках R")
                    is AccountSavedContent.Ready -> {
                        val host = content.value
                        LaunchedEffect(host, columns) { host.columns = normalizeRColumnCount(columns) }
                        LazyRow123(
                            host = host,
                            modifier = Modifier.fillMaxSize(),
                            onClickOpenProfile = { username ->
                                host.currentIndexGoto = host.currentIndex
                                navigator.push(ScreenRedProfile(username))
                            },
                            emptyMessage = if (collectionId.isEmpty()) "У вас пока нет лайков на RedGIFs"
                            else "В этой коллекции пока нет публикаций",
                        )
                    }
                }
            }
        }
    }
}
