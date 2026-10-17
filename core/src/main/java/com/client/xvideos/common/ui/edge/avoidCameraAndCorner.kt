package com.client.xvideos.common.ui.edge

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.getDisplayCutoutBounds
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Сдвигает элемент у правого верхнего края влево, если он попадает под вырез
 * камеры или под скругление угла экрана. Размер и место в разметке не меняются —
 * сдвигаются только отрисовка и область нажатия; на прямоугольном экране без
 * камеры у края сдвига нет.
 *
 * Где камера и какой радиус угла, знает только система: Compose отдаёт лишь
 * толщину полосы выреза. Вырезы берутся из рулеров инсетов (их чтение заодно
 * перезапускает размещение при сложении и повороте), скругление — из инсетов
 * окна (Android 12+).
 */
fun Modifier.avoidCameraAndCorner(gap: Dp = 4.dp): Modifier =
    this then AvoidCameraAndCornerElement(gap, avoidCorner = true)

/**
 * Как [avoidCameraAndCorner], но только от выреза камеры: для кнопок, которым
 * скругление не мешает, — на телефоне их место не меняется.
 */
fun Modifier.avoidCamera(gap: Dp = 4.dp): Modifier =
    this then AvoidCameraAndCornerElement(gap, avoidCorner = false)

private data class AvoidCameraAndCornerElement(
    val gap: Dp,
    val avoidCorner: Boolean,
) : ModifierNodeElement<AvoidCameraAndCornerNode>() {
    override fun create() = AvoidCameraAndCornerNode(gap, avoidCorner)

    override fun update(node: AvoidCameraAndCornerNode) {
        node.gap = gap
        node.avoidCorner = avoidCorner
    }
}

private class AvoidCameraAndCornerNode(var gap: Dp, var avoidCorner: Boolean) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val gapPx = gap.roundToPx()
        return layout(placeable.width, placeable.height) {
            val cutouts = getDisplayCutoutBounds().map {
                Rect(
                    it.left.current(Float.NaN),
                    it.top.current(Float.NaN),
                    it.right.current(Float.NaN),
                    it.bottom.current(Float.NaN),
                )
            }
            val shift = maxOf(
                cutoutShiftLeft(cutouts, placeable.width, placeable.height, gapPx),
                if (avoidCorner) topEndCornerShift(placeable.width, gapPx) else 0,
            )
            placeable.place(-shift, 0)
        }
    }

    private fun Placeable.PlacementScope.topEndCornerShift(width: Int, gap: Int): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0
        val corner = currentValueOf(LocalView).rootWindowInsets
            ?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT) ?: return 0
        val origin = coordinates?.positionInWindow() ?: return 0
        return roundedCornerShiftLeft(
            width = width,
            centerX = corner.center.x - origin.x,
            centerY = corner.center.y - origin.y,
            radius = corner.radius.toFloat(),
            gap = gap,
        )
    }
}
