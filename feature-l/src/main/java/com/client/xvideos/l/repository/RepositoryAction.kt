package com.client.xvideos.l.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Фильтрует поток [RepositoryResult], пропуская только результаты конкретного типа [T].
 */
inline fun <reified T : RepositoryResult> Flow<RepositoryResult>.filterResult(): Flow<T> {
    return filterIsInstance<T>()
}

/**
 * Иерархия команд и действий для репозитория Luscious.
 */
sealed class RepositoryAction {

    /**
     * Загрузить данные альбома по его числовому идентификатору [id].
     *
     * @property id Идентификатор альбома.
     */
    data class LoadAlbum(val id: Long) : RepositoryAction()

    /**
     * Выполнить вход в аккаунт Luscious с сохраненными учетными данными.
     */
    data object Login : RepositoryAction()
}

/** Истина, если действие — загрузка альбома. */
val RepositoryAction.isLoadAlbum: Boolean get() = this is RepositoryAction.LoadAlbum

/** Истина, если действие — вход в систему. */
val RepositoryAction.isLogin: Boolean get() = this is RepositoryAction.Login

/** Идентификатор альбома для действия [RepositoryAction.LoadAlbum] или `null`. */
val RepositoryAction.albumIdOrNull: Long? get() = (this as? RepositoryAction.LoadAlbum)?.id

