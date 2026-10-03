package com.client.xvideos.screenRoot.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.statusbar.shouldShowStatusBar

/**
 * Непрозрачность подложки под баром.
 *
 * Не ниже ~0.7: HyperOS замеряет яркость картинки под прозрачным статус-баром
 * (`StatusBarRegionSamplingInteractor`) и сама переключает значки в чёрный над
 * светлым контентом, игнорируя запрос приложения на белые. Тёмная подложка
 * держит замер тёмным — значки остаются белыми, контент слабо просвечивает.
 */
private const val STATUS_BAR_SCRIM_ALPHA = 0.7f

/**
 * Единственный хозяин системного статус-бара.
 *
 * Считает, виден ли бар (вырез сверху есть и ни один экран не просил его скрыть),
 * сообщает это окну через [onVisibleChange] и рисует под видимым баром
 * подложку цвета фона с непрозрачностью [STATUS_BAR_SCRIM_ALPHA].
 *
 * @param hideRequests Число заявок полноэкранных экранов на скрытие бара.
 */
@Composable
fun StatusBarHost(
    hideRequests: Int,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTopCutout = WindowInsets.displayCutout.getTop(LocalDensity.current) > 0
    val visible = shouldShowStatusBar(hasTopCutout, hideRequests)

    LaunchedEffect(visible) { onVisibleChange(visible) }

    if (visible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Theme.background.copy(alpha = STATUS_BAR_SCRIM_ALPHA))
        )
    }
}

@Preview
@Composable
private fun StatusBarHostPreview() {
    StatusBarHost(hideRequests = 0, onVisibleChange = {})
}
