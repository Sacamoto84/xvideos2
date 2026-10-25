package com.client.xvideos.xlive.auth

import com.client.xvideos.xlive.network.XLiveApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException

/**
 * Вход на сайт XLive на процесс.
 *
 * Сессия — cookie [SESSION_COOKIE] в хранилище WebView: её ставит страница
 * входа сайта ([com.client.xvideos.xlive.ui.login.ScreenXLiveLogin]), а прямые
 * запросы раздела берут её оттуда же ([XLiveWebViewCookieJar]). Логин и
 * пароль приложение не видит и не хранит: их вводит пользователь на странице
 * сайта.
 *
 * Объект, а не Hilt-синглтон, по той же причине, что `XLiveSaved`: графа
 * зависимостей у раздела нет.
 */
object XLiveSession : XLiveAccount {

    private val scope = CoroutineScope(SupervisorJob())

    // Хранилище cookie читается при первом обращении: до открытия раздела WebView не нужен.
    private val _signedIn by lazy { MutableStateFlow(xLiveCookie(SESSION_COOKIE) != null) }

    override val signedIn: StateFlow<Boolean> get() = _signedIn.asStateFlow()

    private val _followRevision = MutableStateFlow(0)
    override val followRevision: StateFlow<Int> = _followRevision.asStateFlow()

    /** Выход на сайте и снятие cookie идут под замком: вход ждёт, пока он закончится. */
    private val signOutLock = Mutex()

    /** Перед входом: прежний выход должен дойти до конца, иначе он снимет новую сессию. */
    suspend fun awaitSignOut() {
        signOutLock.withLock { }
    }

    /**
     * Страница входа ушла со служебных страниц с cookie сессии: узнаёт ли сайт
     * сессию — по счётчику подписок. `true` — вход есть и [signedIn] уже `true`.
     */
    suspend fun confirmSignIn(): Boolean = try {
        XLiveApi.followedCount()
        Timber.i("XLive: вход выполнен")
        _signedIn.value = true
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.w("XLive: вход не подтверждён — %s", e.message)
        false
    }

    override fun onSessionLost() {
        if (!_signedIn.value) return
        Timber.i("XLive: сайт не узнал сессию")
        _signedIn.value = false
        scope.launch { clearXLiveSessionCookie() }
    }

    override fun onFollowChanged() {
        _followRevision.update { it + 1 }
    }

    override fun signOut() {
        _signedIn.value = false
        // Замок берётся прямо в вызове: экран входа, открытый следом, уже его ждёт.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            signOutLock.withLock { closeSession() }
        }
    }

    private suspend fun closeSession() {
        try {
            XLiveApi.signOut()
            Timber.i("XLive: выход выполнен")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w("XLive: выход на сайте не подтверждён — %s", e.javaClass.simpleName)
        }
        clearXLiveSessionCookie()
    }
}
