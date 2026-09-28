package com.ruleup.domain.entity.user

/** 성별. */
enum class Gender(
    val value: String,
) {
    MALE("MALE"),
    FEMALE("FEMALE"),
    ;

    companion object {
        fun fromValue(value: String?): Gender? = entries.find { it.value == value }
    }
}
