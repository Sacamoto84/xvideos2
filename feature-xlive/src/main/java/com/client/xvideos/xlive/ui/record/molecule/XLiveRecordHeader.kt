package com.client.xvideos.xlive.ui.record.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordRoom
import com.client.xvideos.xlive.model.recordBadgeLabel
import com.client.xvideos.xlive.model.recordingElapsedLabel
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.record.atom.XLiveOpenRoomButton
import com.client.xvideos.xlive.ui.record.atom.XLiveRecordBadge
import com.client.xvideos.xlive.ui.record.atom.XLiveSaveToGalleryButton
import com.client.xvideos.xlive.ui.room.atom.XLiveAvatar
import com.client.xvideos.xlive.ui.room.atom.XLiveSaveButton
import com.client.xvideos.xlive.ui.room.atom.XLiveStatPill

/**
 * Шапка записи — раскладка шапки комнаты, но всё на момент записи: серая
 * плашка «Запись · дата» вместо «В эфире», справа кнопки действий («в галерею»,
 * «в мои» и «открыть комнату»), под ними длительность, размер, зрители, рейтинг, подписчики.
 *
 * @param isSaved комната записи в «Моих».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XLiveRecordHeader(
    record: XLiveRecord,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    onOpenRoom: () -> Unit,
    onSaveToGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val realName = record.bio?.realName.orEmpty()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            XLiveAvatar(name = record.username)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (realName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = realName,
                        fontSize = 12.sp,
                        color = XLiveColors.Subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                XLiveRecordBadge(text = recordBadgeLabel(record.id))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                XLiveSaveToGalleryButton(onClick = onSaveToGallery)
                XLiveSaveButton(saved = isSaved, onClick = onToggleSaved)
                XLiveOpenRoomButton(onClick = onOpenRoom)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            XLiveStatPill(Icons.Outlined.Schedule, "Длительность", recordingElapsedLabel(record.durationMs))
            XLiveStatPill(Icons.Outlined.SdStorage, "Размер", formatBytes(record.sizeBytes))
            XLiveStatPill(Icons.Outlined.Visibility, "Зрители", record.room.viewers.toLong().toPrettyCount())
            record.room.ratingPercent?.let { XLiveStatPill(Icons.Outlined.ThumbUp, "Рейтинг", "$it%") }
            record.bio?.followers?.let {
                XLiveStatPill(Icons.Outlined.Group, "Подписчики", it.toLong().toPrettyCount())
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRecordHeaderPreview() {
    XLiveRecordHeader(
        record = XLiveRecord(
            id = 1_793_043_600_000L,
            username = "room_a",
            durationMs = 4_325_000L,
            sizeBytes = 2_254_857_830L,
            room = XLiveRecordRoom(viewers = 562, ratingPercent = 97),
            bio = XLiveBio(realName = "Anna", followers = 20_332),
        ),
        isSaved = true,
        onToggleSaved = {},
        onOpenRoom = {},
        onSaveToGallery = {},
    )
}
