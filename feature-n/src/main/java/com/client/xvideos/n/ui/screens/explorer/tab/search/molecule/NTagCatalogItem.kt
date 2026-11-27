package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.screens.explorer.tab.search.atom.NFilterChip

@Composable
internal fun NTagCatalogItem(
    tag: NTag,
    excluded: Boolean?,
    onChange: (NTag, Boolean?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${tag.name} · ${tag.count}", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NFilterChip("Добавить", excluded == false, onClick = { onChange(tag, if (excluded == false) null else false) })
            NFilterChip("Исключить", excluded == true, onClick = { onChange(tag, if (excluded == true) null else true) })
        }
    }
}

@Preview
@Composable
private fun NTagCatalogItemPreview() {
    NTagCatalogItem(
        tag = NTag(id = 1, type = "tag", name = "full color", count = 5300),
        excluded = false,
        onChange = { _, _ -> },
    )
}
