package com.client.xvideos.x.screens.rubrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.model.XRubric
import com.client.xvideos.x.screens.rubrics.molecule.XRubricsGrid

/**
 * Вкладка «Категории» главного экрана X: список категорий сайта для
 * ориентации [category]. Корень вкладки, как у поиска: подписывается на
 * [vm] и отдаёт состояние молекуле.
 */
@Composable
fun ScreenXRubricsTabContent(
    vm: XRubricsSM,
    category: XCategory,
    onOpenRubric: (XRubric) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(category) { vm.show(category) }

    XRubricsGrid(
        state = vm.state,
        gridState = vm.gridPosition(category).rememberState(),
        topCutout = getTopInsetDp(),
        onOpen = onOpenRubric,
        onRetry = vm::retry,
        modifier = modifier,
    )
}
