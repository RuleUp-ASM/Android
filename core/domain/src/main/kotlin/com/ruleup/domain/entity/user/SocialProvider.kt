package com.ruleup.domain.entity.user

/**
 * 계정에 연결된 소셜 제공자 (명세 `user.provider` — 2026-09-07 신규).
 *
 * 로그인 요청에 싣는 제공자 어휘(`onboarding` 의 `OAuthProvider`, 소문자)와 **다른 축이다** —
 * 이쪽은 서버가 계정에 기록해 둔 값이고 대문자다. 로그인 시점에 앱도 알지만 **재설치·기기 변경
 * 후에는 알 수 없어** 서버 값을 원본으로 둔다.
 */
enum class SocialProvider(
    val value: String,
) {
    KAKAO("KAKAO"),
    GOOGLE("GOOGLE"),
    ;

    companion object {
        /**
         * 미지 값은 null — 제공자를 모르면 「연결된 계정」 줄에서 이름만 빠진다. 아무 쪽으로 접으면
         * 카카오로 가입한 사람에게 구글이라고 말하게 된다.
         */
        fun fromValue(value: String?): SocialProvider? = entries.find { it.value == value }
    }
}
