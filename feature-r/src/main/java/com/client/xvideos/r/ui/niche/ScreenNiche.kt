package com.client.xvideos.r.ui.niche

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.niche.molecule.NicheHeaderContent
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
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
        val sort by vm.lazyHost.sortType.collectAsStateWithLifecycle()
        val savedRed = vm.savedRed

        val followedList = savedRed.niches.list

        val isFollowed by remember(vm.niche.id, followedList) {
            derivedStateOf {
                followedList.any { it.id == vm.niche.id }
            }
        }

        LaunchedEffect(columnSelect) {
            vm.lazyHost.columns = columnSelect
        }

        val onNicheClick: (String) -> Unit = remember(navigator) { { id -> navigator.push(R_ScreenNiche(id)) } }
        val onCreatorClick: (String) -> Unit = remember(navigator) { { username -> navigator.push(ScreenRedProfile(username)) } }

        val onFollowClick: () -> Unit = remember(isFollowed, vm.niche) {
            {
                val nicheInfo = vm.niche
                if (nicheInfo.id.isBlank()) return@remember
                Timber.d("onFollowClick isFollowed: $isFollowed ${nicheInfo.id}")
                if (isFollowed)
                    savedRed.niches.remove(nicheInfo)
                else
                    savedRed.niches.add(nicheInfo)
            }
        }

        val relatedNichesProvider: () -> NichesResponse = remember(vm) { { vm.related } }
        val topCreatorsProvider: () -> TopCreatorsResponse = remember(vm) { { vm.topCreator } }
        val onSortChange: (Order) -> Unit = remember(vm) { { vm.lazyHost.changeSortType(it) } }

        ScreenNicheContent(
            niche = vm.niche,
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .background(Theme.background)
                .fillMaxSize()
        ) {
            LazyRow123(
                host = lazyHost,
                modifier = Modifier.fillMaxSize(),
                onClickOpenProfile = onCreatorClick,
                contentBeforeList = {
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
                        columns = lazyHost.columns,
                    )
                }
            )
        }
    }
}
