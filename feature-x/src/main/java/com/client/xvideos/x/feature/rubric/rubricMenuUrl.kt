package com.client.xvideos.x.feature.rubric

import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.urlStart

/**
 * Адрес, по которому сайт отдаёт меню категорий ориентации [orientation].
 *
 * Список зависит только от ориентации в сессии сайта: `/gay` и `/trans` без
 * смены ориентации отдают меню гетеро. Смена ориентации из свежей сессии
 * (второй сегмент — текущая, `straight`) редиректит на ленту ориентации, и в
 * её HTML уже нужное меню.
 */
fun buildRubricMenuUrl(orientation: XCategory): String =
    "$urlStart/switch-sexual-orientation/${orientation.siteKey}/straight"
