package com.client.xvideos.screenSettings.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.molecule.XProfileRow

/**
 * Страница настроек X: профиль сайта — вход и выход, как на странице L.
 *
 * Прежние переключатели этой страницы («2 столбика», «Shemale») ни на что не
 * влияли и убраны вместе с ней; категорию X выбирает кнопка у нижних вкладок,
 * число колонок считается по ширине окна.
 *
 * @param restoreFailure ответ сайта на сохранённый профиль при автоматическом входе.
 */
@Composable
internal fun XSettingsSection(
    xLogin: String,
    hasXAccount: Boolean,
    restoreFailure: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(modifier = modifier) {
        XProfileRow(
            login = xLogin,
            hasAccount = hasXAccount,
            restoreFailure = restoreFailure,
            onLogin = onLogin,
            onLogout = onLogout
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XSettingsSectionPreview() = SettingsPreview {
    XSettingsSection(
        xLogin = "user@example.com",
        hasXAccount = true,
        restoreFailure = null,
        onLogin = {},
        onLogout = {},
        modifier = Modifier
    )
}
