package com.client.xvideos.common.ui.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.core.HorizontalSeparator

/**
 * Шапка вкладки сохранённого X и R: «Избранное», «Сохранённое», подписки X;
 * «Лайки», «Авторы», коллекции R.
 *
 * Одна на все вкладки, чтобы они не расходились: заголовок у левого края
 * обычной толщины (как у «Истории»), разделитель на всю ширину окна — шапка не
 * ограничивается полосой 640dp и на раскрытом складном. [navigation] — слева
 * от заголовка (стрелка «назад» внутри коллекции), [trailing] — справа от
 * заголовка (число подписок).
 *
 * Высота ряда фиксирована ([SAVED_TAB_HEADER_HEIGHT]): кнопки справа (у «Истории»)
 * и шрифт не сдвигают разделитель — при переключении вкладок он не прыгает.
 */
@Composable
fun SavedTabHeader(
    title: String,
    modifier: Modifier = Modifier,
    topCutout: Dp = 0.dp,
    navigation: (@Composable RowScope.() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topCutout)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SAVED_TAB_HEADER_HEIGHT)
                // Кнопка «назад» сама даёт поле 12dp вокруг значка.
                .padding(start = if (navigation != null) 4.dp else 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigation?.invoke(this)
            Text(
                title,
                color = Color.White,
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
        HorizontalSeparator(color = Color(0xFF9E9E9E))
    }
}

/** Высота ряда шапки вкладок сохранённого — с ней же ряд «Истории». */
val SAVED_TAB_HEADER_HEIGHT = 48.dp

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SavedTabHeaderPreview() {
    SavedTabHeader(title = "Избранное")
}
