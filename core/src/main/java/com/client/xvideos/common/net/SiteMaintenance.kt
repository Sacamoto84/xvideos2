package com.client.xvideos.common.net

import android.os.SystemClock
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Сайт на техобслуживании: так отвечает Cloudflare перед сервером, пока тот
 * недоступен. Раньше любой экран писал только «Не удалось загрузить страницу»,
 * и было не понять, что лежит сервер, а не приложение или сеть.
 */
private const val HTTP_SERVICE_UNAVAILABLE = 503

/** Код ответа сайта на техобслуживании: 503. */
fun isMaintenanceStatus(code: Int): Boolean = code == HTTP_SERVICE_UNAVAILABLE

/** Ответ 503 → [onMaintenance]; сам ответ уходит дальше без изменений. */
class SiteMaintenanceInterceptor(private val onMaintenance: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (isMaintenanceStatus(response.code)) onMaintenance()
        return response
    }
}

/**
 * Не чаще раза в [intervalMs] на сайт: при техобслуживании падает каждая
 * загрузка (страницы, повторы), а снекбары показываются строго по очереди —
 * без ограничения они выстроились бы в длинный хвост.
 */
class SiteMaintenanceThrottle(private val intervalMs: Long) {
    private val lastReport = HashMap<String, Long>()

    @Synchronized
    fun shouldReport(site: String, nowMs: Long): Boolean {
        val last = lastReport[site]
        if (last != null && nowMs - last < intervalMs) return false
        lastReport[site] = nowMs
        return true
    }
}

/**
 * Сообщает о техобслуживании сайта раздела ([site] — `X`, `R` или `L`)
 * событием [Event.SiteMaintenance]; плашку показывает корневой экран.
 */
object SiteMaintenanceReporter {
    /** Сайт сам просит повторить через 2 минуты (`Retry-After: 120`). */
    private const val REPORT_INTERVAL_MS = 120_000L
    private val throttle = SiteMaintenanceThrottle(REPORT_INTERVAL_MS)

    fun report(site: String) {
        if (throttle.shouldReport(site, SystemClock.elapsedRealtime())) {
            EventBus.postEvent(Event.SiteMaintenance(site))
        }
    }
}
