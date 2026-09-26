package com.client.xvideos.r.ui.ui.lazyrow123.model

/**
 * Варианты типов отображаемых лент в сетке [com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host].
 */
enum class TypePager {
    /** Лента видеороликов конкретной ниши. */
    NICHES,
    /** Главная лента Explorer (топ за неделю/месяц/всё время либо поисковая выдача). */
    TOP,
    /** Лента понравившихся роликов (лайков). */
    R_SAVED_LIKES,
    /** Лента роликов из конкретной пользовательской коллекции. */
    SAVED_COLLECTION,
    /** Лента публикаций в профиле автора. */
    PROFILE,
    /** Пустая лента. */
    EMPTY,
    /** Лента публикаций по подпискам на авторов. */
    SUBSCRIPTIONS
}
