package com.client.xvideos.common.kdownloader.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Вспомогательный класс создания и миграции SQLite-базы данных загрузчика [KDownloader].
 *
 * Создает таблицу [AppDbHelper.TABLE_NAME] для сохранения состояния докачки файлов.
 */
class DatabaseOpenHelper internal constructor(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /**
     * Создание структуры таблиц базы данных при первом запуске.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS " +
                    AppDbHelper.TABLE_NAME + "( " +
                    DownloadModel.ID + " INTEGER PRIMARY KEY, " +
                    DownloadModel.URL + " VARCHAR, " +
                    DownloadModel.ETAG + " VARCHAR, " +
                    DownloadModel.DIR_PATH + " VARCHAR, " +
                    DownloadModel.FILE_NAME + " VARCHAR, " +
                    DownloadModel.TOTAL_BYTES + " INTEGER, " +
                    DownloadModel.DOWNLOADED_BYTES + " INTEGER, " +
                    DownloadModel.LAST_MODIFIED_AT + " INTEGER " +
                    ")"
        )
    }

    /**
     * Миграция схемы базы данных при обновлении версии.
     */
    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {}

    companion object {
        private const val DATABASE_NAME = "kdownloader.db"
        private const val DATABASE_VERSION = 1
    }
}
