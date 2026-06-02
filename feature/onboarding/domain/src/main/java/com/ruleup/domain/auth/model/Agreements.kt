package com.ruleup.domain.auth.model

/**
 * 가입 시 약관 동의 내역. [terms] 와 [privacy] 는 반드시 true 여야 한다.
 */
data class Agreements(
    val terms: Boolean,
    val privacy: Boolean,
    val marketing: Boolean = false,
)
