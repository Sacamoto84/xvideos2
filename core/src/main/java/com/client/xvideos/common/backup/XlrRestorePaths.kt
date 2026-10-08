package com.client.xvideos.common.backup

/**
 * Какие пути восстановление действительно трогает.
 *
 * Архив собирают и из части папок раздела — например, X без загрузок. В дереве
 * восстановления у такого архива отмечен раздел целиком, и раньше он целиком и
 * заменялся: папки, которых в архиве нет, стирались вместе с прежней копией.
 * Правило: папка, о которой архив ничего не знает, не трогается.
 */
internal object XlrRestorePaths {

    /**
     * Заменяет путь раздела (`X`) папками этого раздела, которые есть в архиве.
     * Пути глубже раздела остаются как есть. Раздел, от которого в архиве нет
     * ни одной папки, из списка выпадает.
     *
     * @param paths Выбранные пути после [XlrBackupManager.normalizeSelectedPaths].
     * @param archiveFolders Пути второго уровня из архива: `X/Favorites`, `L/Likes`.
     */
    fun expandSections(paths: List<String>, archiveFolders: Set<String>): List<String> =
        paths.flatMap { path ->
            if ('/' in path) listOf(path) else archiveFolders.filter { it.startsWith("$path/") }
        }.distinct().sorted()
}
