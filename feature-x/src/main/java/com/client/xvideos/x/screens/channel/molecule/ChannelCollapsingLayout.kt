package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.screens.channel.model.channelCollapsingGeometry

/**
 * Кастомный макет для схлопывающейся шапки профиля канала, липкой панели сортировок/фильтров
 * и горизонтального пейджера страниц с видеороликами.
 *
 * Шапка начинается от верха экрана и заходит под вырез камеры [topInsetPx]; липкая
 * панель при схлопывании упирается в низ выреза. Раскладку считает [channelCollapsingGeometry].
 */
@Composable
fun ChannelCollapsingLayout(
    headerOffsetPx: Float,
    topInsetPx: Int,
    header: @Composable () -> Unit,
    stickyBar: @Composable () -> Unit,
    pager: @Composable () -> Unit,
    statusCover: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            header()
            stickyBar()
            pager()
            statusCover()
        },
        modifier = modifier
    ) { measurables, constraints ->
        val headerMeasurable = measurables.getOrNull(0)
        val stickyBarMeasurable = measurables.getOrNull(1)
        val pagerMeasurable = measurables.getOrNull(2)
        val statusCoverMeasurable = measurables.getOrNull(3)

        val headerPlaceable = headerMeasurable?.measure(constraints.copy(minHeight = 0))
        val headerHeight = headerPlaceable?.height ?: 0

        val stickyBarPlaceable = stickyBarMeasurable?.measure(constraints.copy(minHeight = 0))
        val stickyBarHeight = stickyBarPlaceable?.height ?: 0

        val availablePagerHeight = (constraints.maxHeight - stickyBarHeight - topInsetPx).coerceAtLeast(0)
        val pagerPlaceable = pagerMeasurable?.measure(
            constraints.copy(minHeight = availablePagerHeight, maxHeight = availablePagerHeight)
        )

        val statusCoverPlaceable = statusCoverMeasurable?.measure(
            constraints.copy(minHeight = topInsetPx, maxHeight = topInsetPx)
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            val geometry = channelCollapsingGeometry(
                headerOffsetPx = headerOffsetPx,
                headerHeight = headerHeight,
                stickyBarHeight = stickyBarHeight,
                topInsetPx = topInsetPx,
            )

            // Порядок отрисовки слоёв:
            // 1. Пейджер снизу
            pagerPlaceable?.placeWithLayer(0, geometry.pagerY)
            // 2. Шапка профиля
            headerPlaceable?.placeWithLayer(0, geometry.headerY)
            // 3. Липкая панель сортировок (перекрывает шапку при схлопывании)
            stickyBarPlaceable?.placeWithLayer(0, geometry.stickyY)
            // 4. Плашка выреза в самом верху. Прозрачную не ставим: она перехватывала
            // бы касания шапки под вырезом.
            if (geometry.coverAlpha > 0f) {
                statusCoverPlaceable?.placeWithLayer(0, 0) { alpha = geometry.coverAlpha }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun ChannelCollapsingLayoutPreview() {
    ChannelCollapsingLayout(
        headerOffsetPx = 0f,
        topInsetPx = 24,
        header = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.DarkGray)
            ) {
                Text("Header Mock", color = Color.White)
            }
        },
        stickyBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.Black)
            ) {
                Text("Sticky Bar Mock", color = Color.White)
            }
        },
        pager = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF141418))
            ) {
                Text("Pager Mock", color = Color.White)
            }
        },
        statusCover = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color.Black)
            )
        }
    )
}
