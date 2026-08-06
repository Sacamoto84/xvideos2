package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.client.xvideos.common.theme.LavenderDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.saved.LSmartCollectionCandidate
import com.composeunstyled.Text

@Composable
internal fun LSmartCollectionsDialog(
    candidates: List<LSmartCollectionCandidate>,
    onDismiss: () -> Unit,
    onCreate: (LSmartCollectionCandidate) -> Unit
) {
    LavenderDialog(
        title = "Smart collections",
        onDismiss = onDismiss,
        content = {
            if (candidates.isEmpty()) {
                Text(
                    "Пока мало метаданных для авто-коллекций. Добавь несколько элементов из альбомов, где есть теги, авторы или общий album id.",
                    color = Theme.L.grey2,
                    style = Theme.L.Type.body
                )
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(candidates) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCreate(candidate) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Theme.L.primaryColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(candidate.count.toString(), color = Theme.L.primaryColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.title, color = Color.Black, style = Theme.L.Type.rowTitle)
                                Text(candidate.subtitle, color = Theme.L.grey2, style = Theme.L.Type.rowSubtitle)
                            }
                        }
                    }
                }
            }
        },
        dismissText = "Закрыть",
    )
}


