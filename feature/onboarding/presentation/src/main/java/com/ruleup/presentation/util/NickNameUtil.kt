package com.ruleup.presentation.util

object NickNameUtil {
    private const val MIN_LENGTH = 2
    private const val MAX_LENGTH = 12
    private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]+$")

    fun inRange(name: String): Boolean = name.length in MIN_LENGTH..MAX_LENGTH

    fun isValidName(name: String): Boolean = NICKNAME_REGEX.matches(name)

    fun isValid(name: String): Boolean = inRange(name) && isValidName(name)
}
