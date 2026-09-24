package com.client.xvideos.r.ui.niche

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreator
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.niche.atom.NichePreview
import com.client.xvideos.r.ui.niche.atom.NicheProfileContent
import com.client.xvideos.r.ui.niche.atom.NicheTopCreator
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.ui.theme.XvideosTheme
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
    onFollowClick: () -> Unit
) {
    StatelessScreenNicheContent(
        content = { padding ->
            Box(
                modifier = Modifier
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
    )
}


@Composable
private fun StatelessScreenNicheContent(
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.background
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content(padding)
        }
    }
}

@Composable
private fun NicheHeaderContent(
    niche: NichesInfo,
    relatedNiches: () -> NichesResponse,
    topCreators: () -> TopCreatorsResponse,
    onNicheClick: (String) -> Unit,
    onCreatorClick: (String) -> Unit,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    columns: Int,
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .fillMaxWidth()
            .background(Theme.background)
    ) {

        NicheProfileContent(
            niche = { niche },
            isFollowed = isFollowed,
            onFollowClick = onFollowClick
        )

        val related = relatedNiches().niches
        if (related.isNotEmpty()) {
            Text(
                "Related Niches",
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )
            LazyRow(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = related,
                    key = { item -> item.id },
                    contentType = { "niche_preview" }
                ) { item ->
                    NichePreviewItem(item = item, onClick = onNicheClick)
                }
            }
        }

        val creators = topCreators().creators
        if (creators.isNotEmpty()) {
            Text(
                "✨ Top Creators in ${niche.name}",
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = creators,
                    key = { creator -> creator.username },
                    contentType = { "top_creator" }
                ) { creator ->
                    NicheCreatorItem(creator = creator, onClick = onCreatorClick)
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        NicheBottomBar(niche = niche, currentSort = currentSort, onSortChange = onSortChange, columns = columns)
    }
}

@Composable
private fun NichePreviewItem(
    item: Niche,
    onClick: (String) -> Unit
) {
    val handleClick = remember(item.id, onClick) { { onClick(item.id) } }
    val itemProvider = remember(item) { { item } }
    NichePreview(itemProvider, onClick = handleClick)
}

@Composable
private fun NicheCreatorItem(
    creator: TopCreator,
    onClick: (String) -> Unit
) {
    val handleClick = remember(creator.username, onClick) { { onClick(creator.username) } }
    NicheTopCreator(creator, onClick = handleClick)
}

@Preview
@Composable
private fun ScreenNicheContentPreview() {
    XvideosTheme {
        StatelessScreenNicheContent(
            content = { padding ->
                Column(
                    modifier = Modifier
                        //.padding(bottom = padding.calculateBottomPadding())
                        .fillMaxSize()
                ) {
                    NicheHeaderContent(
                        niche = sampleNicheInfo,
                        relatedNiches = { sampleNichesResponse },
                        topCreators = { sampleTopCreatorsResponse },
                        onNicheClick = {},
                        onCreatorClick = {},
                        isFollowed = false,
                        onFollowClick = {},
                        currentSort = Order.NICHES_NAME_A_Z,
                        onSortChange = {},
                        columns = 2,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("LazyRow123 Placeholder", color = Color.Gray)
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun NicheHeaderContentPreview() {
    XvideosTheme {
        NicheHeaderContent(
            niche = sampleNicheInfo,
            relatedNiches = { sampleNichesResponse },
            topCreators = { sampleTopCreatorsResponse },
            onNicheClick = {},
            onCreatorClick = {},
            isFollowed = true,
            onFollowClick = {},
            currentSort = Order.NICHES_NAME_A_Z,
            onSortChange = {},
            columns = 2,
        )
    }
}

val sampleNicheInfo = NichesInfo(
    cover = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
    description = "A collection of beautiful female backs.",
    gifs = 245,
    id = "female-backs",
    name = "Female Backs",
    owner = "owner",
    subscribers = 914,
    thumbnail = "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
    rules = "rules"
)

val sampleNichesResponse = NichesResponse(
    niches = listOf(
        Niche(
            id = "amateur-milf",
            name = "Amateur MILF",
            gifs = 1234,
            subscribers = 5678,
            thumbnail = "https://userpic.redgifs.com/niches/thumbnails/amateur-milf-thumbnail.jpg",
            previews = listOf()
        ),
        Niche(
            id = "teen-petite",
            name = "Teen Petite",
            gifs = 4321,
            subscribers = 8765,
            thumbnail = "https://userpic.redgifs.com/niches/thumbnails/teen-petite-thumbnail.jpg",
            previews = listOf()
        )
    ),
    page = 1,
    pages = 1,
    total = 2
)

val sampleTopCreatorsResponse = TopCreatorsResponse(
    creators = listOf(
        TopCreator(
            creationtime = 1672531200,
            description = "I make videos.",
            followers = 100,
            gifs = 10,
            name = "Creator 1",
            profileImageUrl = "https://userpic.redgifs.com/users/creator1.jpg",
            username = "creator1",
            verified = true,
            studio = false,
            views = 1000
        ),
        TopCreator(
            creationtime = 1672531200,
            description = "I also make videos.",
            followers = 200,
            gifs = 20,
            name = "Creator 2",
            profileImageUrl = "https://userpic.redgifs.com/users/creator2.jpg",
            username = "creator2",
            verified = false,
            studio = true,
            views = 2000
        )
    )
)
