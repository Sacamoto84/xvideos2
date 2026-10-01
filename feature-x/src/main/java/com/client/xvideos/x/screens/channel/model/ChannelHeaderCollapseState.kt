package com.client.xvideos.x.screens.channel.model

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Состояние схлопывающейся шапки экрана канала.
 *
 * Свайп по шапке, липкой панели и плашке выреза ([scrollModifier]) сначала двигает
 * шапку, а когда она схлопнута — прокручивает сетку текущей страницы. Скролл самой
 * сетки ([nestedScrollConnection]) только схлопывает шапку: возврат к верху сетки
 * и её инерция шапку не раскрывают.
 *
 * @param offsetState Смещение шапки в пикселях: 0 — раскрыта, минус высота — схлопнута.
 * @param scope Область для анимации доводки шапки.
 * @param currentGrid Сетка текущей страницы пейджера.
 */
@Stable
class ChannelHeaderCollapseState(
    offsetState: MutableFloatState,
    private val scope: CoroutineScope,
    private val currentGrid: () -> LazyGridState,
) {
    private var headerOffsetPxState by offsetState
    private var headerHeightPx by mutableFloatStateOf(0f)
    private var flingAnimationJob: Job? = null

    /** Смещение шапки в пикселях: 0 — раскрыта, минус высота — схлопнута. */
    val headerOffsetPx: Float get() = headerOffsetPxState

    /** Шапка ушла вверх: кнопка «Назад» показывается в липкой панели. */
    val isBackInStickyBar: Boolean get() = headerOffsetPxState < -80f

    /** Скролл сетки видео: схлопывает шапку, но не раскрывает её. */
    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) {
                flingAnimationJob?.cancel()
                flingAnimationJob = null
            }
            val delta = available.y
            if (delta < 0f && headerHeightPx > 0f) {
                val newOffset = (headerOffsetPxState + delta).coerceIn(-headerHeightPx, 0f)
                val consumed = newOffset - headerOffsetPxState
                headerOffsetPxState = newOffset
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Изоляция скролла списка: достижение верха сетки видео не стягивает шапку вниз
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (available.y < 0f && headerOffsetPxState > -headerHeightPx && headerHeightPx > 0f) {
                animateHeaderTo(target = -headerHeightPx, initialVelocity = available.y)
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            // Изоляция флинга списка: инерция сетки не раскрывает шапку
            return Velocity.Zero
        }
    }

    private val headerScrollableState = ScrollableState { delta ->
        flingAnimationJob?.cancel()
        flingAnimationJob = null
        var consumed = 0f
        if (delta < 0f) {
            // Палец вверх: схлопываем шапку
            if (headerHeightPx > 0f && headerOffsetPxState > -headerHeightPx) {
                val newOffset = (headerOffsetPxState + delta).coerceIn(-headerHeightPx, 0f)
                val headerConsumed = newOffset - headerOffsetPxState
                headerOffsetPxState = newOffset
                consumed += headerConsumed
            }
            // Если шапка уже схлопнута, передаем скролл в сетку видео
            val remaining = delta - consumed
            if (remaining < 0f) {
                val gridConsumed = currentGrid().dispatchRawDelta(remaining)
                consumed += gridConsumed
            }
        } else if (delta > 0f) {
            // Палец вниз: разворачиваем шапку
            if (headerHeightPx > 0f && headerOffsetPxState < 0f) {
                val newOffset = (headerOffsetPxState + delta).coerceIn(-headerHeightPx, 0f)
                val headerConsumed = newOffset - headerOffsetPxState
                headerOffsetPxState = newOffset
                consumed += headerConsumed
            }
        }
        consumed
    }

    private val headerFlingBehavior = object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            if (headerHeightPx <= 0f) return 0f
            val target = if (initialVelocity > 300f || (initialVelocity >= -300f && headerOffsetPxState > -headerHeightPx * 0.5f)) {
                0f
            } else {
                -headerHeightPx
            }
            animateHeaderTo(target = target, initialVelocity = initialVelocity)
            return initialVelocity
        }
    }

    /** Свайп по шапке, липкой панели и плашке выреза. */
    val scrollModifier: Modifier = Modifier.scrollable(
        state = headerScrollableState,
        orientation = Orientation.Vertical,
        flingBehavior = headerFlingBehavior,
    )

    /**
     * Новая высота шапки: схлопнутая шапка остаётся схлопнутой, смещение не выходит
     * за высоту.
     */
    fun onHeaderHeightChanged(height: Float) {
        if (headerHeightPx != height) {
            val wasFullyCollapsed = headerHeightPx > 0f && headerOffsetPxState <= -headerHeightPx + 1f
            headerHeightPx = height
            if (wasFullyCollapsed) {
                headerOffsetPxState = -height
            } else if (headerOffsetPxState < -height) {
                headerOffsetPxState = -height
            }
        }
    }

    private fun animateHeaderTo(target: Float, initialVelocity: Float) {
        flingAnimationJob?.cancel()
        val anim = Animatable(headerOffsetPxState)
        flingAnimationJob = scope.launch {
            anim.animateTo(
                targetValue = target,
                initialVelocity = initialVelocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                headerOffsetPxState = value
            }
        }
    }
}
