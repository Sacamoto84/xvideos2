package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.client.xvideos.common.util.getTopInsetDp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import com.client.xvideos.common.theme.LavenderDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import com.client.xvideos.ui.theme.XvideosTheme
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

private const val TOP_BAR_TITLE = "Авторы"
private const val DIALOG_DELETE_TITLE = "Удалить автора?"
private const val BUTTON_DELETE = "Удалить"
private const val CD_DELETE_CREATOR = "Удалить автора"
private const val LABEL_FOLLOWERS = "Подписчики"
private const val LABEL_VIEWS = "Просмотры"
private const val LABEL_POSTS = "Посты"
private const val CONTENT_TYPE_CREATOR_ITEM = "creator_item"
private val ZERO_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)
private val TOP_BAR_START_PADDING = 8.dp
private val TOP_BAR_VERTICAL_PADDING = 8.dp
private val TOP_BAR_TITLE_SIZE = 18.sp
private val CREATOR_CARD_SHAPE = RoundedCornerShape(8.dp)
private val CREATOR_IMAGE_SIZE = 96.dp
private val CREATOR_METRIC_SHAPE = RoundedCornerShape(6.dp)
private val CREATOR_METRIC_BG_COLOR = Color(0xFF242424)
private val CREATOR_USERNAME_COLOR = Color(0xFF9E9DA9)
private val CREATOR_DELETE_ICON_COLOR = Color(0xFFAAAAAA)
private val SCROLLBAR_WIDTH = 2.dp

object R_Screen_CreatorsTab : Screen {

    private fun readResolve(): Any = R_Screen_CreatorsTab

    override val key: ScreenKey = "R_Screen_CreatorsTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedCreatorSM>()
        val state = rememberLazyListState()
        val savedRed = vm.savedRed

        var itemPendingDelete by remember { mutableStateOf<UserInfo?>(null) }
        val onDismissDelete = remember { { itemPendingDelete = null } }

        BackHandler(enabled = itemPendingDelete != null, onBack = onDismissDelete)

        val onCreatorClick = remember(navigator) {
            { username: String -> navigator.push(ScreenRedProfile(username)) }
        }
        val onDeleteRequest = remember {
            { user: UserInfo -> itemPendingDelete = user }
        }
        val onConfirmDelete = remember(savedRed) {
            { pending: UserInfo ->
                savedRed.creators.remove(pending.username)
                itemPendingDelete = null
            }
        }

        DeleteCreatorDialog(
            item = itemPendingDelete,
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )

        val topInset = getTopInsetDp()

        CreatorsTabContent(
            creators = savedRed.creators.list,
            state = state,
            topInset = topInset,
            onCreatorClick = onCreatorClick,
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
fun CreatorsTabContent(
    creators: List<UserInfo>,
    state: LazyListState,
    topInset: Dp,
    onCreatorClick: (String) -> Unit,
    onDeleteRequest: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Theme.background,
        contentWindowInsets = ZERO_WINDOW_INSETS,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset)
                    .padding(start = TOP_BAR_START_PADDING, top = TOP_BAR_VERTICAL_PADDING, bottom = TOP_BAR_VERTICAL_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    TOP_BAR_TITLE,
                    modifier = Modifier,
                    color = Theme.R.colorYellow,
                    fontSize = TOP_BAR_TITLE_SIZE,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = creators,
                    key = { it.username },
                    contentType = { CONTENT_TYPE_CREATOR_ITEM }
                ) { item ->
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

@Composable
private fun BoxScope.CreatorsScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
        gridState = state, itemsToIgnore = 0
    )
    val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .width(SCROLLBAR_WIDTH)
    ) {
        VerticalScrollbar(scrollPercentProvider)
    }
}

@Composable
private fun CreatorListItem(
    item: UserInfo,
    onClick: (String) -> Unit,
    onDelete: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = remember(item.name, item.username) { item.name.ifBlank { item.username } }
    val handleItemClick = remember(item.username, onClick) { { onClick(item.username) } }
    val handleDeleteClick = remember(item, onDelete) { { onDelete(item) } }

    Row(
        modifier = modifier
            .padding(vertical = 2.dp, horizontal = 6.dp)
            .clip(CREATOR_CARD_SHAPE)
            .fillMaxWidth()
            .background(Theme.tabLevel3)
            .clickable(onClick = handleItemClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (item.profileImageUrl != null) {
            UrlImage(
                item.profileImageUrl,
                modifier = Modifier.size(CREATOR_IMAGE_SIZE),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(CREATOR_IMAGE_SIZE)
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

        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .weight(1f),
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
                    color = CREATOR_USERNAME_COLOR,
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
                    label = LABEL_FOLLOWERS,
                    value = item.followers,
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = LABEL_VIEWS,
                    value = item.views,
                    modifier = Modifier.weight(1f)
                )
                CreatorMetric(
                    label = LABEL_POSTS,
                    value = item.publishedGifs,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        IconButton(
            onClick = handleDeleteClick,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = CD_DELETE_CREATOR,
                tint = CREATOR_DELETE_ICON_COLOR,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CreatorMetric(
    label: String,
    value: Long,
    modifier: Modifier = Modifier
) {
    val prettyValue = remember(value) { value.toPrettyCount() }
    Column(
        modifier = modifier
            .clip(CREATOR_METRIC_SHAPE)
            .background(CREATOR_METRIC_BG_COLOR)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            prettyValue,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            maxLines = 1
        )
        Text(
            label,
            color = CREATOR_USERNAME_COLOR,
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
        val handleConfirm = remember(pending, onConfirm) {
            { onConfirm(pending) }
        }
        LavenderDialog(
            title = DIALOG_DELETE_TITLE,
            onDismiss = onDismiss,
            icon = {
                pending.profileImageUrl?.let {
                    UrlImage(it, modifier = Modifier.clip(CREATOR_CARD_SHAPE).size(CREATOR_IMAGE_SIZE))
                }
            },
            body = buildAnnotatedString {
                append("Удалить «")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.name) }
                append("» из сохранённых?")
            },
            confirmText = BUTTON_DELETE,
            onConfirm = handleConfirm,
            destructive = true,
        )
    }
}

@Stable
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
private fun CreatorsTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        val sampleUser = UserInfo(
            name = "Sample Creator",
            username = "samplecreator",
            profileImageUrl = "https://via.placeholder.com/96",
            followers = 21_193,
            views = 32_986_108,
            publishedGifs = 2_176,
            url = "https://example.com/samplecreator"
        )
        CreatorsTabContent(
            creators = listOf(sampleUser),
            state = rememberLazyListState(),
            topInset = 24.dp,
            onCreatorClick = {},
            onDeleteRequest = {}
        )
    }
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
