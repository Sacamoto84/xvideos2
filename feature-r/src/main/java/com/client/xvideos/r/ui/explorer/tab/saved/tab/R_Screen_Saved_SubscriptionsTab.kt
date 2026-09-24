package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler
import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import com.client.xvideos.common.util.getTopInsetDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.saved.SelectedCreator
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

object R_Screen_Saved_SubscriptionsTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_SubscriptionsTab

    override val key: ScreenKey = "R_Screen_Saved_SubscriptionsTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedSubscriptionsSM = getScreenModel()

        val pager = vm.likedHost.pager.collectAsLazyPagingItems()

        // Используем SnapshotStateList напрямую для реактивности UI
        val selectedListCreator = vm.savedRed.subscriptions.selectedListCreator

        val hasSelectedCreators by remember(selectedListCreator) {
            derivedStateOf { selectedListCreator.any { it.select } }
        }

        var userToDelete by remember { mutableStateOf<SelectedCreator?>(null) }

        BackHandler(enabled = userToDelete != null) {
            userToDelete = null
        }
        BackHandler(enabled = userToDelete == null && hasSelectedCreators) {
            for (i in selectedListCreator.indices) {
                if (selectedListCreator[i].select) {
                    selectedListCreator[i] = selectedListCreator[i].copy(select = false)
                }
            }
            pager.refresh()
        }

        val onOpenProfile = remember(navigator) {
            { username: String -> navigator.push(ScreenRedProfile(username)) }
        }
        val onSelectCreator = remember(selectedListCreator, pager) {
            { name: String ->
                val index = selectedListCreator.indexOfFirst { it.name == name }
                if (index != -1) {
                    val item = selectedListCreator[index]
                    selectedListCreator[index] = item.copy(select = !item.select)
                    pager.refresh()
                }
            }
        }
        val onLongClick = remember(selectedListCreator) {
            { name: String ->
                userToDelete = selectedListCreator.firstOrNull { it.name == name }
            }
        }

        SubscriptionsTabContent(
            host = vm.likedHost,
            listCreatorSelectedCreator = selectedListCreator,
            onOpenProfile = onOpenProfile,
            onSelectCreator = onSelectCreator,
            onLongClick = onLongClick
        )

        val onConfirmDelete = remember(vm.savedRed, pager) {
            { creatorName: String ->
                vm.savedRed.subscriptions.remove(creatorName)
                userToDelete = null
                pager.refresh()
            }
        }
        val onDismissDelete = remember { { userToDelete = null } }
        val userToDeleteProvider = remember(userToDelete) { { userToDelete } }

        DialogSubscriptionDelete(
            user = userToDeleteProvider,
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )
    }
}

@Composable
fun SubscriptionsTabContent(
    host: LazyRow123Host?,
    listCreatorSelectedCreator: List<SelectedCreator>,
    onOpenProfile: (String) -> Unit,
    onSelectCreator: (String) -> Unit,
    onLongClick : (String) -> Unit = {}
) {
    val renderContentBeforeList: @Composable () -> Unit = remember(
        listCreatorSelectedCreator,
        onSelectCreator,
        onLongClick
    ) {
        {
            CreatorsHeader(
                listCreators = listCreatorSelectedCreator,
                onCreatorClick = onSelectCreator,
                onLongClick = onLongClick
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        if (host != null) {
            LazyRow123(
                host = host,
                modifier = Modifier.fillMaxSize(),
                onClickOpenProfile = onOpenProfile,
                contentPadding = PaddingValues(top = getTopInsetDp()),
                contentBeforeList = renderContentBeforeList,
                isRunLike = true
            )
        } else {
            // Fallback for Preview
            Box(modifier = Modifier.padding(top = getTopInsetDp())) {
                renderContentBeforeList()
            }
        }
    }
}

@Composable
fun CreatorsHeader(
    listCreators: List<SelectedCreator>,
    onCreatorClick: (String) -> Unit,
    onLongClick : (String) -> Unit = {}
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        listCreators.forEach { creator ->
            CreatorChipItem(
                creator = creator,
                onCreatorClick = onCreatorClick,
                onLongClick = onLongClick
            )
        }
    }
}

@Composable
private fun CreatorChipItem(
    creator: SelectedCreator,
    onCreatorClick: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    val onClick = remember(creator.name, onCreatorClick) { { onCreatorClick(creator.name) } }
    val onLong = remember(creator.name, onLongClick) { { onLongClick(creator.name) } }
    CreatorChip(
        creator = creator.name,
        url = creator.urlProfile,
        isSelected = creator.select,
        onClick = onClick,
        onLongClick = onLong
    )
}

@Composable
fun CreatorChip(
    creator: String,
    url: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.Gray, RoundedCornerShape(50))
            .background(
                if (isSelected) Color.Gray else Color.Transparent,
                RoundedCornerShape(50)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = null,
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {

            if (url != null) {
                UrlImage(url = url)
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = creator,
            fontSize = 16.sp,
            color = Color.White,
            fontFamily = Theme.R.fontFamilyPopinsRegular
        )
        Spacer(Modifier.width(4.dp))
    }
}

@Preview
@Composable
fun SubscriptionsTabPreview() {
    SubscriptionsTabContent(
        host = null,
        listCreatorSelectedCreator = listOf(
            SelectedCreator("Creator 1", true, null),
            SelectedCreator("Another One", false, null),
            SelectedCreator("Superstar", true, null)
        ),
        onOpenProfile = {},
        onSelectCreator = {}
    )
}

@Stable
class ScreenSavedSubscriptionsSM @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
) : ScreenModel {

    val likedHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        typePager = TypePager.SUBSCRIPTIONS,
        block = block,
        redApi = redApi,
        savedRed = savedRed,
        downloadRed = downloadRed,
        search = search,
        searchNiches = searchNiches
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedSubscriptions {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedSubscriptionsSM::class)
    abstract fun bindScreenRedSavedSubscriptionsScreenModel(hiltListScreenModel: ScreenSavedSubscriptionsSM): ScreenModel
}
