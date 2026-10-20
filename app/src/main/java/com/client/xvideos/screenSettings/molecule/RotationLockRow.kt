package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.screenSettings.components.SettingsListItem

/**
 * Строка «Возвращать блокировку поворота» в настройках «Отображение».
 *
 * Если при запуске приложения поворот был заблокирован, а к выходу блокировка снята,
 * её возвращает `RotationLockKeeper` — но только с правом «Изменение
 * системных настроек». Нажатие открывает системный экран этого права.
 *
 * @param canWriteSettings Право уже выдано.
 */
@Composable
internal fun RotationLockRow(
    canWriteSettings: Boolean,
    onOpenPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsListItem(
        icon = R.drawable.screen_rotation_24,
        text = "Возвращать блокировку поворота",
        subtitle = if (canWriteSettings) {
            "Включено: если при запуске поворот был заблокирован, блокировка вернётся при выходе"
        } else {
            "Нажмите и разрешите «Изменение системных настроек» — без него блокировку не вернуть"
        },
        onClick = onOpenPermission,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RotationLockRowPreview() {
    RotationLockRow(canWriteSettings = false, onOpenPermission = {})
}
