package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

/**
 * Категория из меню «Категории» сайта X: лента `/c/<slug>-<id>`.
 *
 * В коде — «рубрика»: [XCategory] уже означает
 * ориентацию (гетеро / геи / трансы).
 *
 * @property id Номер категории на сайте; он и задаёт ленту, ориентация на неё не влияет.
 * @property slug Имя в адресе, как его даёт сайт.
 * @property title Подпись из меню сайта.
 */
@Immutable
data class XRubric(val id: Int, val slug: String, val title: String)
