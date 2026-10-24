package com.client.xvideos.x.screens.login

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.settings.element.SavedCredentials
import com.client.xvideos.x.feature.auth.XSignInResult
import com.client.xvideos.x.feature.auth.XSignInStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Вход в X: логин, пароль и колбэк этапа — как у `XSession.signIn`. */
typealias XSignIn = suspend (login: String, password: String, onStep: (XSignInStep) -> Unit) -> XSignInResult

/**
 * Форма входа X: набранные логин и пароль и ход входа.
 *
 * Живёт в ScreenModel экрана, который показывает форму, как `LLoginFormState`:
 * переживает пересоздание активности и не покидает процесс. В отличие от L
 * форма X не просто сохраняет логин и пароль, а сразу входит на сайт — профиль
 * сохраняется только после того, как сайт его принял.
 *
 * @param saved сохранённый профиль на момент создания — см. [prefill].
 * @param scope где идёт вход; закрылся экран — вход отменяется.
 * @param signIn сам вход.
 */
@Stable
class XLoginFormState(
    saved: SavedCredentials,
    private val scope: CoroutineScope,
    private val signIn: XSignIn,
) {
    var login by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    /** Этап идущего входа; `null` — вход не идёт. */
    var step by mutableStateOf<XSignInStep?>(null)
        private set

    /** Почему последний вход не удался; `null` — ошибки нет. */
    var error by mutableStateOf<String?>(null)
        private set

    /** Сайт принял логин и пароль — форму пора закрыть. */
    var signedIn by mutableStateOf(false)
        private set

    private var job: Job? = null

    /**
     * Номер попытки входа. Отменённая попытка доходит до `finally` позже, чем
     * началась следующая, и не должна трогать её этап и итог.
     */
    private var attempt = 0

    init {
        prefill(saved)
    }

    fun updateLogin(value: String) {
        login = value
        error = null
    }

    fun updatePassword(value: String) {
        password = value
        error = null
    }

    /** Входит с набранными логином и паролем. Пока вход идёт, повторное нажатие ничего не делает. */
    fun submit() {
        if (job?.isActive == true) return
        val normalizedLogin = login.trim()
        if (normalizedLogin.isBlank() || password.isBlank()) {
            error = "Введите логин и пароль X"
            return
        }
        error = null
        step = XSignInStep.OPENING
        val current = ++attempt
        job = scope.launch {
            try {
                val result = signIn(normalizedLogin, password) { if (current == attempt) step = it }
                if (current == attempt) {
                    when (result) {
                        XSignInResult.Success -> signedIn = true
                        is XSignInResult.Rejected -> error = result.message
                        is XSignInResult.Failed -> error = result.message
                    }
                }
            } finally {
                if (current == attempt) step = null
            }
        }
    }

    /**
     * Готовит форму к показу: набранное раньше стирается, логин берётся из
     * сохранённого профиля. Его подставляем всегда: форма открывается и тогда,
     * когда сайт не принял сохранённый пароль, — вводить остаётся только пароль.
     * Пароль в форму не копируется: незачем держать его в памяти второй раз.
     */
    fun prefill(saved: SavedCredentials) {
        cancel()
        login = saved.login
        password = ""
        error = null
        signedIn = false
    }

    /** Закрытие формы: вход отменяется, набранное стирается. */
    fun clear() {
        cancel()
        login = ""
        password = ""
        error = null
        signedIn = false
    }

    private fun cancel() {
        attempt++
        job?.cancel()
        job = null
        step = null
    }
}
