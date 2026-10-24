package com.client.xvideos.x.feature.auth

import android.webkit.WebView
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вход пользователя на сайт X.
 *
 * Сессия сайта — cookie в хранилище WebView ([XWebViewCookieJar]); логин и
 * пароль — в зашифрованном [Settings.x_profile], как профиль L. Профиль
 * сохраняется только после удачного входа: по нему [restoreInBackground]
 * входит заново, когда сессия на сайте закончилась (сайт держит её около
 * месяца).
 *
 * Вход, выход и автоматический вход идут по очереди ([mutex]): иначе выход,
 * пришедший посреди входа, снял бы cookie, а вход следом сохранил бы профиль.
 */
@Singleton
class XSession @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    private val _signInView = MutableStateFlow<WebView?>(null)

    /** WebView идущего входа: форма держит его в своём окне, см. [signInThroughSitePage]. */
    val signInView: StateFlow<WebView?> = _signInView.asStateFlow()

    private val _restoreFailure = MutableStateFlow<String?>(null)

    /**
     * Почему сайт не принял сохранённый профиль при автоматическом входе;
     * `null` — отказа не было. Профиль при этом не стирается: отказ мог быть
     * временным, а войти заново пользователь может в настройках X.
     */
    val restoreFailure: StateFlow<String?> = _restoreFailure.asStateFlow()

    private var restoreJob: Job? = null

    /** Сессию в этом процессе уже проверили или открыли — автоматический вход не нужен. */
    @Volatile
    private var sessionSettled = false

    /**
     * Входит на сайт с [login] и [password]. Прежняя сессия закрывается:
     * страница входа должна показать пустую форму.
     *
     * @param onStep этап входа для формы; вызывается на главном потоке.
     */
    suspend fun signIn(login: String, password: String, onStep: (XSignInStep) -> Unit): XSignInResult {
        restoreJob?.cancelAndJoin()
        return mutex.withLock {
            clearXSessionCookies()
            val result = signInThroughSitePage(login, password, onStep) { _signInView.value = it }
            if (result == XSignInResult.Success) {
                Settings.x_profile.save(login, password)
                _restoreFailure.value = null
                sessionSettled = true
            }
            result
        }
    }

    /**
     * Выходит из профиля X: стирает логин и пароль сразу, сессию на сайте
     * закрывает в фоне — экран настроек к этому моменту может быть уже закрыт.
     */
    fun signOut() {
        restoreJob?.cancel()
        Settings.x_profile.clear()
        _restoreFailure.value = null
        sessionSettled = true
        scope.launch {
            mutex.withLock {
                requestXSignOut()
                clearXSessionCookies()
            }
        }
    }

    /**
     * Проверяет сессию по сохранённому профилю и, если сайт её уже не помнит,
     * входит заново. Работает в фоне, без окна; за процесс — до первого
     * определённого итога.
     */
    fun restoreInBackground() {
        if (sessionSettled || restoreJob?.isActive == true) return
        restoreJob = scope.launch { mutex.withLock { restore() } }
    }

    private suspend fun restore() {
        val saved = Settings.x_profile.field.value
        if (!saved.isComplete) {
            sessionSettled = true
            return
        }
        when (fetchXLoginStatus()) {
            true -> {
                sessionSettled = true
                return
            }
            // Сайт не ответил: проверим, когда X откроют в следующий раз.
            null -> return
            false -> Unit
        }

        clearXSessionCookies()
        when (val result = signInThroughSitePage(saved.login, saved.password, onStep = {}, onWebView = {})) {
            XSignInResult.Success -> sessionSettled = true
            is XSignInResult.Rejected -> {
                sessionSettled = true
                _restoreFailure.value = result.message
                SnackBar.warning("X: сайт не принял сохранённый пароль. Войдите заново в настройках X")
            }
            is XSignInResult.Failed -> Timber.w("X: автоматический вход не удался")
        }
    }
}
