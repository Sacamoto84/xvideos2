package com.client.xvideos.x.screens.search.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.screens.search.SearchUiMode

/**
 * Верхняя строка поиска X с кнопками действий и индикатором загрузки.
 */
@Composable
fun SearchTopBar(
    query: String,
    uiMode: SearchUiMode,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .background(Color(0xFF222222))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск видео, моделей или каналов...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = {
                if (uiMode == SearchUiMode.RESULTS) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад к подсказкам",
                            tint = Color(0xFFFF9900)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = Color.Gray
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2B2B2B),
                unfocusedContainerColor = Color(0xFF282828),
                focusedBorderColor = Color(0xFFFF9900),
                unfocusedBorderColor = Color(0xFF383838),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFF9900)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(top = 4.dp),
                color = Color(0xFFFF9900),
                trackColor = Color(0xFF333333)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Preview
@Composable
private fun SearchTopBarPreview() {
    SearchTopBar(
        query = "test",
        uiMode = SearchUiMode.SUGGESTIONS,
        isLoading = false,
        onQueryChange = {},
        onSearch = {},
        onClear = {},
        onBack = {}
    )
}
