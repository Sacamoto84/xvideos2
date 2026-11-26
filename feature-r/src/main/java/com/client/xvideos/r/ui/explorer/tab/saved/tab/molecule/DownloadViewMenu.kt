package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.DownloadMenuOption
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.DownloadView

private val VIEW_OPTIONS = listOf(
    DownloadMenuOption(DownloadView.DETAILS, "Подробный список", Icons.Outlined.ViewAgenda),
    DownloadMenuOption(DownloadView.GRID, "Сетка", Icons.Outlined.GridView),
)

@Composable
internal fun DownloadViewMenu(view: DownloadView, onViewChange: (DownloadView) -> Unit) {
    DownloadOptionsMenu(
        title = "Вид загрузок",
        icon = VIEW_OPTIONS.first { it.value == view }.icon,
        options = VIEW_OPTIONS,
        selected = view,
        onSelect = onViewChange,
    )
}

@Preview
@Composable
private fun DownloadViewMenuPreview() {
    DownloadViewMenu(view = DownloadView.DETAILS, onViewChange = {})
}
