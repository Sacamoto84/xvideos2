package com.client.xvideos.x.feature.auth

/** Чем закончился вход в X. */
sealed interface XSignInResult {

    /** Сайт принял логин и пароль, сессия открыта. */
    data object Success : XSignInResult

    /**
     * Сайт отказал в логине и пароле: неверная пара, заблокированный аккаунт.
     *
     * @property message текст отказа с сайта.
     */
    data class Rejected(val message: String) : XSignInResult

    /**
     * До ответа о логине и пароле не дошло: нет сети, капча не прошла,
     * страница входа не загрузилась или изменилась. Повтор может помочь.
     *
     * @property message что случилось, для пользователя.
     */
    data class Failed(val message: String) : XSignInResult
}
