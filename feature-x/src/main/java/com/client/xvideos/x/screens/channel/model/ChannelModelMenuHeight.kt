package com.client.xvideos.x.screens.channel.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Высота пункта меню Material3 (минимальная высота `DropdownMenuItem`). */
private val MENU_ITEM_HEIGHT = 48.dp

/** Больше этой высоты список моделей прокручивается внутри меню. */
private val MENU_LIST_MAX_HEIGHT = 300.dp

/**
 * Фиксированная высота ленивого списка моделей в выпадающем меню фильтра.
 *
 * Меню меряет содержимое по intrinsic-ширине, а `Column` при этом спрашивает
 * у каждого ребёнка intrinsic-высоту. Ленивый список её не отдаёт и роняет
 * приложение (`IllegalStateException` про SubcomposeLayout). С фиксированной
 * высотой ответ даёт модификатор размера, и до списка запрос не доходит;
 * `heightIn(max = …)` так не умеет.
 */
fun channelModelMenuListHeight(count: Int): Dp =
    (MENU_ITEM_HEIGHT * count).coerceAtMost(MENU_LIST_MAX_HEIGHT)
