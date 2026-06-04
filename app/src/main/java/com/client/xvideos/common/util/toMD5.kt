package com.client.xvideos.common.util

import java.security.MessageDigest

/**
 * Вернёт строку из 32 символов (hex).MD5:
 * MD5: 8f0f1a13ddcf5cfb11df0c5c51b95e7
 */
fun String.toMD5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}