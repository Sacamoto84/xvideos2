package com.client.xvideos.common.io

import java.io.File

/*
 * Проверки пути, пришедшего из недоверенного источника: zip-архива, манифеста
 * чужого устройства, файла бэкапа. Раньше жили тремя копиями — в ZipUtils, в
 * XlrBackupManager и (не жили вовсе) в P2pBundleInstaller. Разошедшиеся копии
 * одной проверки безопасности — это ровно тот случай, когда одну из них
 * забывают: P2pBundleInstaller и забыли.
 */

/**
 * Приводит относительный путь к каноничному виду `a/b/c` и отвергает всё, чем
 * можно выйти за корень распаковки.
 *
 * Отвергается: пустое имя, `..` и `.` в любом сегменте, двоеточие (диск в
 * windows-путях и ADS в NTFS).
 *
 * Ведущий слеш не отвергается, а срезается: `/a/b` становится `a/b` и остаётся
 * внутри корня. Отказ здесь ломал бы распаковку zip от архиваторов, которые
 * пишут имена с ведущим слешем, а безопасности не добавляет — итог всё равно
 * относительный.
 *
 * @throws IllegalArgumentException если путь небезопасен.
 */
fun normalizeRelativePath(raw: String): String {
    require(raw.isNotBlank()) { "Пустое имя пути" }
    val name = raw.replace('\\', '/').trim('/')
    require(name.isNotBlank()) { "Пустое имя пути" }
    require(!name.contains(':') && !name.contains('\u0000') && name.none { it < ' ' }) { "Небезопасный путь: $raw" }

    val sb = StringBuilder(name.length)
    var start = 0
    val len = name.length
    while (start < len) {
        var end = name.indexOf('/', start)
        if (end == -1) end = len
        if (end > start) {
            require(!isDotSegment(name, start, end)) { "Небезопасный путь: $raw" }
            if (sb.isNotEmpty()) {
                sb.append('/')
            }
            sb.append(name, start, end)
        }
        start = end + 1
    }
    require(sb.isNotEmpty()) { "Пустое имя пути" }
    return sb.toString()
}

private fun isDotSegment(name: String, start: Int, end: Int): Boolean {
    val len = end - start
    if (len == 1) return name[start] == '.'
    if (len == 2) return name[start] == '.' && name[start + 1] == '.'
    return false
}

/**
 * Проверяет, что [target] лежит внутри [root] (или совпадает с ним).
 *
 * Сравниваются канонические пути: без этого символическая ссылка внутри
 * корня уводила бы запись наружу. Разделитель в конце префикса обязателен —
 * иначе `/data/xvideos_backup` считался бы лежащим внутри `/data/xvideos`.
 *
 * @throws IllegalArgumentException если цель выходит за корень.
 */
fun requireInside(root: File, target: File) {
    val rootPath = root.canonicalPath
    val targetPath = target.canonicalPath
    require(targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)) {
        "Путь выходит за пределы корня: $targetPath"
    }
}

/**
 * Проверяет имя элемента хранилища — то, что попадает в `File(dir, "$name.$ext")`.
 *
 * Имя элемента становится частью пути на диске точно так же, как имя коллекции
 * (см. `CollectionName`), но раньше проверялось только второе: в `FileDB` и в
 * `CollectionDB.insert` имя шло в File как есть. Источник имени — id и username
 * из сетевых моделей, канал HTTPS, так что это защита в глубину; ровно на такой
 * асимметрии «одну половину контракта проверили, вторую забыли» и держалась
 * дыра, закрытая нормализацией путей выше.
 *
 * Требования слабее, чем у имени коллекции: элемент не становится папкой, поэтому
 * ведущая точка и двоеточие допустимы. Запрещено только то, чем запись уходит из
 * своего каталога: разделители, `..`, null-байты и управляющие символы.
 */
fun isUnsafeItemName(name: String): Boolean =
    name.isBlank() || name == "." || name == ".." ||
        name.contains('/') || name.contains('\\') || name.contains('\u0000') ||
        name.any { it < ' ' }

/**
 * Проверяет, является ли относительный путь безопасным для использования.
 */
fun isSafeRelativePath(raw: String): Boolean =
    runCatching { normalizeRelativePath(raw) }.isSuccess

/**
 * Нормализует относительный путь либо возвращает null, если путь небезопасен.
 */
fun normalizeRelativePathOrNull(raw: String): String? =
    runCatching { normalizeRelativePath(raw) }.getOrNull()

/**
 * Проверяет, является ли имя элемента безопасным для файлового хранилища.
 */
fun isSafeItemName(name: String): Boolean = !isUnsafeItemName(name)

