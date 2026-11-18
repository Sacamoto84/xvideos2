package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
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
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.ui.atom.NFilterChip

private val LANGUAGES = listOf(
    "" to "Все языки",
    "english" to "English",
    "russian" to "Русский",
    "japanese" to "日本語"
)

private val SORTS = listOf(
    NSortType.RECENT to "Свежее",
    NSortType.POPULAR_ALL_TIME to "Популярное (всё время)",
    NSortType.POPULAR_WEEK to "Популярное за неделю",
    NSortType.POPULAR_TODAY to "Популярное за день"
)

/**
 * Диалог фильтрации каталога манги по языку и порядку сортировки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NAlbumFilterDialog(
    initialFilter: NAlbumFilter,
    onDismiss: () -> Unit,
    onApply: (NAlbumFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(initialFilter.language) }
    var selectedSort by remember { mutableStateOf(initialFilter.sort) }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = modifier
                .clip(shape)
                .border(1.dp, Color(0xFF3E3E3E), shape)
                .background(Color(0xFF1E1E1E))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры каталога",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color(0xFFAAAAAA)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Язык перевода",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LANGUAGES.forEach { (code, label) ->
                    NFilterChip(
                        label = label,
                        isSelected = selectedLanguage == code,
                        onClick = { selectedLanguage = code }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Сортировка",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SORTS.forEach { (sort, label) ->
                    NFilterChip(
                        label = label,
                        isSelected = selectedSort == sort,
                        onClick = { selectedSort = sort }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedLanguage = ""
                        selectedSort = NSortType.RECENT
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA))
                ) {
                    Text("Сбросить")
                }
                Button(
                    onClick = {
                        onApply(NAlbumFilter(language = selectedLanguage, sort = selectedSort))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED2553))
                ) {
                    Text("Применить", color = Color.White)
                }
            }
        }
    }
}

@Preview
@Composable
private fun NAlbumFilterDialogPreview() {
    NAlbumFilterDialog(
        initialFilter = NAlbumFilter(language = "english", sort = NSortType.POPULAR_ALL_TIME),
        onDismiss = {},
        onApply = {}
    )
}
