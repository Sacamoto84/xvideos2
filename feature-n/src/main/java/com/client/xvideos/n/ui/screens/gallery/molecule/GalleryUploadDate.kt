package com.client.xvideos.n.ui.screens.gallery.molecule

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.RelativeDateTimeUnit
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val uploadDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
private val uploadDateLocale = Locale.forLanguageTag("ru")

@Composable
internal fun GalleryUploadDate(uploadDate: Long) {
    if (uploadDate <= 0L) return
    val displayDate = remember(uploadDate) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val date = runCatching { Instant.ofEpochSecond(uploadDate).atZone(zone).toLocalDate() }.getOrNull()
            ?: return@remember null
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
    } ?: return
    GalleryMetadataRow("Загружено") {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.bodySmall,
            color = Theme.Text.primary,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 2.dp)
        )
    }
}

private fun relativeUploadDate(date: LocalDate, today: LocalDate): String? {
    if (date > today) return null
    val elapsed = Period.between(date, today)
    val formatter = RelativeDateTimeFormatter.getInstance(uploadDateLocale)
    val (amount, unit) = when {
        elapsed.years > 0 -> elapsed.years to RelativeDateTimeUnit.YEAR
        elapsed.months > 0 -> elapsed.months to RelativeDateTimeUnit.MONTH
        elapsed.days > 0 -> elapsed.days to RelativeDateTimeUnit.DAY
        else -> return formatter.format(
            RelativeDateTimeFormatter.Direction.THIS,
            RelativeDateTimeFormatter.AbsoluteUnit.DAY
        )
    }
    return formatter.format(-amount.toDouble(), unit)
}

@Preview
@Composable
private fun GalleryUploadDatePreview() {
    GalleryUploadDate(uploadDate = 1_579_132_800L)
}
