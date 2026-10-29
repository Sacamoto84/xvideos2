package com.client.xvideos.xlive.ui.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordStatus
import com.client.xvideos.xlive.model.recordDateSizeLabel
import com.client.xvideos.xlive.model.recordingElapsedLabel
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveLiveBadge
import com.client.xvideos.xlive.ui.saved.atom.XLiveRecordStatusBadge
import java.io.File

private const val THUMBNAIL_ASPECT_RATIO = 16f / 9f
private val CardShape = RoundedCornerShape(8.dp)

/**
 * Карточка записи — вид карточки «Моих»: кадр начала записи (или снимок
 * комнаты, пока кадра нет), на нём длительность или состояние; ник; дата и
 * время начала и размер.
 *
 * @param snapshot файл картинки; `null` — картинки нет, на её месте фон.
 * @param onLongClick долгое нажатие — удалить запись.
 * @param online автор сейчас в эфире: красная плашка, как в «Моих». Не в эфире
 *   и не проверено — без плашки и без затенения.
 * @param onSaveToGallery кнопка «в галерею» рядом с ником; `null` — кнопки нет.
 */
@Composable
fun XLiveRecordCard(
    record: XLiveRecord,
    snapshot: File?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    online: Boolean = false,
    onSaveToGallery: (() -> Unit)? = null,
) {
    val badge = when (record.status) {
        XLiveRecordStatus.READY -> recordingElapsedLabel(record.durationMs)
        XLiveRecordStatus.RECORDING, XLiveRecordStatus.FINISHING -> "сохраняется"
        XLiveRecordStatus.BROKEN -> "не завершена"
    }
    val subtitle = remember(record.id, record.sizeBytes) { recordDateSizeLabel(record.id, record.sizeBytes) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(CardShape)
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF26262C), CardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .background(Color(0xFF1F1F24))
        ) {
            if (snapshot != null) {
                // Абсолютный путь, а не File.toURI(): та даёт «file:/…» с одной
                // чертой, UrlImage принимает её за относительное имя и не находит файл.
                UrlImage(
                    url = snapshot.absolutePath,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            XLiveRecordStatusBadge(
                text = badge,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
            )
            // Длительность — справа снизу, поэтому «в эфире» — слева сверху.
            if (online) {
                XLiveLiveBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = record.username,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = XLiveColors.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onSaveToGallery != null) {
                IconButton(onClick = onSaveToGallery) {
                    Icon(
                        imageVector = Icons.Filled.SaveAlt,
                        contentDescription = "Сохранить в галерею",
                        tint = XLiveColors.Label,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRecordCardPreview() {
    XLiveRecordCard(
        record = XLiveRecord(
            id = 1_793_043_600_000L,
            username = "room_a",
            status = XLiveRecordStatus.READY,
            durationMs = 4_325_000L,
            sizeBytes = 2_254_857_830L,
        ),
        snapshot = null,
        onClick = {},
        onLongClick = {},
        online = true,
        onSaveToGallery = {},
    )
}
