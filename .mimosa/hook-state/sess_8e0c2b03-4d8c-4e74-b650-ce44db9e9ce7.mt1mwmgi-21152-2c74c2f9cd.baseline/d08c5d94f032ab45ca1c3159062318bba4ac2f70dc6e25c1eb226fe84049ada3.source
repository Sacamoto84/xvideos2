package com.client.xvideos.common

import android.annotation.SuppressLint
import android.content.Context

/**
 * Application-контекст для кода, до которого не дотягивается ни DI, ни Compose.
 *
 * Раньше такие места брали его из `App.instance` — то есть базовый слой и
 * разделы знали класс приложения и не могли уехать в свои модули. Теперь
 * контекст публикует сюда сама точка сборки, в `App.onCreate`.
 *
 * Для нового кода это крайняя мера: если объект создаётся через Hilt, контекст
 * надо просить инъекцией (`@ApplicationContext`), а в composable брать
 * `LocalContext`.
 */
// lint видит статическое поле типа Context и объявляет утечку. Здесь её нет:
// init() кладёт applicationContext, который живёт ровно столько же, сколько
// процесс. Утечкой было бы держать Activity — за этим и надо следить в init().
@SuppressLint("StaticFieldLeak")
object AppContextHolder {

    @Volatile
    private var context: Context? = null

    val applicationContext: Context
        get() = requireNotNull(context) {
            "AppContextHolder не инициализирован — вызовите init() в Application.onCreate()"
        }

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
