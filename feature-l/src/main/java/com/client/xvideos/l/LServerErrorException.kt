package com.client.xvideos.l

import java.io.IOException

/**
 * Сервер L ответил кодом 5xx, и это не проверка Cloudflare.
 *
 * Отдельный тип нужен, чтобы отказ сервера не путался с антибот-защитой:
 * раньше страница 500 считалась HTML-челленджем и запрос гонялся по трём
 * циклам кулдауна, а пользователь вместо «сервер упал» видел баннер защиты.
 *
 * @property statusCode HTTP-код ответа.
 */
class LServerErrorException(val statusCode: Int) : IOException("Сервер L недоступен (HTTP $statusCode)")
