package com.client.xvideos.common.applock

import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.semantics

@Composable
fun DisableAppLockAutofill() {
    val context = LocalContext.current
    val view = LocalView.current
    val autofillManager = remember(context) { context.getSystemService(AutofillManager::class.java) }

    DisposableEffect(view, autofillManager) {
        val views = view.rootView.collectViewTree()
        val previousValues = views.map { it to it.importantForAutofill }

        views.forEach {
            it.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        autofillManager?.cancel()

        onDispose {
            previousValues.forEach { (targetView, previousValue) ->
                targetView.importantForAutofill = previousValue
            }
            autofillManager?.cancel()
        }
    }
}

/**
 * Поле вообще не участвует в автозаполнении — для полей кода доступа.
 *
 * [DisableAppLockAutofill] убирает поля из структуры, которую получает служба
 * автозаполнения, но сам сеанс не отменяет: Compose сообщает системе о фокусе
 * в любом редактируемом поле, и служба при каждом показе замка узнавала, что в
 * приложении открыто поле ввода (история запросов в `dumpsys autofill`). С
 * этой пометкой о фокусе не сообщается, и сеанс не открывается.
 */
fun Modifier.excludedFromAutofill(): Modifier =
    semantics { contentDataType = ContentDataType.None }

private fun View.collectViewTree(): List<View> {
    val result = mutableListOf<View>()

    fun collect(view: View) {
        result += view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collect(view.getChildAt(index))
            }
        }
    }

    collect(this)
    return result
}
