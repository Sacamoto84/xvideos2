package com.client.xvideos.l.repository

import com.client.xvideos.common.settings.element.SavedCredentials
import com.client.xvideos.l.model.UserProfile

/**
 * Сохранённый профиль, как его видит [Repository]: значения и номер записи
 * одним снимком.
 *
 * Меняется только через [save] — как в приложении, где у профиля одна операция
 * записи, и она всегда поднимает номер. Раньше тесты подменяли лямбду с
 * профилем, номер при этом не рос: проверялась ветка, которой в приложении нет.
 */
class SavedProfile(initial: UserProfile = UserProfile()) {
    @Volatile
    var snapshot = SavedCredentials(initial.email, initial.password, revision = 0)
        private set

    fun save(value: UserProfile) {
        snapshot = SavedCredentials(value.email, value.password, snapshot.revision + 1)
    }
}
