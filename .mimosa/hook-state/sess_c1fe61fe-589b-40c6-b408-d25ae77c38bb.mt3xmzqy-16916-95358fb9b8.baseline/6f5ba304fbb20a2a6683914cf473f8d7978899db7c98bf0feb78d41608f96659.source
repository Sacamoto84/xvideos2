package com.client.xvideos.common

/**
 * Поля `BuildConfig`, нужные базовому слою.
 *
 * `BuildConfig` генерируется на модуль: у библиотеки он свой, без
 * `applicationId` и версии приложения. Поэтому базовый слой не читает его
 * напрямую, а получает нужное от точки сборки — один раз при старте процесса,
 * см. `App.onCreate`.
 *
 * Значения по умолчанию рассчитаны на тесты: там `App` не создаётся.
 */
object AppBuildInfo {

    @Volatile
    var debug: Boolean = false
        private set

    @Volatile
    var versionName: String = "?"
        private set

    fun init(debug: Boolean, versionName: String) {
        this.debug = debug
        this.versionName = versionName
    }
}
