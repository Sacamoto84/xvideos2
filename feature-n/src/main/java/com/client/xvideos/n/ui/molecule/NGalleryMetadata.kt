package com.client.xvideos.n.ui.molecule

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.RelativeDateTimeUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.atom.NGalleryTagChip
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tagLabels = mapOf(
    "parody" to "Пародии",
    "character" to "Персонажи",
    "tag" to "Теги",
    "artist" to "Художники",
    "group" to "Группы",
    "language" to "Языки",
    "category" to "Категории"
)

private val uploadDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
private val uploadDateLocale = Locale.forLanguageTag("ru")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NGalleryMetadata(gallery: NGallery, onTagClick: (NTag) -> Unit) {
    val tagsByType = remember(gallery.tags) { gallery.tags.groupBy { it.type.lowercase() } }
    val otherTags = remember(tagsByType) { tagsByType.filterKeys { it !in tagLabels }.values.flatten() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((type, label) in tagLabels) {
            GalleryTagRow(label, tagsByType[type].orEmpty(), onTagClick)
        }
        GalleryTagRow("Другие теги", otherTags, onTagClick)
        GalleryMetadataRow("Страниц") {
            Text(
                text = gallery.numPages.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.Text.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .background(Theme.tabLevel3, RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        GalleryUploadDate(gallery.uploadDate)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryTagRow(label: String, tags: List<NTag>, onTagClick: (NTag) -> Unit) {
    if (tags.isEmpty()) return
    GalleryMetadataRow(label) {
        for (tag in tags) {
            NGalleryTagChip(tag = tag, onClick = onTagClick, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryMetadataRow(label: String, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.Text.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryUploadDate(uploadDate: Long) {
    if (uploadDate <= 0L) return
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val date = remember(uploadDate, zone) {
        runCatching { Instant.ofEpochSecond(uploadDate).atZone(zone).toLocalDate() }.getOrNull()
    } ?: return
    val displayDate = remember(date, today) {
        val absoluteDate = date.format(uploadDateFormat)
        val relativeDate = relativeUploadDate(date, today)
        buildAnnotatedString {
            if (relativeDate == null) {
                append(absoluteDate)
            } else {
                append(relativeDate)
                withStyle(SpanStyle(color = Theme.Text.secondary)) {
                    append(" ($absoluteDate)")
                }
            }
        }
    }
    GalleryMetadataRow("Загружено") {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.bodyMedium,
            color = Theme.Text.primary,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

private fun relativeUploadDate(date: LocalDate, today: LocalDate): String? {
    if (date > today) return null
    val elapsed = Period.between(date, today)
    val (amount, unit) = when {
        elapsed.years > 0 -> elapsed.years to RelativeDateTimeUnit.YEAR
        elapsed.months > 0 -> elapsed.months to RelativeDateTimeUnit.MONTH
        else -> elapsed.days to RelativeDateTimeUnit.DAY
    }
    return RelativeDateTimeFormatter.getInstance(uploadDateLocale).format(-amount.toDouble(), unit)
}
