package com.client.xvideos.l.repository

// Результаты операций
sealed class RepositoryResult {
    data object Loading : RepositoryResult()
    data class Success<T>(val data: T) : RepositoryResult()
    data class Error(val message: String, val throwable: Throwable? = null) : RepositoryResult()
}

val RepositoryResult.isLoading: Boolean get() = this is RepositoryResult.Loading
val RepositoryResult.isSuccess: Boolean get() = this is RepositoryResult.Success<*>
val RepositoryResult.isError: Boolean get() = this is RepositoryResult.Error

@Suppress("UNCHECKED_CAST")
fun <T> RepositoryResult.getOrNull(): T? = (this as? RepositoryResult.Success<*>)?.data as? T

fun RepositoryResult.errorMessageOrNull(): String? = (this as? RepositoryResult.Error)?.message

fun RepositoryResult.throwableOrNull(): Throwable? = (this as? RepositoryResult.Error)?.throwable
