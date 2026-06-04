package com.client.xvideos.common.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.client.xvideos.App
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * Файловая система поверх Storage Access Framework.
 *
 * Объект работает не с обычными `File`, а с `Uri` документов внутри папки,
 * выбранной через `WorkDirectory`. Это позволяет писать и читать файлы в
 * пользовательской директории даже на новых Android, где прямой доступ к
 * внешнему хранилищу ограничен scoped storage.
 */
object SafFileSystem {

    /**
     * Описание одного файла или папки внутри SAF-дерева.
     *
     * `uri` нужен для дальнейших операций через `DocumentsContract`, а `mimeType`
     * позволяет отличить директорию от обычного файла.
     */
    data class Entry(
        val name: String,
        val uri: Uri,
        val mimeType: String?,
        val size: Long
    ) {
        val isDirectory: Boolean
            get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    private val context
        get() = App.instance

    private val resolver: ContentResolver
        get() = context.contentResolver

    /**
     * Сериализует операции «найти-или-создать» ([ensureDirectory]/[ensureFile]).
     *
     * Без этого параллельные загрузки в одну папку могут одновременно пройти
     * проверку отсутствия и оба вызвать `createDocument`, из-за чего SAF создаёт
     * дубликат с именем вида «name (1)». Блокировка реентерабельная, поэтому
     * `ensureFile` спокойно вызывает `ensureDirectory` под тем же замком.
     */
    private val lock = Any()

    /**
     * Проверяет, доступна ли выбранная пользователем рабочая папка.
     */
    fun isAvailable(): Boolean = WorkDirectory.hasAccess(context)

    /**
     * Создаёт цепочку папок внутри рабочей директории и возвращает URI последней.
     *
     * Путь нормализуется через `normalizedSegments()`, затем каждый сегмент
     * ищется среди дочерних документов. Если папки нет, она создаётся через
     * `DocumentsContract.createDocument()`.
     */
    fun ensureDirectory(relativePath: String): Uri? = synchronized(lock) {
        val rootUri = rootDocumentUri() ?: return null
        var current: Uri = rootUri
        normalizedSegments(relativePath).forEach { segment ->
            current = findChild(current, segment, onlyDirectory = true)?.uri
                ?: DocumentsContract.createDocument(
                    resolver,
                    current,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    segment
                )
                ?: return null
        }
        current
    }

    /**
     * Возвращает список файлов и папок внутри относительного пути.
     *
     * Если путь не найден или рабочая директория недоступна, возвращается пустой
     * список, чтобы UI мог безопасно показать пустое состояние.
     */
    fun list(relativePath: String): List<Entry> {
        val directory = documentUri(relativePath) ?: return emptyList()
        return listChildren(directory)
    }

    /**
     * Проверяет существование файла или папки по относительному пути.
     */
    fun exists(relativePath: String): Boolean {
        return documentUri(relativePath) != null
    }

    /**
     * Возвращает размер файла в байтах.
     *
     * Для папок и отсутствующих путей возвращает `0L`, потому что SAF не всегда
     * отдаёт агрегированный размер директории.
     */
    fun length(relativePath: String): Long {
        return documentEntry(relativePath)?.size ?: 0L
    }

    /**
     * Возвращает строковое представление URI документа.
     *
     * Удобно для сохранения ссылки в локальную БД или передачи в плеер.
     */
    fun uriString(relativePath: String): String? {
        return documentUri(relativePath)?.toString()
    }

    /**
     * Читает UTF-8 текст из файла внутри рабочей директории.
     *
     * Если файл не найден, возвращает `null`; ошибки открытия потока пробрасывает
     * Android `ContentResolver`, чтобы вызывающий код видел реальную причину.
     */
    fun readText(relativePath: String): String? {
        return openInputStream(relativePath)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }

    /**
     * Записывает UTF-8 текст в файл, создавая недостающие папки и сам файл.
     *
     * По умолчанию используется JSON mime type, потому что метод в основном
     * нужен для сохранения метаданных и кэшей.
     */
    fun writeText(relativePath: String, text: String, mimeType: String = "application/json") {
        openOutputStream(relativePath, mimeType).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * Открывает поток чтения для файла по относительному пути.
     *
     * Возвращает `null`, если документ не существует.
     */
    fun openInputStream(relativePath: String): InputStream? {
        val uri = documentUri(relativePath) ?: return null
        return resolver.openInputStream(uri)
    }

    /**
     * Открывает поток записи для файла, предварительно создавая его при необходимости.
     *
     * Режим `rwt` перезаписывает содержимое и усечёт старый файл, поэтому метод
     * подходит для сохранения актуальной версии кэша или метаданных.
     */
    fun openOutputStream(relativePath: String, mimeType: String): OutputStream {
        val uri = ensureFile(relativePath, mimeType)
            ?: error("Cannot create SAF file: $relativePath")
        return resolver.openOutputStream(uri, "rwt")
            ?: error("Cannot open SAF output stream: $relativePath")
    }

    /**
     * Удаляет один файл или пустую папку по относительному пути.
     *
     * Для непустых папок используйте `deleteTree()`, потому что разные SAF
     * провайдеры могут запрещать удаление директории с дочерними документами.
     */
    fun delete(relativePath: String): Boolean {
        val uri = documentUri(relativePath) ?: return false
        return runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            .onFailure { Timber.w(it, "!!! SAF delete failed: $relativePath") }
            .getOrDefault(false)
    }

    /**
     * Рекурсивно удаляет папку или файл внутри рабочей директории.
     */
    fun deleteTree(relativePath: String): Boolean {
        val uri = documentUri(relativePath) ?: return false
        return deleteUriRecursive(uri)
    }

    /**
     * Гарантирует существование файла и возвращает его URI.
     *
     * Если на месте файла уже есть директория с таким именем, возвращает `null`,
     * чтобы не пытаться открыть папку как поток записи.
     */
    private fun ensureFile(relativePath: String, mimeType: String): Uri? = synchronized(lock) {
        val segments = normalizedSegments(relativePath)
        val fileName = segments.lastOrNull() ?: return null
        val directoryPath = segments.dropLast(1).joinToString("/")
        val directoryUri = ensureDirectory(directoryPath) ?: return null

        val existing = findChild(directoryUri, fileName, onlyDirectory = false)
        if (existing != null && !existing.isDirectory) return existing.uri
        if (existing != null && existing.isDirectory) return null

        DocumentsContract.createDocument(resolver, directoryUri, mimeType, fileName)
    }

    /**
     * Проходит по сегментам относительного пути и возвращает URI найденного документа.
     */
    private fun documentUri(relativePath: String): Uri? {
        val root = rootDocumentUri() ?: return null
        var current = root
        normalizedSegments(relativePath).forEach { segment ->
            current = findChild(current, segment)?.uri ?: return null
        }
        return current
    }

    /**
     * Возвращает `Entry` для файла или папки по относительному пути.
     *
     * Для корня создаёт синтетическую запись с mime type директории.
     */
    private fun documentEntry(relativePath: String): Entry? {
        val segments = normalizedSegments(relativePath)
        if (segments.isEmpty()) {
            return rootDocumentUri()?.let { Entry("", it, DocumentsContract.Document.MIME_TYPE_DIR, 0L) }
        }
        val parentPath = segments.dropLast(1).joinToString("/")
        val parentUri = documentUri(parentPath) ?: return null
        return findChild(parentUri, segments.last())
    }

    /**
     * Преобразует сохранённый tree URI в document URI корневой папки.
     */
    private fun rootDocumentUri(): Uri? {
        val treeUri = WorkDirectory.treeUri(context) ?: return null
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }

    /**
     * Ищет дочерний документ по имени.
     *
     * `onlyDirectory` позволяет явно искать только папку, только файл или любой
     * документ с подходящим именем.
     */
    private fun findChild(parentUri: Uri, name: String, onlyDirectory: Boolean? = null): Entry? {
        return listChildren(parentUri).firstOrNull { child ->
            child.name == name && when (onlyDirectory) {
                true -> child.isDirectory
                false -> !child.isDirectory
                null -> true
            }
        }
    }

    /**
     * Читает дочерние документы SAF-папки через `ContentResolver.query()`.
     *
     * Метод запрашивает только нужные колонки и превращает строки cursor в
     * `Entry`. Ошибки логируются и сворачиваются в пустой список, чтобы сбой
     * одного провайдера документов не ломал весь экран.
     */
    private fun listChildren(parentUri: Uri): List<Entry> {
        val treeUri = WorkDirectory.treeUri(context) ?: return emptyList()
        val parentDocumentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        return runCatching {
            resolver.query(childrenUri, columns, null, null, null)?.use { cursor ->
                buildList {
                    val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val displayName = cursor.getString(nameIndex)
                        val mimeType = cursor.getString(mimeIndex)
                        val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                        add(
                            Entry(
                                name = displayName,
                                uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                                mimeType = mimeType,
                                size = size
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.onFailure {
            Timber.w(it, "!!! SAF list failed: $parentUri")
        }.getOrDefault(emptyList())
    }

    /**
     * Внутренняя рекурсивная реализация удаления дерева документов.
     *
     * Сначала удаляет всех детей, затем саму папку. Если любой дочерний документ
     * удалить не удалось, операция останавливается и возвращает `false`.
     */
    private fun deleteUriRecursive(uri: Uri): Boolean {
        listChildren(uri).forEach { child ->
            val deleted = if (child.isDirectory) {
                deleteUriRecursive(child.uri)
            } else {
                runCatching { DocumentsContract.deleteDocument(resolver, child.uri) }
                    .onFailure { Timber.w(it, "!!! SAF child delete failed: ${child.uri}") }
                    .getOrDefault(false)
            }
            if (!deleted) return false
        }

        return runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            .onFailure { Timber.w(it, "!!! SAF tree delete failed: $uri") }
            .getOrDefault(false)
    }

    /**
     * Нормализует пользовательский относительный путь в безопасный список сегментов.
     *
     * Поддерживает обратные слэши, убирает пустые части и лишние пробелы,
     * поэтому остальные методы могут одинаково работать с `a/b` и `a\\b`.
     */
    private fun normalizedSegments(path: String): List<String> {
        return path
            .replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
