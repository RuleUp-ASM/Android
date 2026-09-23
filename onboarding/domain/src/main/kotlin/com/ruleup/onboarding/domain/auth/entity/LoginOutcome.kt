package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.user.LockInfo

/**
 * 소셜 로그인 후 화면이 가야 할 곳. [OAuthResult] 를 계정 상태까지 반영해 정규화한 값이다.
 *
 * 영구 정지(`ACCOUNT_BANNED`)와 동일 설치 다계정(`INSTALLATION_ALREADY_REGISTERED`)은 여기 없다 —
 * 403 이라 응답 본문이 오지 않으므로 예외로 전파돼 화면이 분기한다.
 */
sealed interface LoginOutcome {
    /**
     * 홈으로.
     *
     * @property restored 탈퇴 1년 내 재가입으로 계정이 복원됐는지. 기능 스펙의 복원 건수 지표가
     *   이 값을 센다.
     */
    data class GoHome(
        val restored: Boolean,
    ) : LoginOutcome

    /**
     * 제한이 걸린 계정. 로그인 자체는 허용되지만 **어디까지 막혔는지는 여기서 알 수 없다.**
     *
     * 로그인 응답의 상태값은 정지 종류를 구분하지 않으므로(`SUSPENDED` 하나), 화면이
     * `AccountRestrictionProvider` 에 한 번 더 물어 전체 잠금과 기능 정지를 가른다.
     *
     * @property lockInfo 사유와 해제 시각. 응답에 있으면 안내 문구에 쓴다.
     */
    data class Restricted(
        val lockInfo: LockInfo?,
    ) : LoginOutcome

    /**
     * 닉네임 재설정 강제. 복원 중 기존 닉네임을 남이 선점한 경우(`nicknameStatus=CONFLICT`)이며,
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
