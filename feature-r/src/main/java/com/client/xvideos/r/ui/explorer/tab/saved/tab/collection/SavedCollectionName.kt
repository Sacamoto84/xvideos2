package com.client.xvideos.r.ui.explorer.tab.saved.tab.collection

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import androidx.compose.material3.Text
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import timber.log.Timber

private val TOP_BAR_START_PADDING = 4.dp
private val TOP_BAR_END_PADDING = 8.dp
private val TITLE_START_PADDING = 4.dp
private val TITLE_FONT_SIZE = 18.sp
private const val CD_BACK = "Назад"
private val ZERO_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)
private val TOP_BAR_HORIZONTAL_PADDING_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(start = TOP_BAR_START_PADDING, end = TOP_BAR_END_PADDING)
private val TITLE_MODIFIER = Modifier.padding(start = TITLE_START_PADDING)
private val CONTENT_BOX_BASE_MODIFIER = Modifier.fillMaxSize()
private val LAZY_ROW_MODIFIER = Modifier.fillMaxSize()

class ScreenCollectionName(
    val collectionName: String,
    private val popOnBack: Boolean = false
) : Screen {

    override val key: ScreenKey = "RCollection:$collectionName:$popOnBack"

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenRedCollectionNameSM, ScreenRedCollectionNameSM.Factory> { factory -> factory.create(collectionName) }
        val navigator = LocalNavigator.currentOrThrow
        val savedRed = vm.savedRed

        val selectedCollection by savedRed.collections.selectedCollection.collectAsStateWithLifecycle()

        val closeCollection: () -> Unit = remember(savedRed, popOnBack, navigator) {
            {
                Timber.d("BackHandler SavedCollectionTab")
                savedRed.collections.selectedCollection.value = null
                if (popOnBack) {
                    navigator.pop()
                }
            }
        }

        BackHandler(onBack = closeCollection)

        val columnSelectRaw by Settings.r_collectionTab_column_current_count.field.collectAsStateWithLifecycle()
        val columnSelect = normalizeRColumnCount(columnSelectRaw)

        //Изменение количества отображаемых элементов
        LaunchedEffect(columnSelect) { vm.likedHost.columns = columnSelect }

        val onClickOpenProfile: (String) -> Unit = remember(navigator) {
            { profileName -> navigator.push(ScreenRedProfile(profileName)) }
        }

        val titleText = selectedCollection ?: collectionName

        Scaffold(
            contentWindowInsets = ZERO_WINDOW_INSETS,
            topBar = {
                Row(
                    modifier = Modifier
                        .padding(top = getTopInsetDp())
                        .then(TOP_BAR_HORIZONTAL_PADDING_MODIFIER),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = closeCollection) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = CD_BACK,
                            tint = Theme.R.colorYellow
                        )
                    }
                    Text(
                        text = titleText,
                        modifier = TITLE_MODIFIER,
                        color = Theme.R.colorYellow,
                        fontSize = TITLE_FONT_SIZE,
                        fontFamily = Theme.R.fontFamilyPopinsRegular
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = CONTENT_BOX_BASE_MODIFIER.padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LazyRow123(
                    host = vm.likedHost,
                    modifier = LAZY_ROW_MODIFIER,
                    onClickOpenProfile = onClickOpenProfile
                )
            }
        }

    }
}


@Stable
class ScreenRedCollectionNameSM @AssistedInject constructor(
    @Assisted val collectionName: String,
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(collectionName: String): ScreenRedCollectionNameSM
    }

    val likedHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        typePager = TypePager.SAVED_COLLECTION,
        extraString = collectionName,
        startOrder = Order.LATEST,
        block = block,
        redApi = redApi,
        savedRed = savedRed,
        downloadRed = downloadRed,
        search = search,
        searchNiches = searchNiches,
        isCollection = true
    )


}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedCollectionName {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenRedCollectionNameSM.Factory::class)
    abstract fun bindScreenRedSavedCollectionNameScreenModel(hiltDetailsScreenModelFactory: ScreenRedCollectionNameSM.Factory): ScreenModelFactory
}



