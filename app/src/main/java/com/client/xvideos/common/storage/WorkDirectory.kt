package com.client.xvideos.common.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.client.xvideos.common.util.defaultSharedPreferences
import android.provider.DocumentsContract
import androidx.core.content.edit
import timber.log.Timber

/**
 * Хранит выбранную пользователем рабочую папку приложения.
 *
 * Это тонкая обёртка над Storage Access Framework: объект запоминает `treeUri`
 * в `SharedPreferences`, проверяет сохранённые read/write permissions и умеет
 * создать intent для выбора папки через системный UI.
 */
object WorkDirectory {

    private const val KEY_TREE_URI = "app_work_tree_uri"

    /**
     * Возвращает сохранённый URI рабочей папки как строку.
     *
     * Пустая строка означает, что пользователь ещё не выбирал папку или выбор
     * был очищен через `clear()`.
     */
    fun treeUriString(context: Context): String {
        return prefs(context).getString(KEY_TREE_URI, "").orEmpty()
    }

    /**
     * Парсит сохранённый `treeUri` в Android `Uri`.
     *
     * Если значение пустое или повреждено, возвращает `null`, чтобы вызывающий
     * код мог безопасно перейти к сценарию выбора папки.
     */
    fun treeUri(context: Context): Uri? {
        return treeUriString(context)
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    }

    /**
     * Проверяет, сохранился ли у приложения постоянный read/write-доступ к папке.
     *
     * Простого наличия URI недостаточно: пользователь мог отозвать разрешение
     * в настройках, поэтому сверяемся с `persistedUriPermissions`.
     */
    fun hasAccess(context: Context): Boolean {
        val uri = treeUri(context) ?: return false
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }

    /**
     * Фиксирует выбранную пользователем папку и забирает постоянные разрешения.
     *
     * `flags` приходят из результата `ACTION_OPEN_DOCUMENT_TREE`; из них
     * оставляются только read/write permissions, после чего URI сохраняется
     * в настройках приложения.
     */
    fun persist(context: Context, uri: Uri, flags: Int) {
        val takeFlags = flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        prefs(context).edit { putString(KEY_TREE_URI, uri.toString()) }
        Timber.i("!!! WorkDirectory selected: $uri")
    }

    /**
     * Удаляет сохранённую рабочую папку и пытается освободить SAF-разрешение.
     *
     * Ошибка при release не прерывает очистку настроек: после вызова приложение
     * всё равно будет считать, что папка не выбрана.
     */
    fun clear(context: Context) {
        treeUri(context)?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.onFailure {
                Timber.w(it, "!!! WorkDirectory release permission failed: $uri")
            }
        }
        prefs(context).edit { remove(KEY_TREE_URI) }
    }

    /**
     * Создаёт intent для системного выбора рабочей папки.
     *
     * Флаги сразу запрашивают чтение, запись, постоянное разрешение и доступ
     * ко всем дочерним документам выбранного дерева.
     */
    fun createOpenTreeIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
    }

    /**
     * Возвращает человекочитаемое имя выбранной папки для UI.
     *
     * Для SAF tree URI извлекается document id и часть после `:`, чтобы вместо
     * длинного URI показать путь внутри выбранного провайдера документов.
     */
    fun displayName(context: Context): String {
        val uri = treeUri(context) ?: return "Not selected"
        return runCatching {
            val documentId = DocumentsContract.getTreeDocumentId(uri)
            documentId.substringAfter(':').ifBlank { uri.toString() }
        }.getOrDefault(uri.toString())
    }

    /**
     * Общие настройки, где хранится URI рабочей папки.
     */
    private fun prefs(context: Context) = context.defaultSharedPreferences()
}
