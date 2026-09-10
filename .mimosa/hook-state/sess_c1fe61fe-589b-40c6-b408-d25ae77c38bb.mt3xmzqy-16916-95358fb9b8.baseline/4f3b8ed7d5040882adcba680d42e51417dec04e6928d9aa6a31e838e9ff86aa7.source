package com.client.xvideos.x.screens.tags

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.parcer.parserScreenTags
import com.client.xvideos.x.urlStart
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ScreenTagsViewModel @AssistedInject constructor(
    @Assisted val tag: String,
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(tag: String): ScreenTagsViewModel
    }

    // Compose-состояние: экран перерисуется, когда асинхронная загрузка завершится.
    var screen by mutableStateOf(ModelScreenTag("", "", emptyList()))
        private set

    init {
        // Раньше здесь был runBlocking { readHtmlFromURLDirect(...) } — сетевой запрос
        // блокировал поток создания ScreenModel (UI-поток) → ANR на медленной сети.
        // Теперь грузим в screenModelScope, а тяжёлый парсинг уводим на Dispatchers.Default.
        screenModelScope.launch {
            // Непойманное исключение здесь роняет приложение целиком: это launch
            // без обработчика. Заголовок и число страниц не настолько важны,
            // чтобы платить за них падением — сами страницы грузит пейджер.
            try {
                screen = loadPage(0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "!!! Заголовок тега %s не загрузился", tag)
            }
        }
    }

    /**
     * Разбор страницы выдачи с номером [index] (считается с нуля).
     *
     * Нулевая страница грузится дважды: здесь, в [init], ради заголовка и числа
     * страниц, и ещё раз первой страницей пейджера. Принято сознательно —
     * убирать кэшем в сетевом слое, если понадобится.
     */
    suspend fun loadPage(index: Int): ModelScreenTag {
        // Страницы адресуются /tags/<тег>/N; /tags/<тег> и /tags/<тег>/0 — одно и то же.
        val html = readHtmlFromURLDirect("$urlStart/tags/$tag/$index")
        return withContext(Dispatchers.Default) { parserScreenTags(html) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleTags {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenTagsViewModel.Factory::class)
    abstract fun bindScreenTagsScreenModel(
        hiltDetailsScreenModelFactory: ScreenTagsViewModel.Factory,
    ): ScreenModelFactory

}
