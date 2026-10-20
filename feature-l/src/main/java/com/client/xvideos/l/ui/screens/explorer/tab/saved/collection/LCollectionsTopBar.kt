package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule.LSortMenuButton
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList



private val SORT_TITLES: ImmutableList<String> = LCollectionSortOrder.entries.map { it.title }.toPersistentList()

@Composable
internal fun LCollectionsTopBar(
    selectedCollection: String?,
    sortOrder: LCollectionSortOrder,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSelectSort = remember(onSortOrderClick) {
        { index: Int -> onSortOrderClick(LCollectionSortOrder.entries[index]) }
    }
    val collectionTitle = remember(selectedCollection) {
        selectedCollection?.let { ">$it" }
    }

    // Топ-бар лежит в Scaffold(topBar = ...) — Material3 не применяет инсет
    // к этому слоту, а хост таба паддит только низ. Соседние таби (Albums,
    // Likes) отступают от выреза сами; этот бар при миграции на Material3
    // потерял displayCutoutPadding и рендерился от y=0, под камерой.
    val topInset = getTopInsetDp()
    val columnModifier = modifier
        .fillMaxWidth()
        .padding(top = topInset)

    // Ряд — не шире 640dp по центру, разделитель под ним — на всю ширину.
    Column(
        modifier = columnModifier
    ) {
        Row(
            modifier = Modifier
                .adaptiveContentWidth()
                .fillMaxWidth()
                // Кнопка сортировки даёт поле 12dp вокруг значка: значок — на
                // 16dp от края, как стрелка «назад» в шапках.
                .padding(start = if (selectedCollection == null) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCollection == null) {
                LSortMenuButton(
                    options = SORT_TITLES,
                    selectedIndex = sortOrder.ordinal,
                    onSelectIndex = onSelectSort,
                    contentDescription = "Сортировка коллекций"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (collectionTitle != null) {
                    Text(
                        collectionTitle,
                        color = Theme.L.primaryColor,
                        fontSize = 18.sp,
                        fontFamily = Theme.L.fontFamilyPopinsRegular
                    )
                }
            }
        }

        HorizontalDivider()
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewLCollectionsTopBarSelectionNull() {
    XvideosTheme(darkTheme = true) {
        LCollectionsTopBar(
            selectedCollection = null,
            sortOrder = LCollectionSortOrder.RECENT,
            onSortOrderClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewLCollectionsTopBarWithSelection() {
    XvideosTheme(darkTheme = true) {
        LCollectionsTopBar(
            selectedCollection = "My Private Collection",
            sortOrder = LCollectionSortOrder.NAME,
            onSortOrderClick = {},
        )
    }
}

