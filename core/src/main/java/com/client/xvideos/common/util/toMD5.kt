package com.client.xvideos.common.util

import java.security.MessageDigest

private val HEX_DIGITS = "0123456789abcdef".toCharArray()
private const val EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e"

/**
 * Вернёт строку из 32 символов (hex).
 */
fun ByteArray.toMD5(): String {
    if (isEmpty()) return EMPTY_MD5
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this)
    val chars = CharArray(digest.size * 2)
    for (i in digest.indices) {
        val b = digest[i].toInt() and 0xFF
        chars[i * 2] = HEX_DIGITS[b ushr 4]
        chars[i * 2 + 1] = HEX_DIGITS[b and 0x0F]
    }
    return String(chars)
}

/**
 * Вернёт строку из 32 символов (hex).MD5:
 * MD5: 8f0f1a13ddcf5cfb11df0c5c51b95e7
 */
fun String.toMD5(): String {
    if (isEmpty()) return EMPTY_MD5
    return this.toByteArray(Charsets.UTF_8).toMD5()
}
