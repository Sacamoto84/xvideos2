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
    val name = raw.replace('\\', '/').trim('/')
    require(name.isNotBlank()) { "Пустое имя пути" }
    require(!name.contains(':')) { "Небезопасный путь: $raw" }
    val parts = name.split('/').filter { it.isNotBlank() }
    require(parts.none { it == "." || it == ".." }) { "Небезопасный путь: $raw" }
    return parts.joinToString("/")
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
