package com.client.xvideos.common.backup

import androidx.compose.runtime.Immutable

/**
 * Итоговый отчет о размере бэкапа или отдельной его ветки.
 *
 * @property files Общее количество файлов.
 * @property bytes Суммарный размер в байтах.
 */
@Immutable
data class XlrBackupReport(
    val files: Int = 0,
    val bytes: Long = 0L
) {
    /** Истина, если отчет не содержит файлов (0 файлов и 0 байт). */
    val isEmpty: Boolean get() = files == 0 && bytes == 0L

    /** Истина, если отчет содержит хотя бы один файл. */
    val isNotEmpty: Boolean get() = !isEmpty

    companion object {
        /** Пустой отчет по умолчанию. */
        val EMPTY = XlrBackupReport(0, 0L)
    }
}

/**
 * Описание элемента дерева архива бэкапа для отображения в UI и калькуляции объема.
 *
 * @property path Относительный путь внутри архива (например, "L/Likes").
 * @property title Отображаемое имя категории или раздела.
 * @property section Идентификатор корневой секции ("X", "L" или "R").
 * @property parentPath Относительный путь родительского элемента (null для корневых секций).
 * @property files Количество файлов внутри элемента.
 * @property bytes Размер данных элемента в байтах.
 */
@Immutable
data class XlrBackupItem(
    val path: String,
    val title: String,
    val section: String,
    val parentPath: String? = null,
    val files: Int = 0,
    val bytes: Long = 0L
) {
    /** Проверяет валидность элемента (непустой путь и название). */
    val isValid: Boolean get() = path.isNotBlank() && title.isNotBlank()

    /** Истина, если элемент пуст. */
    val isEmpty: Boolean get() = files == 0 && bytes == 0L

    /** Истина, если элемент содержит данные. */
    val isNotEmpty: Boolean get() = !isEmpty

    /** Истина, если элемент имеет родительский раздел в иерархии. */
    val hasParent: Boolean get() = !parentPath.isNullOrBlank()
}

/**
 * Режим детализации содержимого при создании бэкапа для секций с тяжелым медиа.
 */
enum class XlrBackupContentMode {
    /** Полный бэкап, включая исходные медиафайлы (видео, полноразмерные изображения). */
    FULL,

    /** Облегченный бэкап: только метаданные, списки избранного, закладки и превью. */
    MINI;

    /** Истина, если выбран полный режим. */
    val isFull: Boolean get() = this == FULL

    /** Истина, если выбран мини-режим. */
    val isMini: Boolean get() = this == MINI

    companion object {
        /** Режим по умолчанию (MINI). */
        val DEFAULT = MINI
    }
}

/**
 * Настройки экспорта бэкапа для различных разделов приложения.
 *
 * @property lMode Режим экспорта для раздела L (Luscious).
 * @property rMode Режим экспорта для раздела R (RedGifs).
 */
@Immutable
data class XlrBackupOptions(
    val lMode: XlrBackupContentMode = XlrBackupContentMode.MINI,
    val rMode: XlrBackupContentMode = XlrBackupContentMode.MINI
) {
    /** Истина, если все разделы экспортируются в полном объеме (FULL). */
    val isFullBackup: Boolean get() = lMode == XlrBackupContentMode.FULL && rMode == XlrBackupContentMode.FULL

    /** Истина, если все разделы экспортируются в минимальном объеме (MINI). */
    val isMiniBackup: Boolean get() = lMode == XlrBackupContentMode.MINI && rMode == XlrBackupContentMode.MINI

    /** Истина, если хотя бы один раздел экспортируется в режиме FULL. */
    val hasFullContent: Boolean get() = lMode.isFull || rMode.isFull

    /** Истина, если хотя бы один раздел экспортируется в режиме MINI. */
    val hasMiniContent: Boolean get() = lMode.isMini || rMode.isMini

    companion object {
        /** Настройки по умолчанию (все в MINI). */
        val DEFAULT = XlrBackupOptions()

        /** Полный бэкап всех разделов. */
        val FULL = XlrBackupOptions(XlrBackupContentMode.FULL, XlrBackupContentMode.FULL)

        /** Мини-бэкап всех разделов. */
        val MINI = XlrBackupOptions(XlrBackupContentMode.MINI, XlrBackupContentMode.MINI)
    }
}
