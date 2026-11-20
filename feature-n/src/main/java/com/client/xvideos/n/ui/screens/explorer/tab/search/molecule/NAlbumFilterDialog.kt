package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.common.ui.excludedFromAutofill
import com.client.xvideos.n.model.NAlbumFilter
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagCondition
import com.client.xvideos.n.model.NTagPage
import com.client.xvideos.n.model.NTagSort
import com.client.xvideos.n.model.NTagType
import com.client.xvideos.n.model.NUploadPeriod
import com.client.xvideos.n.ui.screens.explorer.tab.search.atom.NFilterChip

private val languages = listOf(
    "" to "Все языки",
    "english" to "English",
    "russian" to "Русский",
    "japanese" to "日本語"
)

@Composable
fun NAlbumFilterDialog(
    initialFilter: NAlbumFilter,
    onDismiss: () -> Unit,
    onApply: (NAlbumFilter) -> Unit,
    loadTagPage: suspend (NTagType, NTagSort, Int) -> Result<NTagPage>,
    findTags: suspend (String, NTagType) -> Result<List<NTag>>,
) {
    var draft by remember { mutableStateOf(initialFilter) }
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

@Composable
private fun NAlbumFilterFields(
    draft: NAlbumFilter,
    onChange: (NAlbumFilter) -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NFilterChoices("Язык перевода", languages, draft.language) { onChange(draft.copy(language = it)) }
        NFilterChoices("Сортировка", NSortType.entries.map { it to it.title }, draft.sort) {
            onChange(draft.copy(sort = it))
        }
        OutlinedButton(onClick = onOpenTags, modifier = Modifier.fillMaxWidth()) { Text("Добавить тег") }
        NTagConditionChips(draft.tags, onRemove = { onChange(draft.copy(tags = draft.tags - it)) })
        Text("Количество страниц", color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NFilterNumberField(
                draft.minPages, "От", { onChange(draft.copy(minPages = it)) },
                Modifier.weight(1f), isError = !draft.hasValidPageRange
            )
            NFilterNumberField(
                draft.maxPages, "До", { onChange(draft.copy(maxPages = it)) },
                Modifier.weight(1f), isError = !draft.hasValidPageRange
            )
        }
        if (!draft.hasValidPageRange) {
            Text("Укажите положительный диапазон. «От» должно быть не больше «До».", color = Color(0xFFFF8B9B))
        }
        NFilterNumberField(
            draft.minFavorites, "Добавлений в избранное от",
            { onChange(draft.copy(minFavorites = it)) }, Modifier.fillMaxWidth()
        )
        NFilterChoices("Загружено", NUploadPeriod.entries.map { it to it.title }, draft.uploadedWithin) {
            onChange(draft.copy(uploadedWithin = it))
        }
    }
}

private val NAlbumFilter.hasValidPageRange: Boolean
    get() = minPages != 0 && maxPages != 0 && (minPages ?: 1) <= (maxPages ?: Int.MAX_VALUE)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> NFilterChoices(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color.LightGray)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                NFilterChip(label, value == selected, onClick = { onSelect(value) })
            }
        }
    }
}

@Composable
private fun NFilterNumberField(
    value: Int?,
    label: String,
    onChange: (Int?) -> Unit,
    modifier: Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            if (text.isEmpty()) onChange(null)
            else text.toIntOrNull()?.takeIf { it >= 0 && text.all(Char::isDigit) }?.let(onChange)
        },
        modifier = modifier.excludedFromAutofill(),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
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
