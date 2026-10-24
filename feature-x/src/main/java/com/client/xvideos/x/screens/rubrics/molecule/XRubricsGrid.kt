package com.client.xvideos.x.screens.rubrics.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.x.model.XRubric
import com.client.xvideos.x.screens.rubrics.XRubricsState
import com.client.xvideos.x.screens.rubrics.atom.XRubricTile
import com.client.xvideos.x.screens.tags.atom.TagsStateMessage
import kotlinx.collections.immutable.persistentListOf

/**
 * Содержимое вкладки «Категории»: до загрузки — только индикатор по центру,
 * ошибка — текст и «Повторить», список — сетка плиток.
 */
@Composable
fun XRubricsGrid(
    state: XRubricsState,
    gridState: LazyGridState,
    topCutout: Dp,
    onOpen: (XRubric) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        XRubricsState.Loading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        XRubricsState.Failed -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TagsStateMessage(message = "Категории не загрузились", onRetry = onRetry)
        }

        is XRubricsState.Ready -> LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveGridColumns()),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = topCutout + 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.rubrics, key = { it.id }) { rubric ->
                XRubricTile(title = rubric.title, onClick = { onOpen(rubric) })
            }
        }
    }
}

@Preview
@Composable
private fun XRubricsGridPreview() {
    XRubricsGrid(
        state = XRubricsState.Ready(persistentListOf(XRubric(1, "A", "Категория А"), XRubric(2, "B", "Категория Б"))),
        gridState = rememberLazyGridState(),
        topCutout = 0.dp,
        onOpen = {},
        onRetry = {},
    )
}
