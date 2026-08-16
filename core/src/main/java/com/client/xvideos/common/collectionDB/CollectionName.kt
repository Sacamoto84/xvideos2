package com.client.xvideos.common.collectionDB

/**
 * Имя коллекции — это имя папки на диске, поэтому проверять его обязательно.
 *
 * Раньше не проверялось нигде: UI отсекал только пустую строку, а дальше имя
 * шло прямо в `File(root, name)`. Имя `..` уводило `deleteRecursively()` на
 * родительский каталог, то есть на весь раздел.
 *
 * Проверка отвергает, а не «чинит» имя: коллекция видна пользователю в списке,
 * и тихая подмена `a/b` на `a_b` расходится с тем, что он ввёл, а для
 * переименования существующей коллекции ещё и промахнётся мимо папки.
 */
object CollectionName {

    /**
     * Ведущая точка запрещена: такие папки скрыты в списках, и с них же
     * начинается служебный префикс `.xlr_old_` отодвинутых копий в
     * [com.client.xvideos.common.backup.XlrBackupManager] — коллекция с таким
     * именем была бы стёрта восстановлением бэкапа.
     */
    private const val HIDDEN_PREFIX = '.'

    /**
     * Возвращает имя, обрезанное по краям, или `null`, если оно непригодно как
     * имя папки.
     */
    fun normalizeOrNull(raw: String): String? {
        val name = raw.trim()
        if (name.isBlank()) return null
        if (name.startsWith(HIDDEN_PREFIX)) return null
        if (name.any { it == '/' || it == '\\' || it == ':' }) return null
        return name
    }

    fun isValid(raw: String): Boolean = normalizeOrNull(raw) != null
}
