package com.client.xvideos.x.model

/**
 * Категория сайта X. Сайт хранит выбор в сессии, но те же страницы отдаёт и по
 * своим адресам — приложение берёт адреса, сессия ему не нужна.
 *
 * @property title Подпись в меню переключателя.
 * @property feedPath Главная категории: `""` — корень сайта, иначе `/gay`, `/trans`.
 * @property siteKey Имя категории в адресах сайта: фильтр выдачи `typef=`, префикс
 *   тегов `t:`, рейтинг профиля `/ranks/…`.
 */
enum class XCategory(val title: String, val feedPath: String, val siteKey: String) {
    STRAIGHT("Гетеро", "", "straight"),
    GAY("Геи", "/gay", "gay"),
    TRANS("Трансы", "/trans", "shemale"),
}

/** Категория по сохранённому имени; пустое или чужое значение — гетеро, как на сайте по умолчанию. */
fun xCategoryOf(name: String?): XCategory =
    XCategory.entries.firstOrNull { it.name == name } ?: XCategory.STRAIGHT
