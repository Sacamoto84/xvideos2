package com.client.xvideos.r.ui.niche

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.niche.molecule.NicheHeaderContent
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.MetadataLoadState
import com.client.xvideos.r.ui.ui.atom.MetadataLoadingContent
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.PageableFeed
import timber.log.Timber

class R_ScreenNiche(val nicheName: String = "pumped-pussy") : Screen {

    override val key: ScreenKey = "RedNiche:$nicheName"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNicheSM, ScreenNicheSM.Factory> { factory -> factory.create(nicheName) }
        BackHandler {
            navigator.pop()
        }
        val columnSelect by Settings.r_current_count_niches.field.collectAsStateWithLifecycle()
        LaunchedEffect(columnSelect) {
            vm.lazyHost.columns = columnSelect
        }

        val metadata = vm.metadata
        if (metadata !is MetadataLoadState.Ready) {
            MetadataLoadingContent(
                errorMessage = (metadata as? MetadataLoadState.Failed)?.message,
                onRetry = vm::retryMetadata,
            )
            return
        }

        val niche = metadata.value
        val sort by vm.lazyHost.sortType.collectAsStateWithLifecycle()
        val savedRed = vm.savedRed

        val followedList = savedRed.niches.list

        val isFollowed by remember(niche.id, followedList) {
            derivedStateOf {
                followedList.any { it.id == niche.id }
            }
        }

        val onNicheClick: (String) -> Unit = remember(navigator) { { id -> navigator.push(R_ScreenNiche(id)) } }
        val onCreatorClick: (String) -> Unit = remember(navigator) { { username -> navigator.push(ScreenRedProfile(username)) } }

        val onFollowClick: () -> Unit = remember(isFollowed, niche) {
            {
                Timber.d("onFollowClick isFollowed: $isFollowed ${niche.id}")
                if (isFollowed)
                    savedRed.niches.remove(niche)
                else
                    savedRed.niches.add(niche)
            }
        }

        val relatedNichesProvider: () -> NichesResponse = remember(vm) { { vm.related } }
        val topCreatorsProvider: () -> TopCreatorsResponse = remember(vm) { { vm.topCreator } }
        val onSortChange: (Order) -> Unit = remember(vm) { { vm.lazyHost.changeSortType(it) } }

        ScreenNicheContent(
            niche = niche,
            relatedNiches = relatedNichesProvider,
            topCreators = topCreatorsProvider,
            lazyHost = vm.lazyHost,
            currentSort = sort,
            onSortChange = onSortChange,
            onNicheClick = onNicheClick,
            onCreatorClick = onCreatorClick,
            isFollowed = isFollowed,
            onFollowClick = onFollowClick
        )
    }
}

/**
 * Корневая компоновка экрана ниши раздела R.
 */
@Composable
fun ScreenNicheContent(
    niche: NichesInfo,
    relatedNiches: () -> NichesResponse,
    topCreators: () -> TopCreatorsResponse,
    lazyHost: LazyRow123Host,
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    onNicheClick: (String) -> Unit,
    onCreatorClick: (String) -> Unit,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PageableFeed(
        host = lazyHost,
        modifier = modifier,
        onClickOpenProfile = onCreatorClick,
        header = { scrollMode ->
            NicheHeaderContent(
                niche = niche,
                relatedNiches = relatedNiches,
                topCreators = topCreators,
                onNicheClick = onNicheClick,
                onCreatorClick = onCreatorClick,
                isFollowed = isFollowed,
                onFollowClick = onFollowClick,
                currentSort = currentSort,
                onSortChange = onSortChange,
                scrollMode = scrollMode,
            )
        },
    )
}
