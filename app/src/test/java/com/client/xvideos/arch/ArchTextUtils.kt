package com.client.xvideos.arch

internal fun stripCommentsAndStrings(src: String): String {
    val out = StringBuilder(src.length)
    var i = 0
    val n = src.length

    while (i < n) {
        i = when {
            src.startsWith("\"\"\"", i) -> skipTripleQuotedString(src, i, out)
            src[i] == '"' -> skipDoubleQuotedString(src, i, out)
            src[i] == '\'' -> skipCharLiteral(src, i, out)
            src.startsWith("//", i) -> skipLineComment(src, i, out)
            src.startsWith("/*", i) -> skipBlockComment(src, i, out)
            else -> {
                out.append(src[i])
                i + 1
            }
        }
    }
    return out.toString()
}

private fun skipTripleQuotedString(src: String, start: Int, out: StringBuilder): Int {
    out.append("   ")
    var i = start + 3
    while (i < src.length) {
        if (src.startsWith("\"\"\"", i)) {
            out.append("   ")
            return i + 3
        }
        out.append(if (src[i] == '\n') '\n' else ' ')
        i++
    }
    return i
}

private fun skipDoubleQuotedString(src: String, start: Int, out: StringBuilder): Int {
    out.append(' ')
    var i = start + 1
    while (i < src.length) {
        val ch = src[i]
        if (ch == '\\' && i + 1 < src.length) {
            out.append("  ")
            i += 2
            continue
        }
        out.append(if (ch == '\n') '\n' else ' ')
        i++
        if (ch == '"') return i
    }
    return i
}

private fun skipCharLiteral(src: String, start: Int, out: StringBuilder): Int {
    out.append(' ')
    var i = start + 1
    while (i < src.length) {
        val ch = src[i]
        if (ch == '\\' && i + 1 < src.length) {
            out.append("  ")
            i += 2
            continue
        }
        out.append(if (ch == '\n') '\n' else ' ')
        i++
        if (ch == '\'' || ch == '\n') return i
    }
    return i
}

private fun skipLineComment(src: String, start: Int, out: StringBuilder): Int {
    var i = start
    while (i < src.length && src[i] != '\n') {
        out.append(' ')
        i++
    }
    return i
}

private fun skipBlockComment(src: String, start: Int, out: StringBuilder): Int {
    var depth = 1
    out.append("  ")
    var i = start + 2
    while (i < src.length && depth > 0) {
        when {
            src.startsWith("/*", i) -> {
                depth++
                out.append("  ")
                i += 2
            }
            src.startsWith("*/", i) -> {
                depth--
                out.append("  ")
                i += 2
            }
            else -> {
                out.append(if (src[i] == '\n') '\n' else ' ')
                i++
            }
        }
    }
    return i
}
