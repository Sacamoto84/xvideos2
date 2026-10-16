package com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.molecule

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.choice.ChoiceRow
import com.client.xvideos.common.ui.choice.ChoiceRowStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Отбор на «Лайках» L: ряд выбора дизайн-системы в стиле C, в полосе до 640dp. */
@Composable
fun LikesFilterRow(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    topInset: Dp,
    modifier: Modifier = Modifier
) {
    ChoiceRow(
        options = options,
        selectedIndex = selectedIndex,
        onSelectIndex = onSelectIndex,
        style = ChoiceRowStyle.TABS,
        accent = Theme.L.primaryColor,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(top = topInset)
            .adaptiveContentWidth()
            .padding(horizontal = 4.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LikesFilterRowPreview() {
    LikesFilterRow(
        options = persistentListOf("All", "Image", "Gif"),
        selectedIndex = 0,
        onSelectIndex = {},
        topInset = 24.dp
    )
}
