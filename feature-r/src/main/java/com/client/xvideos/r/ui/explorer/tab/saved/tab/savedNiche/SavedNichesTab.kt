package com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche

import androidx.activity.compose.BackHandler
import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import com.client.xvideos.common.util.getTopInsetDp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

private val NICHE_ROW_CORNER = 8.dp
private val NICHE_ROW_SHAPE = RoundedCornerShape(NICHE_ROW_CORNER)
private val NICHE_ROW_VERTICAL_PADDING = 2.dp
private val NICHE_ROW_HORIZONTAL_PADDING = 6.dp
private val NICHE_THUMBNAIL_SIZE = 96.dp
private val NICHE_SPACER_WIDTH = 8.dp
private val DELETE_BUTTON_WIDTH = 96.dp
private val DELETE_BUTTON_HEIGHT = 48.dp
private val DELETE_BUTTON_BORDER_WIDTH = 1.dp
private val TOP_BAR_PADDING = 8.dp
private val TOP_BAR_TITLE_FONT_SIZE = 18.sp
private val NICHE_NAME_FONT_SIZE = 20.sp
private val DELETE_TEXT_FONT_SIZE = 18.sp
private val SCROLLBAR_WIDTH = 2.dp
private val ZERO_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)
private const val TEXT_NICHES_TITLE = "Группы"
private const val TEXT_LEAVE_NICHE = "Выйти"

private val TOP_BAR_HORIZONTAL_PADDING_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(start = TOP_BAR_PADDING, top = TOP_BAR_PADDING, bottom = TOP_BAR_PADDING)

private val LAZY_COLUMN_MODIFIER = Modifier.fillMaxSize()
private val SCROLLBAR_CONTAINER_MODIFIER = Modifier
    .fillMaxHeight()
    .width(SCROLLBAR_WIDTH)

private val NICHE_ROW_BASE_MODIFIER = Modifier
    .padding(vertical = NICHE_ROW_VERTICAL_PADDING, horizontal = NICHE_ROW_HORIZONTAL_PADDING)
    .fillMaxWidth()
    .clip(NICHE_ROW_SHAPE)

private val NICHE_ROW_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val NICHE_THUMBNAIL_MODIFIER = Modifier.size(NICHE_THUMBNAIL_SIZE)
private val NICHE_SPACER_MODIFIER = Modifier.width(NICHE_SPACER_WIDTH)

private val DELETE_BUTTON_BASE_MODIFIER = Modifier
    .width(DELETE_BUTTON_WIDTH)
    .height(DELETE_BUTTON_HEIGHT)
    .clip(NICHE_ROW_SHAPE)
    .border(DELETE_BUTTON_BORDER_WIDTH, Color.White, NICHE_ROW_SHAPE)
    .background(Color.Black)

object SavedNichesTab : Screen {

    private fun readResolve(): Any = SavedNichesTab

    override val key: ScreenKey = "SavedNichesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedNichesSM = getScreenModel()
        val state = rememberLazyListState()

        // Без `by`: см. VerticalScrollbar — чтение позиции скролла здесь
        // перекомпоновывало бы весь экран на каждом кадре.
        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
            gridState = state, itemsToIgnore = 0
        )

        var itemPendingDelete by remember { mutableStateOf<NichesInfo?>(null) }
        val onDismissDelete = remember { { itemPendingDelete = null } }
        val onConfirmDelete = remember(vm) {
            { pending: NichesInfo ->
                vm.savedRed.niches.remove(pending)
                itemPendingDelete = null
            }
        }

        BackHandler(enabled = itemPendingDelete != null, onBack = onDismissDelete)

        DialogNicheDelete(
            item = itemPendingDelete,
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )

        val onNicheClick: (NichesInfo) -> Unit = remember(navigator) {
            { item -> navigator.push(R_ScreenNiche(item.id)) }
        }
        val onDeleteClick: (NichesInfo) -> Unit = remember {
            { item -> itemPendingDelete = item }
        }
        val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

        Scaffold(
            contentWindowInsets = ZERO_WINDOW_INSETS,
            topBar = {
                Row(
                    modifier = Modifier
                        .padding(top = getTopInsetDp())
                        .then(TOP_BAR_HORIZONTAL_PADDING_MODIFIER),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        TEXT_NICHES_TITLE,
                        color = Theme.R.colorYellow,
                        fontSize = TOP_BAR_TITLE_FONT_SIZE,
                        fontFamily = Theme.R.fontFamilyPopinsRegular
                    )
                }
            },
            containerColor = Theme.background
        ) { padding ->

            Box(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
            ) {

                LazyColumn(
                    state = state,
                    modifier = LAZY_COLUMN_MODIFIER
                ) {
                    items(vm.savedRed.niches.list, key = { it.id }, contentType = { "saved_niche" }) { item ->
                        SavedNicheRow(
                            item = item,
                            onClick = onNicheClick,
                            onDeleteClick = onDeleteClick
                        )
                    }
                }

                Box(
                    modifier = SCROLLBAR_CONTAINER_MODIFIER
                        .align(Alignment.CenterEnd)
                ) {
                    VerticalScrollbar(scrollPercentProvider)
                }
            }
        }
    }
}

@Composable
private fun SavedNicheRow(
    item: NichesInfo,
    onClick: (NichesInfo) -> Unit,
    onDeleteClick: (NichesInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val onRowClick = remember(item, onClick) { { onClick(item) } }
    val onRowDelete = remember(item, onDeleteClick) { { onDeleteClick(item) } }

    Row(
        modifier = modifier
            .then(NICHE_ROW_BASE_MODIFIER)
            .background(Theme.tabLevel3)
            .clickable(onClick = onRowClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = NICHE_ROW_HORIZONTAL_ARRANGEMENT
    ) {
        UrlImage(item.thumbnail, modifier = NICHE_THUMBNAIL_MODIFIER)
        Spacer(modifier = NICHE_SPACER_MODIFIER)
        Text(
            item.name,
            color = Color.White,
            fontSize = NICHE_NAME_FONT_SIZE,
            fontFamily = Theme.R.fontFamilyDMsanss,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = DELETE_BUTTON_BASE_MODIFIER.clickable(onClick = onRowDelete),
            contentAlignment = Alignment.Center
        ) {
            Text(
                TEXT_LEAVE_NICHE,
                fontFamily = Theme.R.fontFamilyDMsanss,
                fontSize = DELETE_TEXT_FONT_SIZE,
                color = Color.White
            )
        }

        Spacer(modifier = NICHE_SPACER_MODIFIER)
    }
}



@Stable
class ScreenSavedNichesSM @Inject constructor( val savedRed: SavedRed ) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedNichesSM::class)
    abstract fun bindScreenRedSavedNichesScreenModel(screenModel: ScreenSavedNichesSM): ScreenModel
}

