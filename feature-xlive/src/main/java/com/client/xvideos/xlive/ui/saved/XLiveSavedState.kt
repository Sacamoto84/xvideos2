package com.client.xvideos.xlive.ui.saved

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.model.XLiveSavedRoom
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Сетка «Моих» или «Истории».
 *
 * @property rooms комнаты в порядке показа: «Мои» — сначала в эфире, «История» — от недавних.
 * @property online ник → в эфире; комнаты, которую ещё не проверили, здесь нет.
 * @property isChecking идёт проверка эфира.
 * @property snapshotStamp метка снимков, unix-время в секундах: новая — свежие кадры.
 */
@Immutable
data class XLiveSavedState(
    val rooms: ImmutableList<XLiveSavedRoom> = persistentListOf(),
    val online: ImmutableMap<String, Boolean> = persistentMapOf(),
    val isChecking: Boolean = false,
    val snapshotStamp: Long = 0L,
)
