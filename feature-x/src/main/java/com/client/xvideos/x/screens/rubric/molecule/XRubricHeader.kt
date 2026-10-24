package com.client.xvideos.x.screens.rubric.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.x.screens.tags.atom.TagsHeader

/**
 * Шапка выдачи категории: название из меню, под ним [subtitle] страницы (число
 * роликов) и панель фильтров [filterBar]. Прокручивается вместе со списком; на
 * широком окне — блоком по центру, название и фильтры с одного края.
 */
@Composable
fun XRubricHeader(
    title: String,
    subtitle: String,
    topCutout: Dp,
    filterBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .adaptiveContentWidth()
    ) {
        // Собственный текст заголовка страницы не нужен: это то же название.
        TagsHeader(tag = title, title0 = "", title1 = subtitle, topCutout = topCutout)
        filterBar()
    }
}

@Preview
@Composable
private fun XRubricHeaderPreview() {
    XRubricHeader(
        title = "Категория Б",
        subtitle = "(1 000 результатов)",
        topCutout = 24.dp,
        filterBar = { Text("Фильтры", color = Color.White) },
    )
}
