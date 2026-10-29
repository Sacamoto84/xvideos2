package com.client.xvideos.xlive.ui.saved

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.model.XLiveRecord
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Подвкладка «Записи».
 *
 * @property records записи от новых к старым.
 * @property snapshots id записи → путь к снимку комнаты; файла может не быть.
 * @property online ник → в эфире, как у «Моих»; ника, которого ещё не
 *   проверили, здесь нет. Карточка показывает только «в эфире».
 */
@Immutable
data class XLiveRecordsState(
    val records: ImmutableList<XLiveRecord> = persistentListOf(),
    val snapshots: ImmutableMap<Long, String> = persistentMapOf(),
    val online: ImmutableMap<String, Boolean> = persistentMapOf(),
)
