package com.client.xvideos.xlive.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Вход на сайт для тестов: без WebView и сети. Выход и потеря сессии
 * считаются и снимают вход сразу.
 */
class TestAccount(signedIn: Boolean = true) : XLiveAccount {

    override val signedIn = MutableStateFlow(signedIn)
    override val followRevision = MutableStateFlow(0)

    var sessionLost = 0
        private set

    var signOuts = 0
        private set

    override fun onSessionLost() {
        sessionLost++
        signedIn.value = false
    }

    override fun onFollowChanged() {
        followRevision.update { it + 1 }
    }

    override fun signOut() {
        signOuts++
        signedIn.value = false
    }
}
