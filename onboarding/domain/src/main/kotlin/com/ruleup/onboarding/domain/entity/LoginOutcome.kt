package com.ruleup.onboarding.domain.entity

/**
 * 소셜 로그인 후 화면이 가야 할 곳. [OAuthResult] 를 계정 상태까지 반영해 정규화한 값이다.
 *
 * 영구 정지(`ACCOUNT_BANNED`)와 동일 설치 다계정(`INSTALLATION_ALREADY_REGISTERED`)은 여기 없다 —
 * 403 이라 응답 본문이 오지 않으므로 예외로 전파돼 화면이 분기한다.
 */
sealed interface LoginOutcome {
    data object GoHome : LoginOutcome

    /**
     * 열람 전용 홈. 계정이 잠겼지만 로그인 자체는 허용된다.
     *
     * @property lockInfo 사유와 해제 시각. 안내 문구에 쓴다.
     */
    data class GoHomeReadOnly(
        val lockInfo: LockInfo?,
    ) : LoginOutcome

    /**
     * 닉네임 재설정 강제. 복원 과정에서 기존 닉네임을 남이 선점한 경우다
     * (`nicknameStatus=CONFLICT`).
     *
     * 세션은 이미 저장됐지만 **닉네임을 바꾸기 전엔 홈으로 보내지 않는다.**
     *
     * @property currentNickname 선점당한 기존 닉네임. 표시용이며 타인에게는 임시 닉네임이 보인다.
     */
    data class ResetNickname(
        val currentNickname: String,
    ) : LoginOutcome

    /** 신규 가입. 온보딩 1단계로 간다. */
    data class GoSignup(
        val signupToken: String,
        val expiresInSeconds: Int,
        val profile: OAuthProfile,
    ) : LoginOutcome
}
