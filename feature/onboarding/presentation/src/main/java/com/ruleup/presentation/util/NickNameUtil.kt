package com.ruleup.presentation.util

object NickNameUtil {
    private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]+$")

    fun inRange(name: String): Boolean = name.length in 2..12

    fun isValidName(name: String): Boolean = NICKNAME_REGEX.matches(name)

    fun isValid(name: String): Boolean = inRange(name) && isValidName(name)
}
