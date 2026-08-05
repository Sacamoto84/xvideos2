package com.client.xvideos.common.applock

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun DisableAppLockAutofill() {
    val context = LocalContext.current
    val view = LocalView.current
    val autofillManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        remember(context) { context.getSystemService(AutofillManager::class.java) }
    } else {
        null
    }

    DisposableEffect(view, autofillManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        } else {
            onDispose {}
        }
    }
}

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
