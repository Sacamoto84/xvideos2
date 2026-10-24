package com.client.xvideos.screenSettings.model

import androidx.compose.runtime.Stable
import com.client.xvideos.l.ui.screens.LLoginFormState
import com.client.xvideos.x.screens.login.XLoginFormState

/**
 * Формы входа, которые настройки показывают вместо своей страницы по кнопке
 * «Войти»: L и X. Обе живут в `AppSettingsSM`.
 *
 * Одним параметром, а не двумя: `AppSettingsScreenContent` и так стоит у
 * порога detekt в двенадцать параметров, см. `SettingsDataHolders`.
 */
@Stable
internal class SettingsLoginForms(
    val l: LLoginFormState,
    val x: XLoginFormState,
)
