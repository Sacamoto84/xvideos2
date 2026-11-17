package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

/**
 * Верхняя панель при просмотре содержимого отдельной коллекции.
 */
@Composable
fun NCollectionTopBar(
    collectionName: String,
    itemsCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRenameCollection: (() -> Unit)? = null,
    onDeleteCollection: (() -> Unit)? = null,
    topInset: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel1)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collectionName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemsCount элементов",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }

            if (onRenameCollection != null) {
                IconButton(onClick = onRenameCollection) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Переименовать коллекцию",
                        tint = Color.White
                    )
                }
            }

            if (onDeleteCollection != null) {
                IconButton(onClick = onDeleteCollection) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить коллекцию",
                        tint = Color(0xFFED2553)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NCollectionTopBarPreview() {
    NCollectionTopBar(
        collectionName = "Моя коллекция",
        itemsCount = 12,
        onBack = {},
        onRenameCollection = {},
        onDeleteCollection = {}
    )
}
