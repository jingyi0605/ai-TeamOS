package com.outcomego.mobile.network

class NetworkTargetException(val code: String) : IllegalArgumentException(code)

fun canonicalHost(host: String): String {
    if (host.isBlank() || host.length > 253 || host.contains('%')) {
        throw NetworkTargetException("destination_blocked")
    }
    val value = host.trim().removeSuffix(".").lowercase()
    if (value.isEmpty() || value == "localhost" || value.endsWith(".localhost") || value.endsWith(".local")) {
        throw NetworkTargetException("destination_blocked")
    }
    if (value.contains(':')) {
        if (!isPublicAddress(value)) throw NetworkTargetException("destination_blocked")
        return value
    }
    if (isIpv4(value)) {
        if (!isPublicAddress(value)) throw NetworkTargetException("destination_blocked")
        return value
    }
    if (
        value.any { it.code > 0x7f || it.isWhitespace() || it.code < 0x20 || it == '/' || it == '@' || it == '?' || it == '#' || it == '%' || it == '\\' } ||
        value.split('.').any { label ->
            label.isEmpty() ||
                label.length > 63 ||
                !label.first().isLetterOrDigit() ||
                !label.last().isLetterOrDigit() ||
                label.any { !it.isLetterOrDigit() && it != '-' }
        } ||
        value.all { it.isDigit() || it == '.' } ||
        value.startsWith("0x") ||
        value.matches(Regex("0[0-7]+"))
    ) {
        throw NetworkTargetException("destination_blocked")
    }
    return value
}

fun isPublicAddress(address: String): Boolean {
    val value = address.trim().removePrefix("[").removeSuffix("]")
    if (isIpv4(value)) return publicIpv4(value)
    val groups = parseIpv6(value) ?: return false
    if (groups.all { it == 0 }) return false
    if (groups.dropLast(1).all { it == 0 } && groups.last() == 1) return false
    val first = groups[0]
    if (first in 0xfc00..0xfdff || first in 0xfe80..0xfebf || first in 0xff00..0xffff) return false
    if (first == 0x2001 && groups[1] == 0x0db8) return false
    if (first == 0x2001 && groups[1] in 0..0x00ff) return false
    if (first == 0x2002 || first == 0x3fff) return false
    if (first == 0x0064 && groups[1] == 0xff9b) return false
    return true
}

private fun publicIpv4(value: String): Boolean {
    val parts = value.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4 || parts.any { it !in 0..255 }) return false
    val a = parts[0]
    val b = parts[1]
    val c = parts[2]
    return when {
        a == 0 || a == 10 || a == 127 || a >= 224 -> false
        a == 100 && b in 64..127 -> false
        a == 169 && b == 254 -> false
        a == 172 && b in 16..31 -> false
        a == 192 && b == 168 -> false
        a == 192 && b == 0 && c == 0 -> false
        a == 192 && b == 0 && c == 2 -> false
        a == 198 && b == 18 -> false
        a == 198 && b == 19 -> false
        a == 198 && b == 51 && c == 100 -> false
        a == 203 && b == 0 && c == 113 -> false
        else -> true
    }
}

private fun isIpv4(value: String): Boolean =
    value.count { it == '.' } == 3 && value.split('.').all { it.isNotEmpty() && it.all(Char::isDigit) }

private fun parseIpv6(value: String): List<Int>? {
    if (value.isEmpty() || value.count { it == ':' } < 2) return null
    val normalized = value.lowercase()
    if (normalized.count { it == '.' } > 0) return null
    val double = normalized.indexOf("::")
    if (double >= 0 && normalized.indexOf("::", double + 1) >= 0) return null
    val left = if (double >= 0) normalized.substring(0, double) else normalized
    val right = if (double >= 0) normalized.substring(double + 2) else ""
    fun parsePart(part: String): List<Int>? =
        if (part.isEmpty()) emptyList()
        else part.split(':').map { token ->
            if (token.isEmpty() || token.length > 4 || token.toIntOrNull(16) == null)
                return null
            token.toInt(16)
        }
    val leftValues = parsePart(left) ?: return null
    val rightValues = parsePart(right) ?: return null
    val missing = if (double >= 0) 8 - leftValues.size - rightValues.size else 0
    if (missing < 0 || (double < 0 && leftValues.size != 8)) return null
    return leftValues + List(missing) { 0 } + rightValues
}
