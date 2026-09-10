package com.client.xvideos.common.io

import java.io.File
import java.io.IOException

/**
 * Пишет текст во временный файл рядом и переименовывает его поверх целевого.
 *
 * Переименование в пределах одной ФС атомарно, поэтому читатель видит либо
 * старое содержимое целиком, либо новое целиком — но не обрывок. Обрезанный
 * JSON в хранилищах приложения не диагностируется: он молча отбрасывается на
 * чтении, и элемент просто исчезает из списка.
 *
 * @throws IOException если записать файл не удалось.
 */
fun File.writeTextAtomically(text: String) {
    val temp = File(parentFile, "$name.tmp")
    temp.parentFile?.mkdirs()
    temp.writeText(text, Charsets.UTF_8)
    if (!temp.renameTo(this)) {
        // На некоторых ФС renameTo не перезаписывает существующий файл.
        delete()
        if (!temp.renameTo(this)) {
            temp.delete()
            throw IOException("Не удалось записать файл: $absolutePath")
        }
    }
}
