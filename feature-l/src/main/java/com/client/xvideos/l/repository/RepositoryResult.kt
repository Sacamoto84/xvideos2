package com.client.xvideos.l.repository

/**
 * Иерархия обобщенных результатов выполнения операций репозитория.
 */
sealed class RepositoryResult {
    /** Статус выполнения операции (загрузка данных). */
    data object Loading : RepositoryResult()

    /**
     * Успешное завершение операции с полученными данными [data].
     *
     * @property data Полезная нагрузка результата.
     */
    data class Success<T>(val data: T) : RepositoryResult()

    /**
     * Ошибка выполнения операции.
     *
     * @property message Текстовое описание ошибки.
     * @property throwable Исходное исключение (если есть).
     */
    data class Error(val message: String, val throwable: Throwable? = null) : RepositoryResult()
}

/** Флаг состояния загрузки. */
val RepositoryResult.isLoading: Boolean get() = this is RepositoryResult.Loading
/** Флаг успешного завершения. */
val RepositoryResult.isSuccess: Boolean get() = this is RepositoryResult.Success<*>
/** Флаг ошибки. */
val RepositoryResult.isError: Boolean get() = this is RepositoryResult.Error

/**
 * Безопасно извлекает данные типа [T] при успешном результате либо возвращает `null`.
 */
@Suppress("UNCHECKED_CAST")
fun <T> RepositoryResult.getOrNull(): T? = (this as? RepositoryResult.Success<*>)?.data as? T

/**
 * Извлекает данные типа [T] при успехе либо возвращает указанное значение по умолчанию [default].
 */
fun <T> RepositoryResult.getOrDefault(default: T): T = getOrNull<T>() ?: default

/**
 * Выполняет блок [action], если результат является успешным с данными [T].
 */
inline fun <T> RepositoryResult.onSuccess(action: (T) -> Unit): RepositoryResult {
    getOrNull<T>()?.let(action)
    return this
}

/**
 * Выполняет блок [action], если результат содержит ошибку.
 */
inline fun RepositoryResult.onError(action: (message: String, throwable: Throwable?) -> Unit): RepositoryResult {
    if (this is RepositoryResult.Error) {
        action(message, throwable)
    }
    return this
}

/**
 * Выполняет блок [action], если результат находится в состоянии загрузки.
 */
inline fun RepositoryResult.onLoading(action: () -> Unit): RepositoryResult {
    if (this is RepositoryResult.Loading) {
        action()
    }
    return this
}

/**
 * Трансформирует данные успешного результата с помощью [transform].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R> RepositoryResult.map(transform: (T) -> R): RepositoryResult {
    return when (this) {
        is RepositoryResult.Success<*> -> RepositoryResult.Success(transform(this.data as T))
        else -> this
    }
}

/**
 * Возвращает текст ошибки, если результат является [RepositoryResult.Error], либо `null`.
 */
fun RepositoryResult.errorMessageOrNull(): String? = (this as? RepositoryResult.Error)?.message

/**
 * Возвращает исходное исключение ошибки, если результат является [RepositoryResult.Error], либо `null`.
 */
fun RepositoryResult.throwableOrNull(): Throwable? = (this as? RepositoryResult.Error)?.throwable

/**
 * Сворачивает результат в единое значение [R].
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R> RepositoryResult.fold(
    onSuccess: (T) -> R,
    onError: (String, Throwable?) -> R,
    onLoading: () -> R
): R {
    return when (this) {
        is RepositoryResult.Success<*> -> onSuccess(this.data as T)
        is RepositoryResult.Error -> onError(this.message, this.throwable)
        else -> onLoading()
    }
}

/**
 * Заменяет ошибку на успешный fallback результат.
 */
fun <T> RepositoryResult.recover(fallback: T): RepositoryResult =
    if (this is RepositoryResult.Error) RepositoryResult.Success(fallback) else this

