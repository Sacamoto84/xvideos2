package com.client.xvideos.common.kdownloader.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Реализация хранилища [DbHelper] на базе Android SQLite.
 *
 * Обеспечивает персистентность метаданных загрузок, потокобезопасные асинхронные
 * операции в контексте [Dispatchers.IO] и обработку ошибок SQLite с логированием через Timber.
 */
class AppDbHelper(context: Context?) : DbHelper {

    private var db: SQLiteDatabase

    companion object {
        /** Имя таблицы для хранения записей о загрузках. */
        const val TABLE_NAME = "downloads"
    }

    init {
        val databaseOpenHelper = DatabaseOpenHelper(context)
        db = databaseOpenHelper.writableDatabase
    }

    @SuppressLint("Range")
    override suspend fun find(id: Int): DownloadModel? = withContext(Dispatchers.IO) {
        var downloadModel: DownloadModel? = null
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE ${DownloadModel.ID} = ?",
            arrayOf(id.toString())
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                downloadModel = DownloadModel(
                    id = id,
                    url = c.getString(c.getColumnIndex(DownloadModel.URL)).orEmpty(),
                    eTag = c.getString(c.getColumnIndex(DownloadModel.ETAG)).orEmpty(),
                    dirPath = c.getString(c.getColumnIndex(DownloadModel.DIR_PATH)).orEmpty(),
                    fileName = c.getString(c.getColumnIndex(DownloadModel.FILE_NAME)).orEmpty(),
                    totalBytes = c.getLong(c.getColumnIndex(DownloadModel.TOTAL_BYTES)),
                    downloadedBytes = c.getLong(c.getColumnIndex(DownloadModel.DOWNLOADED_BYTES)),
                    lastModifiedAt = c.getLong(c.getColumnIndex(DownloadModel.LAST_MODIFIED_AT))
                )
            }
        }
        downloadModel
    }

    override suspend fun insert(model: DownloadModel): Unit = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(DownloadModel.ID, model.id)
                put(DownloadModel.URL, model.url)
                put(DownloadModel.ETAG, model.eTag)
                put(DownloadModel.DIR_PATH, model.dirPath)
                put(DownloadModel.FILE_NAME, model.fileName)
                put(DownloadModel.TOTAL_BYTES, model.totalBytes)
                put(DownloadModel.DOWNLOADED_BYTES, model.downloadedBytes)
                put(DownloadModel.LAST_MODIFIED_AT, model.lastModifiedAt)
            }
            db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            Timber.e(e, "KDownloader DB: insert(model id=${model.id}) failed")
        }
    }

    override suspend fun update(model: DownloadModel): Unit = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(DownloadModel.URL, model.url)
                put(DownloadModel.ETAG, model.eTag)
                put(DownloadModel.DIR_PATH, model.dirPath)
                put(DownloadModel.FILE_NAME, model.fileName)
                put(DownloadModel.TOTAL_BYTES, model.totalBytes)
                put(DownloadModel.DOWNLOADED_BYTES, model.downloadedBytes)
                put(DownloadModel.LAST_MODIFIED_AT, model.lastModifiedAt)
            }
            db.update(
                TABLE_NAME,
                values,
                DownloadModel.ID + " = ? ",
                arrayOf((model.id).toString())
            )
        } catch (e: Exception) {
            Timber.e(e, "KDownloader DB: update(model id=${model.id}) failed")
        }
    }

    override suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(DownloadModel.DOWNLOADED_BYTES, downloadedBytes)
                put(DownloadModel.LAST_MODIFIED_AT, lastModifiedAt)
            }
            db.update(
                TABLE_NAME,
                values,
                DownloadModel.ID + " = ? ",
                arrayOf("$id")
            )
        } catch (e: Exception) {
            Timber.e(e, "KDownloader DB: updateProgress(id=$id) failed")
        }
    }

    override suspend fun remove(id: Int): Unit = withContext(Dispatchers.IO) {
        try {
            db.delete(
                TABLE_NAME,
                "${DownloadModel.ID} = ?",
                arrayOf(id.toString())
            )
        } catch (e: Exception) {
            Timber.e(e, "KDownloader DB: remove(id=$id) failed")
        }
    }

    @SuppressLint("Range")
    override suspend fun getUnwantedModels(days: Int): List<DownloadModel> = withContext(Dispatchers.IO) {
        val models: MutableList<DownloadModel> = ArrayList()
        try {
            val daysInMillis = days * 24 * 60 * 60 * 1000L
            val beforeTimeInMillis = System.currentTimeMillis() - daysInMillis
            db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE ${DownloadModel.LAST_MODIFIED_AT} <= ?",
                arrayOf(beforeTimeInMillis.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val model = DownloadModel().apply {
                            id = cursor.getInt(cursor.getColumnIndex(DownloadModel.ID))
                            url = cursor.getString(cursor.getColumnIndex(DownloadModel.URL)).orEmpty()
                            eTag = cursor.getString(cursor.getColumnIndex(DownloadModel.ETAG)).orEmpty()
                            dirPath = cursor.getString(cursor.getColumnIndex(DownloadModel.DIR_PATH)).orEmpty()
                            fileName = cursor.getString(cursor.getColumnIndex(DownloadModel.FILE_NAME)).orEmpty()
                            totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadModel.TOTAL_BYTES))
                            downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadModel.DOWNLOADED_BYTES))
                            lastModifiedAt = cursor.getLong(cursor.getColumnIndex(DownloadModel.LAST_MODIFIED_AT))
                        }
                        models.add(model)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "KDownloader DB: getUnwantedModels() failed")
        }
        models
    }

    override suspend fun empty(): Unit = withContext(Dispatchers.IO) {
        try {
            // Было "DELETE * FROM downloads" — невалидный SQLite (звёздочка в DELETE
            // не допускается). execSQL всегда кидал SQLiteException, а printStackTrace
            // его гасил, поэтому cancelAll() никогда не очищал таблицу.
            db.delete(TABLE_NAME, null, null)
        } catch (e: Exception) {
            Timber.e(e, "KDownloader DB: empty() failed")
        }
    }

}
