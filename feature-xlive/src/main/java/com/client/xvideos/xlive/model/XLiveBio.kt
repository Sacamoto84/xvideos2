package com.client.xvideos.xlive.model

import kotlinx.serialization.Serializable

/**
 * Анкета модели из `api/biocontext/<ник>/`.
 *
 * Пустая строка или `null` — поле на сайте не заполнено.
 *
 * @property aboutMe «О себе» простым текстом: HTML сайта уже разобран.
 * @property interestedIn кто интересует, как отдаёт сайт: `Women`, `Men`, `Couples`, `Trans`.
 */
@Serializable
data class XLiveBio(
    val realName: String = "",
    val age: Int? = null,
    val location: String = "",
    val languages: String = "",
    val bodyType: String = "",
    val interestedIn: List<String> = emptyList(),
    val followers: Int? = null,
    val aboutMe: String = "",
)
