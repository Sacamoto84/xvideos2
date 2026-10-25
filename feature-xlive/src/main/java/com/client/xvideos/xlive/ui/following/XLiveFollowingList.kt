package com.client.xvideos.xlive.ui.following

import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.xlive.auth.XLiveAccount
import com.client.xvideos.xlive.model.XLiveFollowed
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.network.XLiveSignInRequiredException
import com.client.xvideos.xlive.ui.saved.XLiveSavedState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

/**
 * «Подписки» аккаунта: список с сайта, отписка долгим нажатием.
 *
 * Список грузится, когда подвкладку открыли и есть вход. Вход и выход
 * [account] список перечитывают или стирают сами; подписка или отписка в
 * комнате ([XLiveAccount.followRevision]) перечитывает уже загруженный список
 * тихо — пока «Подписки» не на экране.
 *
 * Держит список корень раздела, и [scope] у него его.
 *
 * @param loadFollowed подписки аккаунта.
 * @param setFollowing подписаться (`true`) или отписаться; ответ — подписка после запроса.
 * @param nowSeconds часы для метки снимков.
 */
class XLiveFollowingList(
    private val scope: CoroutineScope,
    private val account: XLiveAccount,
    private val loadFollowed: suspend () -> XLiveFollowed,
    private val setFollowing: suspend (String, Boolean) -> Boolean,
    private val nowSeconds: () -> Long,
) {

    private val _state = MutableStateFlow(XLiveFollowingState(isSignedIn = account.signedIn.value))
    val state: StateFlow<XLiveFollowingState> = _state.asStateFlow()

    /** Позиция сетки — у списка, а не в композиции, как у лент раздела. */
    val gridPosition = LazyGridPosition()

    /** Подвкладку открывали: после входа список нужен сразу. */
    private var wanted = false
    private var loaded = false
    private var loadJob: Job? = null

    init {
        scope.launch {
            account.signedIn.collect { signedIn ->
                if (signedIn) {
                    _state.update { it.copy(isSignedIn = true) }
                    if (wanted && !loaded) load()
                } else {
                    loadJob?.cancel()
                    loaded = false
                    _state.value = XLiveFollowingState(isSignedIn = false)
                }
            }
        }
        scope.launch {
            account.followRevision.drop(1).collect { if (loaded) load(quiet = true) }
        }
    }

    /** Подвкладку открыли: список грузится, если его ещё нет. */
    fun show() {
        wanted = true
        if (account.signedIn.value && !loaded && loadJob?.isActive != true) load()
    }

    /** Свайп вниз: список заново, старый на месте, пока не пришёл новый. */
    fun refresh() {
        if (account.signedIn.value) load()
    }

    /** «Повторить» после ошибки. */
    fun retry() {
        if (account.signedIn.value) load()
    }

    /**
     * Отписка от [username]: карточка уходит, как только сайт ответил, и
     * список перечитывается по [XLiveAccount.onFollowChanged].
     */
    fun unfollow(username: String) {
        scope.launch {
            try {
                setFollowing(username, false)
                Timber.i("XLive: отписка выполнена")
                _state.update { it.copy(list = it.list.without(username)) }
                account.onFollowChanged()
            } catch (e: CancellationException) {
                throw e
            } catch (e: XLiveSignInRequiredException) {
                Timber.w("XLive: отписка — %s", e.message)
                account.onSessionLost()
            } catch (e: IOException) {
                Timber.w("XLive: отписка не удалась — %s", e.javaClass.simpleName)
                EventBus.postEvent(Event.ShowSnackBar(UiMessage.Error("Не удалось отписаться")))
            }
        }
    }

    /**
     * Список с сайта. Пустой экран — со спиннером, при списке на экране —
     * с индикатором свайпа, [quiet] — без индикаторов.
     */
    private fun load(quiet: Boolean = false) {
        loadJob?.cancel()
        _state.update { current ->
            when {
                quiet -> current
                current.list.rooms.isEmpty() -> current.copy(isLoading = true, error = null)
                else -> current.copy(list = current.list.copy(isChecking = true), error = null)
            }
        }
        loadJob = scope.launch {
            try {
                val followed = loadFollowed()
                Timber.i("XLive: подписки — в эфире %d, не в эфире %d", followed.online.size, followed.offline.size)
                loaded = true
                _state.value = XLiveFollowingState(
                    isSignedIn = true,
                    list = followed.toSavedState(nowSeconds()),
                    online = followed.online.size,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: XLiveSignInRequiredException) {
                Timber.w("XLive: подписки — %s", e.message)
                account.onSessionLost()
            } catch (e: IOException) {
                Timber.w("XLive: подписки не получены — %s", e.javaClass.simpleName)
                onLoadFailed(quiet)
            }
        }
    }

    private fun onLoadFailed(quiet: Boolean) {
        // Тихое перечитывание индикаторов не ставило, и молча остаётся прежний список.
        if (quiet) return
        if (_state.value.list.rooms.isEmpty()) {
            _state.update { it.copy(isLoading = false, error = "Не удалось загрузить подписки") }
        } else {
            _state.update { it.copy(list = it.list.copy(isChecking = false)) }
            EventBus.postEvent(Event.ShowSnackBar(UiMessage.Error("Не удалось обновить подписки")))
        }
    }

    private companion object {

        /** Подписки сеткой «Моих»: сначала в эфире — в порядке сайта, потом остальные. */
        fun XLiveFollowed.toSavedState(stamp: Long): XLiveSavedState {
            val onlineNames = online.map { it.username }.toSet()
            val rooms = (online + offline.filterNot { it.username in onlineNames }).distinctBy { it.username }
            return XLiveSavedState(
                rooms = rooms.map { XLiveSavedRoom(username = it.username, age = it.age) }.toImmutableList(),
                online = rooms.associate { it.username to (it.username in onlineNames) }.toImmutableMap(),
                snapshotStamp = stamp,
            )
        }

        fun XLiveSavedState.without(username: String): XLiveSavedState =
            copy(rooms = rooms.filterNot { it.username == username }.toImmutableList())
    }
}
