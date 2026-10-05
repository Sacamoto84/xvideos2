package com.client.xvideos.l.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.settings.element.SavedCredentials

/**
 * Набранные в форме входа L логин и пароль.
 *
 * Живёт в ScreenModel экрана, который показывает форму: переживает
 * пересоздание активности и при этом не покидает процесс приложения. Раньше
 * логин лежал в `rememberSaveable` — сохранённое состояние активности хранит
 * система, а проект держит логин в зашифрованном хранилище наравне с паролем.
 * Пароль лежал в `remember` и при смене темы терялся.
 *
 * @param saved сохранённый профиль на момент создания — см. [prefill].
 */
@Stable
class LLoginFormState(saved: SavedCredentials) {
    var login by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    init {
        prefill(saved)
    }

    fun updateLogin(value: String) {
        login = value
    }

    fun updatePassword(value: String) {
        password = value
    }

    /**
     * Готовит форму к показу: набранное раньше заменяется сохранённым профилем.
     *
     * Подставляется только незавершённый профиль — половина остаётся после
     * переноса старых настроек. Завершённый в форму не копируем: пароль незачем
     * держать в памяти второй раз.
     */
    fun prefill(saved: SavedCredentials) {
        login = if (saved.isComplete) "" else saved.login
        password = if (saved.isComplete) "" else saved.password
    }

    /** Стирает набранное, когда форма закрыта: пароль незачем держать в памяти. */
    fun clear() {
        login = ""
        password = ""
    }
}
