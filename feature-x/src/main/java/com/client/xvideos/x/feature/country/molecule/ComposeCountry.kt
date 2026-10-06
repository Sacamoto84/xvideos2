package com.client.xvideos.x.feature.country.molecule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.launchCatching
import com.client.xvideos.ui.theme.grayColor
import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.feature.country.atom.CountryRowItem
import com.client.xvideos.x.feature.country.model.Country
import com.client.xvideos.x.feature.country.model.countries
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.composables.core.Menu
import com.composables.core.MenuButton
import com.composables.core.MenuContent
import com.composables.core.rememberMenuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Suppress("DEPRECATION")
@Composable
fun ComposeCountry(modifier: Modifier = Modifier) {
    val state = rememberMenuState(expanded = false)
    val stateLazyList = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val onCountryClick: (Country) -> Unit = remember(scope, state) {
        { item ->
            state.expanded = false
            scope.launchCatching(
                message = "Смена страны не удалась: ${item.name}",
                onError = { SnackBar.error("Не удалось сменить страну") },
            ) {
                val htmlContent = readHtmlFromURLWebView(normalizeXUrl(item.url))
                // Пустой ответ — страница не загрузилась, страну сайт не сменил.
                if (htmlContent.isBlank()) throw IOException("Страница смены страны не загрузилась")
                val flag = parseSiteCountryFlag(htmlContent) ?: item.flagEmoji

                withContext(Dispatchers.Main) {
                    CountryState.onCountrySelected(flag)
                    Toast.makeText(
                        AppContextHolder.applicationContext,
                        "${item.flagEmoji} ${item.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Box(modifier.size(48.dp)) {
        Menu(modifier = Modifier, state = state) {
            MenuButton(Modifier.fillMaxSize().background(Color(0xFF151515))) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText(
                        CountryState.current,
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    )
                }
            }

            MenuContent(
                modifier = Modifier
                    .padding(bottom = 0.dp)
                    .width(312.dp)
                    .alpha(0.9f)
                    .background(grayColor(0x35)),
            ) {
                LazyColumn(state = stateLazyList) {
                    items(
                        items = countries,
                        key = { it.url },
                        contentType = { "country_item" }
                    ) { item ->
                        CountryRowItem(
                            item = item,
                            isSelected = item.flagEmoji == CountryState.current,
                            onClick = onCountryClick
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ComposeCountryPreview() {
    ComposeCountry()
}
