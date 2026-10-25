package com.client.xvideos.xlive.ui.login

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.xlive.auth.XLiveSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Экран входа: когда открыть форму и когда вход подтверждён.
 *
 * @param awaitSignOut ждёт конца прежнего выхода; подменяется в тестах.
 * @param confirmSignIn спрашивает сайт, узнаёт ли он сессию; подменяется в тестах.
 */
class XLiveLoginSM(
    awaitSignOut: suspend () -> Unit = XLiveSession::awaitSignOut,
    private val confirmSignIn: suspend () -> Boolean = XLiveSession::confirmSignIn,
) : ScreenModel {

    private val _ready = MutableStateFlow(false)

    /** Форму можно открывать: выход, нажатый только что, закончился. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _done = MutableStateFlow(false)

    /** Сайт подтвердил вход — экран закрывается. */
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private var confirmJob: Job? = null

    init {
        screenModelScope.launch {
            awaitSignOut()
            _ready.value = true
        }
    }

    /**
     * WebView загрузил страницу сайта вне служебных страниц входа. Пока идёт
     * одна проверка, следующая не начинается; не подтвердилось — проверим на
     * следующей странице.
     */
    fun onLeftSignInPages() {
        if (_done.value || confirmJob?.isActive == true) return
        confirmJob = screenModelScope.launch {
            if (confirmSignIn()) _done.value = true
        }
    }
}
