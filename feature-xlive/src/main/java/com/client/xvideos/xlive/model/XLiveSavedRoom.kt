package com.client.xvideos.xlive.model

import kotlinx.serialization.Serializable

/**
 * Комната в «Моих» или в «Истории» — то, что хранится на диске.
 *
 * Тема, зрители и снимок здесь не хранятся: они живые и к следующему
 * открытию устаревают. Снимок строится по нику, эфир проверяется заново.
 *
 * @property username ник модели, он же имя комнаты и ключ записи.
 * @property age возраст из анкеты; `null` — анкета не успела прийти.
 * @property savedAt когда сохранили или открыли последний раз, unix-время в миллисекундах.
 */
@Serializable
data class XLiveSavedRoom(
    val username: String,
    val age: Int? = null,
    val savedAt: Long = 0L,
)
