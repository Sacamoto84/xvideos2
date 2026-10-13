package com.client.xvideos.common.ui

import android.view.View
import android.view.ViewParent
import android.view.autofill.AutofillManager
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
 * Незавершённый сеанс автозаполнения при уходе закрывается: сохранять поля
 * экрана, которого уже нет, незачем. Сеанс, завершённый через
 * `AutofillManager.commit()`, к этому моменту уже закрыт, и на предложение
 * сохранить пароль отмена не влияет.
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
            view.context.getSystemService(AutofillManager::class.java)?.cancel()
        }
    }
}

private fun Int.excludesDescendants(): Boolean =
    this == View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS ||
        this == View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS
