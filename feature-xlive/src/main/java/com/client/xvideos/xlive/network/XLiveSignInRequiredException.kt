package com.client.xvideos.xlive.network

import java.io.IOException

/** Сайт ответил «нужен вход»: сессии нет, она истекла или закрыта на сайте. */
class XLiveSignInRequiredException(reason: String = "") : IOException("Нужен вход на сайт: $reason")
