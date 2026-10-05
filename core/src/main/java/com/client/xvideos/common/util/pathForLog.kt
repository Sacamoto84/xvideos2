package com.client.xvideos.common.util

/**
 * Путь адреса без схемы, хоста и строки запроса — для журнала.
 *
 * Адрес целиком в журнал не пишется: в нём имя сайта. По пути страницу всё ещё
 * можно опознать. Первый сегмент строки без схемы считается хостом, поэтому у
 * строки без `/` путь пустой.
 */
fun String.pathForLog(): String =
    substringBefore('?').substringBefore('#')
        .substringAfter("://").removePrefix("//")
        .substringAfter('/', missingDelimiterValue = "")

/** Имя файла из адреса или локального пути — для журнала, см. [pathForLog]. */
fun String.fileNameForLog(): String = pathForLog().trimEnd('/').substringAfterLast('/')
