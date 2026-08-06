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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val url = "$urlStart/tags/$tag"
            val html = readHtmlFromURLDirect(url)
            screen = withContext(Dispatchers.Default) { parserScreenTags(html) }
        }
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
