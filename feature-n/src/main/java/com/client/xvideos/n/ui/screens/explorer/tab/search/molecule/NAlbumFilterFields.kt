package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.model.NAlbumFilter
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NUploadPeriod
import com.client.xvideos.n.ui.model.hasValidPageRange
import com.client.xvideos.n.ui.model.nAlbumFilterLanguages

@Composable
internal fun NAlbumFilterFields(
    draft: NAlbumFilter,
    onChange: (NAlbumFilter) -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NFilterChoices("Язык перевода", nAlbumFilterLanguages, draft.language) { onChange(draft.copy(language = it)) }
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

@Preview
@Composable
private fun NAlbumFilterFieldsPreview() {
    NAlbumFilterFields(
        draft = NAlbumFilter.DEFAULT,
        onChange = {},
        onOpenTags = {},
    )
}
