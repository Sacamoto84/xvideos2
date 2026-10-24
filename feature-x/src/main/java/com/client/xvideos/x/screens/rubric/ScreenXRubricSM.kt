package com.client.xvideos.x.screens.rubric

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.rubric.model.XRubricFeed
import com.client.xvideos.x.screens.rubric.model.XRubricFilterGroup
import com.client.xvideos.x.screens.rubric.model.XRubricFilterOption
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ScreenModel экрана категории X: выдача [feed], открытая группа фильтров и
 * действия меню роликов.
 *
 * @property saved Избранное и загрузки X — для меню роликов выдачи.
 */
@Stable
class ScreenXRubricSM @AssistedInject constructor(
    @Assisted id: Int,
    @Assisted slug: String,
    val saved: SavedX,
) : ScreenModel {

    /** Фабрика assisted injection: категория задаётся id и slug из меню сайта. */
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(id: Int, slug: String): ScreenXRubricSM
    }

    val feed = XRubricFeed(id, slug, ::readHtmlFromURLDirect)

    /** Группа фильтров, чей список вариантов раскрыт; `null` — все закрыты. */
    var openGroup: XRubricFilterGroup? by mutableStateOf(null)
        private set

    init {
        loadHeader()
    }

    /** Раскрывает список вариантов [group] или закрывает его, если он уже раскрыт. */
    fun toggleGroup(group: XRubricFilterGroup) {
        openGroup = if (openGroup == group) null else group
    }

    /** Выбирает [option] в его группе; другая выдача грузится с первой страницы. */
    fun selectFilter(option: XRubricFilterOption) {
        openGroup = null
        if (feed.applyFilters(feed.filters.with(option))) loadHeader()
    }

    /** Ролик в избранном. */
    fun isFavorite(id: Long): Boolean = saved.favorites.contains(id)

    /** Добавить ролик в избранное. */
    fun addFavorite(item: ItemsX) = saved.favorites.add(item)

    /** Убрать ролик из избранного. */
    fun removeFavorite(item: ItemsX) = saved.favorites.remove(item)

    /** Скачать ролик в «Сохранённое». */
    fun download(item: ItemsX) = saved.downloads.download(item)

    /** Сохранить ролик в галерею устройства. */
    fun saveToGallery(item: ItemsX) = saved.downloads.saveToGallery(item)

    /** Заголовок и число страниц — со страницы 0; сами страницы грузит пейджер. */
    private fun loadHeader() {
        screenModelScope.launch {
            // Непойманное исключение в launch без обработчика роняет приложение:
            // заголовок того не стоит.
            try {
                feed.loadPage(0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w("!!! Заголовок категории не загрузился: %s", e.javaClass.simpleName)
            }
        }
    }
}

/** Hilt-модуль привязки фабрики [ScreenXRubricSM.Factory]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRubric {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenXRubricSM.Factory::class)
    abstract fun bindScreenXRubricScreenModel(factory: ScreenXRubricSM.Factory): ScreenModelFactory
}
