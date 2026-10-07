package com.client.xvideos.common.theme

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Текст окна удаления коллекции, общий для разделов R и L: имя коллекции — жирным.
 *
 * Раньше оба раздела писали «Удалить «имя» из коллекции». Так читается удаление
 * одной записи, а удаляется коллекция целиком.
 */
fun collectionDeleteBody(name: String): AnnotatedString = buildAnnotatedString {
    append("Коллекция «")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
    append("» будет удалена вместе со всем содержимым")
}
