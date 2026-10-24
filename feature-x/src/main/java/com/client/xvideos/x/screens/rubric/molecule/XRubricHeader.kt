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
import com.client.xvideos.x.screens.tags.atom.TagsHeader

/**
 * Шапка выдачи категории: название из меню, подпись страницы и под ними
 * панель фильтров [filterBar]. Прокручивается вместе со списком.
 */
@Composable
fun XRubricHeader(
    title: String,
    title0: String,
    title1: String,
    topCutout: Dp,
    filterBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TagsHeader(tag = title, title0 = title0, title1 = title1, topCutout = topCutout)
        filterBar()
    }
}

@Preview
@Composable
private fun XRubricHeaderPreview() {
    XRubricHeader(
        title = "Категория Б",
        title0 = "",
        title1 = "(1 000 результатов)",
        topCutout = 24.dp,
        filterBar = { Text("Фильтры", color = Color.White) },
    )
}
