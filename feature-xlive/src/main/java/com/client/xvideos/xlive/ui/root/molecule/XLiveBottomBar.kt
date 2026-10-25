package com.client.xvideos.xlive.ui.root.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Tag
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
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.ui.root.XLiveTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Значки нижних вкладок — в порядке [XLiveTab]. */
private val tabIcons: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.LiveTv,
    Icons.Outlined.Tag,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Search,
)

/** Значки подборок — в порядке [XLiveCollection]. */
private val collectionIcons: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.LocalFireDepartment,
    Icons.Outlined.FiberNew,
    Icons.Outlined.Shuffle,
)

/**
 * Нижняя панель раздела XLive — двухуровневая, как у X: сверху ряд
 * подборок «Эфиров», снизу вкладки и справа переключатель пола.
 *
 * Подборки — ряд значков на втором уровне, как подвкладки «Сохранённого» у X:
 * это разные ленты, а не фильтр внутри одной.
 */
@Composable
fun XLiveBottomBar(
    tab: XLiveTab,
    onTab: (XLiveTab) -> Unit,
    collection: XLiveCollection,
    onCollection: (XLiveCollection) -> Unit,
    gender: XLiveGender,
    onGender: (XLiveGender) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = Color(0xFF333333))
        if (tab == XLiveTab.ROOMS) {
            TabRow(
                titlesIcon = collectionIcons,
                value = collection.ordinal,
                onChangeState = { onCollection(XLiveCollection.entries[it]) },
                containerColor = Theme.tabLevel1,
            )
        }
        Row(
            modifier = Modifier.background(Theme.tabLevel0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TabRow(
                    titlesIcon = tabIcons,
                    value = tab.ordinal,
                    onChangeState = { onTab(XLiveTab.entries[it]) },
                    containerColor = Theme.tabLevel0,
                )
            }
            XLiveGenderSwitch(current = gender, onSelect = onGender)
        }
    }
}

@Preview
@Composable
private fun XLiveBottomBarPreview() {
    XLiveBottomBar(
        tab = XLiveTab.ROOMS,
        onTab = {},
        collection = XLiveCollection.POPULAR,
        onCollection = {},
        gender = XLiveGender.ALL,
        onGender = {},
    )
}
