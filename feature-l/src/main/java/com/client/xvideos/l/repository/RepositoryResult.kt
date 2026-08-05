package com.client.xvideos.l.repository

// Результаты операций
sealed class RepositoryResult {
    object Loading : RepositoryResult()
    data class Success<T>(val data: T) : RepositoryResult()
    data class Error(val message: String, val throwable: Throwable? = null) : RepositoryResult()
}
