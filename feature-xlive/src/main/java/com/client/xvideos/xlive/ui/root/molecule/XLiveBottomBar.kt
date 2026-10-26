package com.client.xvideos.xlive.ui.root.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.ui.root.XLiveBarActions
import com.client.xvideos.xlive.ui.root.XLiveBarState
import com.client.xvideos.xlive.ui.root.XLiveTab
import com.client.xvideos.xlive.ui.saved.XLiveSavedTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Значки нижних вкладок — в порядке [XLiveTab]. */
private val tabIcons: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.LiveTv,
    Icons.Outlined.Tag,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Search,
)

/** Подсказки вкладок по долгому нажатию — в порядке [XLiveTab]. */
private val tabHints: ImmutableList<String> = persistentListOf(
    "Эфиры — комнаты в эфире",
    "Теги — комнаты по тегам",
    "Избранное — сохранённые комнаты",
    "Поиск — по нику и теме",
)

/** Значки подборок — в порядке [XLiveCollection]. */
private val collectionIcons: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.LocalFireDepartment,
    Icons.Outlined.FiberNew,
    Icons.Outlined.Shuffle,
)

/** Подсказки подборок — в порядке [XLiveCollection]. */
private val collectionHints: ImmutableList<String> = persistentListOf(
    "Популярные — больше всего зрителей",
    "Новые комнаты",
    "Случайные комнаты",
)

/** Значки порядка тегов — в порядке [XLiveTagSort]: зрители, комнаты, А–Я. */
private val tagSortIcons: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.Visibility,
    Icons.Outlined.Videocam,
    Icons.Outlined.SortByAlpha,
)

/** Подсказки порядка тегов — в порядке [XLiveTagSort]. */
private val tagSortHints: ImmutableList<String> = persistentListOf(
    "Теги по числу зрителей",
    "Теги по числу комнат",
    "Теги по алфавиту",
)

/** Значки подвкладок «Избранного» — в порядке [XLiveSavedTab]. */
private val savedTabIcons: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.Bookmarks,
    Icons.Outlined.FavoriteBorder,
    Icons.Outlined.History,
    Icons.Outlined.VideoLibrary,
)

/** Подсказки подвкладок «Избранного» — в порядке [XLiveSavedTab]. */
private val savedTabHints: ImmutableList<String> = persistentListOf(
    "Мои — сохранённые в приложении",
    "Подписки аккаунта на сайте",
    "История — недавно открытые",
    "Записи — сохранённые эфиры",
)

/**
 * Нижняя панель раздела XLive — двухуровневая, как у X: сверху ряд текущей
 * вкладки (подборки «Эфиров», порядок «Тегов», подвкладки «Избранного»),
 * снизу вкладки и справа переключатель пола.
 *
 * Второй ряд — значки, как подвкладки «Сохранённого» у X: подборки — разные
 * ленты, порядок — разные таблицы, а не фильтр внутри одной. Что значит
 * значок, говорит подсказка по долгому нажатию. Нажатие на уже выбранный
 * значок второго ряда обновляет список — решает ScreenModel.
 */
@Composable
fun XLiveBottomBar(
    bar: XLiveBarState,
    actions: XLiveBarActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = Color(0xFF333333))
        when (bar.tab) {
            XLiveTab.ROOMS -> TabRow(
                titlesIcon = collectionIcons,
                value = bar.collection.ordinal,
                onChangeState = { actions.onCollection(XLiveCollection.entries[it]) },
                containerColor = Theme.tabLevel1,
                tooltips = collectionHints,
            )
            XLiveTab.TAGS -> TabRow(
                titlesIcon = tagSortIcons,
                value = bar.tagSort.ordinal,
                onChangeState = { actions.onTagSort(XLiveTagSort.entries[it]) },
                containerColor = Theme.tabLevel1,
                tooltips = tagSortHints,
            )
            XLiveTab.SAVED -> TabRow(
                titlesIcon = savedTabIcons,
                value = bar.savedTab.ordinal,
                onChangeState = { actions.onSavedTab(XLiveSavedTab.entries[it]) },
                containerColor = Theme.tabLevel1,
                tooltips = savedTabHints,
            )
            XLiveTab.SEARCH -> Unit
        }
        Row(
            modifier = Modifier.background(Theme.tabLevel0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TabRow(
                    titlesIcon = tabIcons,
                    value = bar.tab.ordinal,
                    onChangeState = { actions.onTab(XLiveTab.entries[it]) },
                    containerColor = Theme.tabLevel0,
                    tooltips = tabHints,
                )
            }
            XLiveGenderSwitch(current = bar.gender, onSelect = actions.onGender)
        }
    }
}

@Preview
@Composable
private fun XLiveBottomBarPreview() {
    XLiveBottomBar(bar = XLiveBarState(), actions = XLiveBarActions())
}
