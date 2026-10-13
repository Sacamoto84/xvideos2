package com.client.xvideos

/**
 * Есть ли в задаче экран замка — показанный или только что запрошенный.
 *
 * Запросов на показ замка несколько: `onStart` и `onResume` у `MainActivity` и
 * событие «сеанс закрыли». Они приходят вплотную друг к другу, а система два
 * запуска одной Activity подряд не объединяет: `SINGLE_TOP` действует, только
 * когда первый экран уже создан, иначе в задаче оказываются две `LockActivity`
 * (проверено на эмуляторе с Android 12). Поэтому запускает замок тот, кто
 * первым получил здесь разрешение, остальные видят, что экран уже есть.
 *
 * Учёт ведётся по задачам: вторая задача приложения показывает свой замок сама
 * и не должна ждать экрана из первой.
 *
 * Только для главного потока.
 */
internal object LockScreenPresence {

    /**
     * Сколько запрошенному экрану даётся на создание. Дольше — запрос считается
     * несостоявшимся, и следующий вызывающий запустит замок заново: иначе
     * сорвавшийся запуск оставил бы приложение на чёрной заглушке навсегда.
     */
    const val START_GRACE_MS = 5_000L

    private val shownInTask = HashMap<Int, Int>()
    private val requestedAtElapsed = HashMap<Int, Long>()

    /**
     * @return `true` — экрана замка в задаче нет, запускать можно, и запрос
     *   теперь за вызывающим; `false` — экран показан или запускается.
     */
    fun claim(taskId: Int, elapsedNow: Long): Boolean {
        if ((shownInTask[taskId] ?: 0) > 0) return false
        val requestedAt = requestedAtElapsed[taskId]
        if (requestedAt != null && elapsedNow - requestedAt in 0 until START_GRACE_MS) return false
        requestedAtElapsed[taskId] = elapsedNow
        return true
    }

    /** Экран замка создан и остаётся на экране. */
    fun onShown(taskId: Int) {
        requestedAtElapsed.remove(taskId)
        shownInTask[taskId] = (shownInTask[taskId] ?: 0) + 1
    }

    /** Экран замка создался и сразу ушёл: сеанс к этому моменту уже открыт. */
    fun onRequestDropped(taskId: Int) {
        requestedAtElapsed.remove(taskId)
    }

    /** Экран замка закрывается или уничтожен. */
    fun onGone(taskId: Int) {
        val left = (shownInTask[taskId] ?: 0) - 1
        if (left > 0) shownInTask[taskId] = left else shownInTask.remove(taskId)
    }

    internal fun resetForTesting() {
        shownInTask.clear()
        requestedAtElapsed.clear()
    }
}
