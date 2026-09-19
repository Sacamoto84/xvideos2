package com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.selectionKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
class ScreenLServerLikesSM @Inject constructor(
    private val repository: LusciousServerFavoritesRepository
) : ScreenModel {

    val host = LazyRowPictureDetailsHost("l_server_likes")

    private val _pictures = MutableStateFlow<List<PicsDetails>>(emptyList())
    val pictures = _pictures.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var hasMore: Boolean = true
        private set

    private var currentPage: Int = 1

    init {
        host.onItemRemoved = { removedPic ->
            val targetKey = removedPic.selectionKey()
            _pictures.value = _pictures.value.filterNot {
                (!it.id.isNullOrBlank() && it.id == removedPic.id) ||
                    it.selectionKey() == targetKey
            }
        }
        loadInitial()
    }

    fun unlikePicture(pic: PicsDetails) {
        host.removePicture(pic)
    }

    fun loadInitial() {
        if (_isLoading.value) return
        screenModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 1
            val result = repository.getServerLikedPictures(currentPage)
            result.onSuccess { list ->
                _pictures.value = list
                host.replaceFilteredPictures(list)
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
            }.onFailure { error ->
                Timber.e(error, "Failed to load server liked pictures")
                _errorMessage.value = error.message ?: "Ошибка загрузки лайков с сервера"
            }
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || !hasMore || _errorMessage.value != null) return
        screenModelScope.launch {
            _isLoading.value = true
            val nextPage = currentPage + 1
            val result = repository.getServerLikedPictures(nextPage)
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    currentPage = nextPage
                    val combined = _pictures.value + list
                    _pictures.value = combined
                    host.replaceFilteredPictures(combined)
                } else {
                    hasMore = false
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to load next page ($nextPage) of server likes")
            }
            _isLoading.value = false
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        screenModelScope.launch {
            _isRefreshing.value = true
            currentPage = 1
            val result = repository.getServerLikedPictures(currentPage)
            result.onSuccess { list ->
                _pictures.value = list
                host.replaceFilteredPictures(list)
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
            }.onFailure { error ->
                Timber.e(error, "Failed to refresh server likes")
                _errorMessage.value = error.message ?: "Ошибка обновления лайков с сервера"
            }
            _isRefreshing.value = false
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLServerLikes {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLServerLikesSM::class)
    abstract fun bindScreenLServerLikesSM(sm: ScreenLServerLikesSM): ScreenModel
}
