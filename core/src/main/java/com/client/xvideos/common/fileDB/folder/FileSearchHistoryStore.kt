package com.client.xvideos.common.fileDB.folder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class FileSearchHistoryStore(
    private val table: FolderTable
) {
    private val texts = MutableStateFlow<List<String>>(emptyList())

    fun observeAllTexts(): Flow<List<String>> = texts.asStateFlow()

    suspend fun insertAndTrim(text: String, limit: Int) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return

        val now = System.currentTimeMillis()
        table.upsert(
            key = normalized,
            fields = mapOf(
                FIELD_TEXT to normalized,
                FolderTable.FIELD_TIME_CREATE to now.toString(),
                FolderTable.FIELD_TIME_CREATE_TEXT to currentFileDbTimeText()
            )
        )
        trim(limit)
        refresh()
    }

    suspend fun deleteByTexts(text: String) {
        table.delete(text)
        refresh()
    }

    suspend fun deleteAll() {
        table.deleteAll()
        refresh()
    }

    suspend fun refresh() {
        val all = table.all()
        if (all.isEmpty()) {
            if (texts.value.isNotEmpty()) {
                texts.value = emptyList()
            }
            return
        }
        texts.value = all
            .sortedByDescending { it.fields[FolderTable.FIELD_TIME_CREATE]?.toLongOrNull() ?: 0L }
            .mapNotNull { it.fields[FIELD_TEXT] ?: it.key }
    }

    private suspend fun trim(limit: Int) {
        val all = table.all()
        if (all.size <= limit) return
        all.sortedByDescending { it.fields[FolderTable.FIELD_TIME_CREATE]?.toLongOrNull() ?: 0L }
            .drop(limit)
            .forEach { table.delete(it.key) }
    }

    private companion object {
        const val FIELD_TEXT = "text"
    }
}
