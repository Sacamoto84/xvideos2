package com.client.xvideos.r.ui.explorer.tab.saved.tab.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.saved.tab.collection.molecule.CollectionNameTopBar
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.ui.theme.XvideosTheme
import timber.log.Timber

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

        LaunchedEffect(columnSelect) { vm.likedHost.columns = columnSelect }

        val onClickOpenProfile: (String) -> Unit = remember(navigator) {
            { profileName -> navigator.push(ScreenRedProfile(profileName)) }
        }

        val titleText = selectedCollection ?: collectionName

        ScreenCollectionNameContent(
            title = titleText,
            host = vm.likedHost,
            topInset = getTopInsetDp(),
            onBackClick = closeCollection,
            onClickOpenProfile = onClickOpenProfile
        )
    }
}

@Composable
fun ScreenCollectionNameContent(
    title: String,
    host: LazyRow123Host?,
    topInset: Dp,
    onBackClick: () -> Unit,
    onClickOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Шапка — первым элементом сетки: на всю ширину и уезжает с ней, как на
    // вкладках сохранённого. Вырез — отступом списка, под ним и индикатор обновления.
    val header: @Composable () -> Unit = remember(title, onBackClick) {
        { CollectionNameTopBar(title = title, onBackClick = onBackClick) }
    }
    val contentPadding = remember(topInset) { PaddingValues(top = topInset) }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (host != null) {
                LazyRow123(
                    host = host,
                    modifier = Modifier.fillMaxSize(),
                    onClickOpenProfile = onClickOpenProfile,
                    contentPadding = contentPadding,
                    contentBeforeList = header
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScreenCollectionNameContentPreview() {
    XvideosTheme(darkTheme = true) {
        ScreenCollectionNameContent(
            title = "Favorites",
            host = null,
            topInset = 24.dp,
            onBackClick = {},
            onClickOpenProfile = {}
        )
    }
}
