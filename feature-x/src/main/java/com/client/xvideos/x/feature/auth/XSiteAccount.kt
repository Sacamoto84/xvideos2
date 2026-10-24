package com.client.xvideos.x.feature.auth

import com.client.xvideos.x.feature.net.logLabel
import com.client.xvideos.x.feature.net.postFormData
import com.client.xvideos.x.urlStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Вошёл ли пользователь на сайт X по cookie текущей сессии.
 *
 * @return `true`/`false` — ответ сайта; `null` — сайт не ответил или ответ не разобрался.
 */
internal suspend fun fetchXLoginStatus(): Boolean? = withContext(Dispatchers.IO) {
    try {
        parseCheckLogin(postFormData("$urlStart/account/checklogin", emptyMap()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("X: проверка входа не удалась: ${e.logLabel()}")
        null
    }
}

/**
 * Закрывает сессию на сайте. Ошибку только пишет в журнал: cookie сессии
 * вызывающий снимает в любом случае, и на устройстве вход закончится.
 */
internal suspend fun requestXSignOut() = withContext(Dispatchers.IO) {
    try {
        postFormData("$urlStart/account/signout?no_redirect=1", emptyMap())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("X: выход на сайте не выполнен: ${e.logLabel()}")
    }
    Unit
}
