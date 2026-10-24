package com.client.xvideos.x.screens.rubrics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.x.feature.rubric.XRubricCatalog
import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.model.XRubric
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Что показывает вкладка «Категории». */
sealed interface XRubricsState {
    /** Список грузится: только индикатор по центру. */
    data object Loading : XRubricsState

    /** Не загрузился или не разобрался: текст и «Повторить». */
    data object Failed : XRubricsState

    @Immutable
    data class Ready(val orientation: XCategory, val rubrics: ImmutableList<XRubric>) : XRubricsState
}

/**
 * ScreenModel вкладки «Категории»: список ориентации из [XRubricCatalog] и
 * позиции сетки. Позиция у каждой ориентации своя и живёт здесь, а не в
 * композиции: корневой навигатор пересоздаёт главный экран после возврата из
 * категории.
 */
@Stable
class XRubricsSM internal constructor(
    private val catalog: XRubricCatalog,
    private val scopeOverride: CoroutineScope?,
) : ScreenModel {

    @Inject
    constructor(catalog: XRubricCatalog) : this(catalog, null)

    var state: XRubricsState by mutableStateOf(XRubricsState.Loading)
        private set

    private val scope: CoroutineScope get() = scopeOverride ?: screenModelScope
    private var shown: XCategory? = null
    private var loadJob: Job? = null
    private val gridPositions = mutableMapOf<XCategory, LazyGridPosition>()

    /** Позиция сетки ориентации [orientation]. */
    fun gridPosition(orientation: XCategory): LazyGridPosition =
        gridPositions.getOrPut(orientation) { LazyGridPosition() }

    /**
     * Что показать для ориентации [orientation] прямо сейчас.
     *
     * Композиция узнаёт о новой ориентации раньше, чем [show] из её эффекта:
     * без этого кадр рисовал список прежней ориентации с позицией новой, и
     * сетка запоминала сдвинутую позицию. Уже загруженный список отдаётся сразу.
     */
    fun stateFor(orientation: XCategory): XRubricsState {
        val current = state
        if (current is XRubricsState.Ready && current.orientation == orientation) return current
        catalog.cached(orientation)?.let { return XRubricsState.Ready(orientation, it) }
        return if (orientation == shown) current else XRubricsState.Loading
    }

    /** Показывает список [orientation]; та же ориентация без ошибки ничего не перезапускает. */
    fun show(orientation: XCategory) {
        if (orientation == shown && state != XRubricsState.Failed) return
        load(orientation)
    }

    /** Грузит список показанной ориентации заново. */
    fun retry() {
        shown?.let(::load)
    }

    private fun load(orientation: XCategory) {
        shown = orientation
        // Прежняя загрузка другой ориентации не должна ответить поверх новой.
        loadJob?.cancel()
        val cached = catalog.cached(orientation)
        if (cached != null) {
            state = XRubricsState.Ready(orientation, cached)
            return
        }
        state = XRubricsState.Loading
        loadJob = scope.launch {
            state = try {
                XRubricsState.Ready(orientation, catalog.load(orientation))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w("!!! Категории X не загрузились: %s", e.javaClass.simpleName)
                XRubricsState.Failed
            }
        }
    }
}

/** Hilt-модуль привязки [XRubricsSM]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRubrics {

    @Binds
    @IntoMap
    @ScreenModelKey(XRubricsSM::class)
    abstract fun bindXRubricsSM(hiltModel: XRubricsSM): ScreenModel
}
