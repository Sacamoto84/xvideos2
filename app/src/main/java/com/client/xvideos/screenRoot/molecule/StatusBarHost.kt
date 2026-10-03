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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.statusbar.shouldShowStatusBar

/**
 * Непрозрачность подложки под баром.
 *
 * Значение у верхнего края; книзу подложка уходит в прозрачность
 * (см. [StatusBarScrimBrush]).
 *
 * HyperOS замеряет яркость картинки под прозрачным статус-баром
 * (`StatusBarRegionSamplingInteractor`) и сама переключает значки в чёрный над
 * светлым контентом, игнорируя запрос приложения на белые. Тёмная подложка
 * держит замер тёмным — значки остаются белыми. Значение подобрано на устройстве
 * для градиента: при меньшем значки начинают переключаться.
 */
private const val STATUS_BAR_SCRIM_ALPHA = 0.9f

/** Число отрезков, которыми набирается плавная кривая градиента. */
private const val STATUS_BAR_SCRIM_STEPS = 16

/**
 * Подложка градиентом: сверху [STATUS_BAR_SCRIM_ALPHA], к нижнему краю бара
 * полностью прозрачная.
 *
 * Спад не линейный, а по кривой smootherstep. Линейный градиент упирается в ноль
 * под углом, и глаз подчёркивает это место светлой полосой (полосы Маха). У
 * smootherstep на обоих концах нулевые наклон и кривизна — граница не читается.
 * Средняя плотность та же, что у линейного (0.5), поэтому замер яркости HyperOS
 * видит прежнюю картинку.
 *
 * Цвет на всех шагах — фон с убывающей альфой, а не переход в
 * `Color.Transparent`: иначе середина уходит в серо-чёрный.
 */
private val StatusBarScrimBrush = Brush.verticalGradient(
    colors = List(STATUS_BAR_SCRIM_STEPS + 1) { step ->
        val t = step / STATUS_BAR_SCRIM_STEPS.toFloat()
        val eased = t * t * t * (t * (t * 6f - 15f) + 10f)
        Theme.background.copy(alpha = STATUS_BAR_SCRIM_ALPHA * (1f - eased))
    }
)

/**
 * Единственный хозяин системного статус-бара.
 *
 * Считает, виден ли бар (вырез сверху есть и ни один экран не просил его скрыть),
 * сообщает это окну через [onVisibleChange] и рисует под видимым баром
 * подложку цвета фона: градиент от [STATUS_BAR_SCRIM_ALPHA] сверху до прозрачного
 * у нижнего края бара.
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
                .background(StatusBarScrimBrush)
        )
    }
}

@Preview
@Composable
private fun StatusBarHostPreview() {
    StatusBarHost(hideRequests = 0, onVisibleChange = {})
}
