package com.client.xvideos.l.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val email: String = "",
    val password: String = ""
) {
    val isValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
    val isEmpty: Boolean get() = email.isEmpty() && password.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val hasEmail: Boolean get() = email.isNotBlank()
    val hasPassword: Boolean get() = password.isNotBlank()
    val isConfigured: Boolean get() = isValid
    val hasCredentials: Boolean get() = isValid

    companion object {
        val EMPTY = UserProfile()
    }
}
