package com.client.xvideos.l.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val email: String = "",
    val password: String = ""
)