package org.bharatscan.app.ui.screens.export

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG
}

fun evaluatePasswordStrength(password: String): PasswordStrength {
    val trimmed = password.trim()
    if (trimmed.isEmpty()) return PasswordStrength.WEAK

    var score = 0
    val length = trimmed.length
    if (length >= 8) score++
    if (length >= 12) score++
    if (trimmed.any { it.isLowerCase() }) score++
    if (trimmed.any { it.isUpperCase() }) score++
    if (trimmed.any { it.isDigit() }) score++
    if (trimmed.any { !it.isLetterOrDigit() }) score++

    return when {
        score >= 5 -> PasswordStrength.STRONG
        score >= 3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}
