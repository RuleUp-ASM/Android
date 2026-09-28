package com.ruleup.domain.entity.user

/** 계정에 연결된 소셜 제공자. */
enum class SocialProvider(
    val value: String,
) {
    KAKAO("KAKAO"),
    GOOGLE("GOOGLE"),
    ;

    companion object {
        /** 미지 값은 null */
        fun fromValue(value: String?): SocialProvider? = entries.find { it.value == value }
    }
}
