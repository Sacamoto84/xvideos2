package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.n.model.NAlbumFilter
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagCondition
import com.client.xvideos.n.model.NTagPage
import com.client.xvideos.n.model.NTagSort
import com.client.xvideos.n.model.NTagType
import com.client.xvideos.n.ui.model.hasValidPageRange

@Composable
fun NAlbumFilterDialog(
    initialFilter: NAlbumFilter,
    onDismiss: () -> Unit,
    onApply: (NAlbumFilter) -> Unit,
    loadTagPage: suspend (NTagType, NTagSort, Int) -> Result<NTagPage>,
    findTags: suspend (String, NTagType) -> Result<List<NTag>>,
) {
    var draft by rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var showTags by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val close = { if (showTags) showTags = false else onDismiss() }

    Dialog(onDismissRequest = close) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(shape)
                .border(1.dp, Color(0xFF3E3E3E), shape)
                .background(Color(0xFF1E1E1E))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showTags) "Каталог тегов" else "Фильтры поиска",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = close) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (showTags) "Вернуться к фильтрам" else "Закрыть",
                        tint = Color.LightGray
                    )
                }
            }

            if (showTags) {
                NTagCatalog(
                    selectedTags = draft.tags,
                    onTagChange = { tag, excluded ->
                        val otherTags = draft.tags.filterNot { it.tag.id == tag.id }
                        draft = draft.copy(tags = if (excluded == null) otherTags else otherTags + NTagCondition(tag, excluded))
                    },
                    loadPage = loadTagPage,
                    findTags = findTags,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("К фильтрам") }
            } else {
                NAlbumFilterFields(
                    draft = draft,
                    onChange = { draft = it },
                    onOpenTags = { showTags = true },
                    modifier = Modifier.weight(1f).verticalScroll(scroll)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { draft = NAlbumFilter.DEFAULT },
                        modifier = Modifier.weight(1f)
                    ) { Text("Сбросить") }
                    Button(
                        onClick = { onApply(draft) },
                        enabled = draft.hasValidPageRange,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED2553))
                    ) { Text("Применить") }
                }
            }
        }
    }
}

@Preview(name = "Фильтры альбомов", widthDp = 390, heightDp = 760)
@Composable
private fun NAlbumFilterDialogPreview() {
    NAlbumFilterDialog(
        initialFilter = NAlbumFilter(
            language = "english",
            tags = listOf(NTagCondition(NTag(id = 1, type = "tag", name = "full color"), excluded = false)),
            minPages = 12,
            maxPages = 48
        ),
        onDismiss = {},
        onApply = {},
        loadTagPage = { _, _, _ -> Result.success(NTagPage(emptyList(), totalPages = 1)) },
        findTags = { _, _ -> Result.success(emptyList()) }
    )
}
