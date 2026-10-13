package com.client.xvideos.common.ui

import android.view.View
import android.view.ViewParent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Пока вызвана, окно открыто системной службе автозаполнения.
 *
 * Окно приложения закрыто от неё целиком — см. `MainActivity.configureWindow`:
 * тексту поиска у менеджера паролей делать нечего. Запрет стоит на корне окна
 * и действует на всех потомков, снять его с одного поля нельзя. Поэтому экран,
 * которому менеджер паролей нужен, снимает запрет со всей цепочки предков на
 * время своего показа и возвращает прежние значения, когда уходит.
 *
 * Сеанс автозаполнения здесь не трогается. Когда поля с типом содержимого
 * уходят с экрана, Compose сам завершает сеанс (`commit`) — и после
 * сохранения формы, и если из неё вышли без сохранения: менеджер паролей
 * вправе предложить запомнить введённое. Раньше при уходе здесь вызывался
 * `cancel()` «для незавершённого сеанса», но он выполнялся уже после этого
 * завершения и ничего не отменял — по журналу автозаполнения на телефоне и на
 * эмуляторе `commit` всякий раз шёл первым.
 */
@Composable
fun AllowWindowAutofill() {
    val view = LocalView.current
    DisposableEffect(view) {
        val blocking = buildList {
            var ancestor: ViewParent? = view.parent
            while (ancestor is View) {
                if (ancestor.importantForAutofill.excludesDescendants()) {
                    add(ancestor to ancestor.importantForAutofill)
                }
                ancestor = ancestor.parent
            }
        }
        blocking.forEach { (ancestor, _) ->
            ancestor.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_AUTO
        }
        onDispose {
            blocking.forEach { (ancestor, previous) -> ancestor.importantForAutofill = previous }
        }
    }
}

private fun Int.excludesDescendants(): Boolean =
    this == View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS ||
        this == View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS
