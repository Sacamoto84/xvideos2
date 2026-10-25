package com.client.xvideos.xlive.ui.following.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.following.XLiveFollowingActions
import com.client.xvideos.xlive.ui.following.XLiveFollowingState
import com.client.xvideos.xlive.ui.following.atom.XLiveSignInPrompt
import com.client.xvideos.xlive.ui.room.atom.XLiveMessage
import com.client.xvideos.xlive.ui.saved.XLiveSavedActions
import com.client.xvideos.xlive.ui.saved.XLiveSavedState
import com.client.xvideos.xlive.ui.saved.molecule.XLiveSavedGrid
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Подвкладка «Подписки»: без входа — кнопка «Войти», со входом — строка
 * аккаунта и подписки сеткой «Моих». Долгое нажатие на карточку — отписка.
 *
 * @param gridActions действия сетки: открыть комнату, отписаться, свайп вниз.
 */
@Composable
fun XLiveFollowingContent(
    state: XLiveFollowingState,
    gridState: LazyGridState,
    actions: XLiveFollowingActions,
    gridActions: XLiveSavedActions,
    modifier: Modifier = Modifier,
) {
    when {
        !state.isSignedIn -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            XLiveSignInPrompt(onSignIn = actions.onSignIn)
        }
        state.isLoading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = XLiveColors.Accent)
        }
        state.error != null -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            XLiveMessage(text = state.error, onRetry = actions.onRetry)
        }
        else -> Column(modifier = modifier.fillMaxSize()) {
            XLiveAccountBar(online = state.online, total = state.list.rooms.size, onSignOut = actions.onSignOut)
            XLiveSavedGrid(
                state = state.list,
                gridState = gridState,
                actions = gridActions,
                emptyText = "Подписок на сайте пока нет. Подписаться можно в шапке комнаты",
                listName = "подписок на сайте",
            )
        }
    }
}

@Preview
@Composable
private fun XLiveFollowingContentPreview() {
    XLiveFollowingContent(
        state = XLiveFollowingState(
            isSignedIn = true,
            list = XLiveSavedState(
                rooms = persistentListOf(XLiveSavedRoom("room_a", 24), XLiveSavedRoom("room_b")),
                online = persistentMapOf("room_a" to true, "room_b" to false),
            ),
            online = 1,
        ),
        gridState = rememberLazyGridState(),
        actions = XLiveFollowingActions(),
        gridActions = XLiveSavedActions(),
    )
}
