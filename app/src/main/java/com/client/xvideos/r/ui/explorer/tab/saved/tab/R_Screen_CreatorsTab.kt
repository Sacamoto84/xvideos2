package com.client.xvideos.r.ui.explorer.tab.saved.tab

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import com.client.xvideos.common.theme.LavenderDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.profile.atom.VerticalScrollbar
import com.client.xvideos.r.ui.profile.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

object R_Screen_CreatorsTab : Screen {

    private fun readResolve(): Any = R_Screen_CreatorsTab

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedCreatorSM>()
        val state = rememberLazyListState()
        val savedRed = vm.savedRed

        var itemPendingDelete by remember { mutableStateOf<UserInfo?>(null) }

        val onCreatorClick = remember(navigator) {
            { username: String -> navigator.push(ScreenRedProfile(username)) }
        }
        val onDeleteRequest = remember {
            { user: UserInfo -> itemPendingDelete = user }
        }

        DeleteCreatorDialog(
            item = itemPendingDelete,
            onDismiss = { itemPendingDelete = null },
            onConfirm = { pending ->
                savedRed.creators.remove(pending.username)
                itemPendingDelete = null
            }
        )

        Scaffold(
            containerColor = Theme.background,
            topBar = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        // Высота = вырез камеры, минимум 16dp (union = max).
                        // statusBars не годится: бары спрятаны, их инсет всегда 0.
                        .windowInsetsTopHeight(WindowInsets.displayCutout.union(WindowInsets(top = 16.dp))),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        "Авторы",
                        modifier = Modifier,
                        color = Theme.R.colorYellow,
                        fontSize = 18.sp,
                        fontFamily = Theme.R.fontFamilyPopinsRegular,
                        textAlign = TextAlign.Center
                    )
                }
        }) { padding ->
            Box(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedRed.creators.list, key = { it.username }) { item ->
                        CreatorListItem(
                            item = item,
                            onClick = onCreatorClick,
                            onDelete = onDeleteRequest
                        )
                    }
                }

                CreatorsScrollbar(state)
            }
        }
    }
}

@Composable
private fun BoxScope.CreatorsScrollbar(state: LazyListState) {
    val scrollPercent by rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
        gridState = state, itemsToIgnore = 0
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .width(2.dp)
    ) {
        VerticalScrollbar(scrollPercent)
    }
}

@Composable
private fun CreatorListItem(
    item: UserInfo,
    onClick: (String) -> Unit,
    onDelete: (UserInfo) -> Unit
) {
    val displayName = item.name.ifBlank { item.username }

    Row(
        modifier = Modifier
            .padding(vertical = 2.dp, horizontal = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .background(Theme.tabLevel3)
            .clickable { onClick(item.username) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (item.profileImageUrl != null) {
            UrlImage(
                item.profileImageUrl,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = Theme.R.fontFamilyDMsanss,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (displayName != item.username) {
                Text(
                    "@${item.username}",
                    color = Color(0xFF9E9DA9),
                    fontSize = 12.sp,
                    fontFamily = Theme.R.fontFamilyDMsanss,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CreatorMetric(
                    label = "Подписчики",
                    value = item.followers,
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = "Просмотры",
                    value = item.views,
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = "Посты",
                    value = item.publishedGifs,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        IconButton(
            onClick = { onDelete(item) },
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить автора",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(24.dp)
            )
        }
        //Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun CreatorMetric(
    label: String,
    value: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF242424))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value.toPrettyCount(),
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            maxLines = 1
        )
        Text(
            label,
            color = Color(0xFF9E9DA9),
            fontSize = 9.sp,
            fontFamily = Theme.R.fontFamilyDMsanss,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeleteCreatorDialog(
    item: UserInfo?,
    onDismiss: () -> Unit,
    onConfirm: (UserInfo) -> Unit
) {
    item?.let { pending ->
        LavenderDialog(
            title = "Удалить автора?",
            onDismiss = onDismiss,
            icon = {
                pending.profileImageUrl?.let {
                    UrlImage(it, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(96.dp))
                }
            },
            body = buildAnnotatedString {
                append("Удалить «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из сохранённых?")
            },
            confirmText = "Удалить",
            onConfirm = { onConfirm(pending) },
            destructive = true,
        )
    }
}

class ScreenSavedCreatorSM @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    val savedRed: SavedRed,
    val block: BlockRed,
    val redApi: RedApi,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
) : ScreenModel {

    val gridState = LazyGridState()

    val likedHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        typePager = TypePager.SAVED_COLLECTION,
        extraString = "",
        startOrder = Order.LATEST,
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
abstract class ScreenModuleRedSavedCreator {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCreatorSM::class)
    abstract fun bindScreenRedSavedCreatorScreenModel(hiltListScreenModel: ScreenSavedCreatorSM): ScreenModel
}

@Preview
@Composable
private fun CreatorListItemPreview() {
    val sampleUser = UserInfo(
        name = "Sample Creator",
        username = "samplecreator",
        profileImageUrl = "https://via.placeholder.com/96",
        followers = 21_193,
        views = 32_986_108,
        publishedGifs = 2_176,
        url = "https://example.com/samplecreator"
    )
    CreatorListItem(
        item = sampleUser,
        onClick = {},
        onDelete = {}
    )
}

@Preview
@Composable
private fun DeleteCreatorDialogPreview() {
    XvideosTheme {
        val sampleUser = UserInfo(
            name = "Sample Creator",
            username = "samplecreator",
            profileImageUrl = "https://via.placeholder.com/96",
            followers = 21_193,
            views = 32_986_108,
            publishedGifs = 2_176,
            url = "https://example.com/samplecreator"
        )
        DeleteCreatorDialog(
            item = sampleUser,
            onDismiss = {},
            onConfirm = {}
        )
    }
}
