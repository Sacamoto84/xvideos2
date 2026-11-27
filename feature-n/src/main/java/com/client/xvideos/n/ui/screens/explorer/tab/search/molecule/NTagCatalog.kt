package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.excludedFromAutofill
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagCondition
import com.client.xvideos.n.model.NTagPage
import com.client.xvideos.n.model.NTagSort
import com.client.xvideos.n.model.NTagType
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.molecule.NRequestChallengeDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

private val tagTypes = NTagType.entries.map { it to it.title }
private val tagSorts = NTagSort.entries.map { it to it.title }

@Composable
internal fun NTagCatalog(
    selectedTags: List<NTagCondition>,
    onTagChange: (NTag, excluded: Boolean?) -> Unit,
    loadPage: suspend (NTagType, NTagSort, Int) -> Result<NTagPage>,
    findTags: suspend (String, NTagType) -> Result<List<NTag>>,
    modifier: Modifier = Modifier,
) {
    var type by rememberSaveable { mutableStateOf(NTagType.TAG) }
    var sort by rememberSaveable { mutableStateOf(NTagSort.POPULAR) }
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var retry by remember { mutableIntStateOf(0) }
    val term = query.trim()
    val browsing = term.isEmpty()
    val pageSort = sort.takeIf { browsing }
    var result by remember(type, pageSort, term, page, retry) { mutableStateOf<Result<NTagPage>?>(null) }
    var dismissedChallenge by remember { mutableStateOf<CloudflareBlockedException?>(null) }

    LaunchedEffect(type, pageSort, term, page, retry) {
        val response = if (pageSort != null) {
            loadPage(type, pageSort, page)
        } else {
            delay(350)
            findTags(term, type).map { NTagPage(it, totalPages = 1) }
        }
        ensureActive()
        result = response
    }

    val matchedPage = result?.getOrNull()
    val challenge = result?.exceptionOrNull() as? CloudflareBlockedException
    val tags = remember(matchedPage, browsing, sort) {
        val items = matchedPage?.tags.orEmpty()
        when {
            browsing -> items
            sort == NTagSort.NAME -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            else -> items.sortedByDescending { it.count }
        }
    }
    val selections = remember(selectedTags) { selectedTags.associateBy { it.tag.id } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                page = 1
            },
            modifier = Modifier.fillMaxWidth().excludedFromAutofill(),
            label = { Text("Начало названия") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NTagCatalogMenu("Тип тега", type.title, tagTypes, Modifier.weight(1f)) {
                if (type != it) page = 1
                type = it
            }
            NTagCatalogMenu("Сортировка", sort.title, tagSorts, Modifier.weight(1f)) {
                if (sort != it) page = 1
                sort = it
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                result == null -> CircularProgressIndicator()
                challenge != null -> NCloudflareBanner(onVerifyClick = { dismissedChallenge = null })
                matchedPage == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не удалось загрузить теги", color = Color.LightGray)
                    TextButton(onClick = { retry++ }) { Text("Повторить") }
                }
                tags.isEmpty() -> Text("Теги не найдены", color = Color.LightGray)
                else -> key(type, term, page, sort) {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(tags, key = { it.id }) { tag ->
                            NTagCatalogItem(tag, selections[tag.id]?.excluded, onTagChange)
                        }
                    }
                }
            }
        }
        if (browsing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { page-- }, enabled = page > 1) { Text("Назад") }
                val total = matchedPage?.totalPages?.let { " / $it" }.orEmpty()
                Text("$page$total", color = Color.LightGray)
                TextButton(onClick = { page++ }, enabled = page < (matchedPage?.totalPages ?: 0)) { Text("Далее") }
            }
        }
    }
    if (challenge != null && challenge !== dismissedChallenge) {
        NRequestChallengeDialog(
            challenge = challenge,
            onDismiss = { dismissedChallenge = challenge },
            onVerified = { retry++ }
        )
    }
}

@Preview(name = "Каталог тегов", widthDp = 360, heightDp = 440)
@Composable
private fun NTagCatalogPreview() {
    val tags = listOf(
        NTag(id = 1, type = "tag", name = "full color", count = 5300),
        NTag(id = 2, type = "tag", name = "monochrome", count = 2100),
        NTag(id = 3, type = "tag", name = "illustrations", count = 1250)
    )
    NTagCatalog(
        selectedTags = listOf(NTagCondition(tags[0], excluded = false), NTagCondition(tags[1], excluded = true)),
        onTagChange = { _, _ -> },
        loadPage = { _, _, _ -> Result.success(NTagPage(tags, totalPages = 2)) },
        findTags = { query, _ -> Result.success(tags.filter { it.name.startsWith(query, ignoreCase = true) }) },
        modifier = Modifier.fillMaxSize().padding(16.dp)
    )
}
