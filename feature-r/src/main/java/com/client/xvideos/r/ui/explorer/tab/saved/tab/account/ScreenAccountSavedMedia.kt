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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountSavedHeader
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountSignInPrompt
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host

internal class ScreenAccountSavedMedia(
    private val collectionId: String = "",
    private val title: String = "",
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

        ScreenAccountSavedMediaContent(
            title = title.ifEmpty { "Лайки R" },
            collectionId = collectionId,
            state = state,
            columns = columns,
            onBack = { navigator.pop() },
            onOpenProfile = { username -> navigator.push(ScreenRedProfile(username)) },
        )
    }
}

@Composable
internal fun ScreenAccountSavedMediaContent(
    title: String,
    collectionId: String,
    state: AccountSavedContent<LazyRow123Host>,
    columns: Int,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Theme.background)) {
        AccountSavedHeader(title = title, onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (state) {
                AccountSavedContent.Loading -> CircularProgressIndicator()
                AccountSavedContent.SignedOut -> AccountSignInPrompt()
                is AccountSavedContent.Ready -> {
                    val host = state.value
                    LaunchedEffect(host, columns) { host.columns = normalizeRColumnCount(columns) }
                    LazyRow123(
                        host = host,
                        modifier = Modifier.fillMaxSize(),
                        onClickOpenProfile = { username ->
                            host.currentIndexGoto = host.currentIndex
                            onOpenProfile(username)
                        },
                        emptyMessage = if (collectionId.isEmpty()) "У вас пока нет лайков в R"
                        else "В этой коллекции пока нет публикаций",
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScreenAccountSavedMediaPreview() {
    ScreenAccountSavedMediaContent(
        title = "Лайки R",
        collectionId = "",
        state = AccountSavedContent.SignedOut,
        columns = 2,
        onBack = {},
        onOpenProfile = {},
    )
}
