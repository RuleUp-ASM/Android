package com.ruleup.onboarding.domain.auth.entity

/** 인증·가입에서 화면이 구분해야 하는 실패. */
enum class AuthFailure {
    /** 인가 코드·id_token 검증 실패. */
    LOGIN_FAILED,

    /** redirectUri 불일치(구글). */
    INVALID_REDIRECT_URI,

    /** deviceId·deviceInfo 누락/형식 오류. */
    INVALID_DEVICE_INFO,

    /** IdP 장애(502). */
    PROVIDER_UNAVAILABLE,

    /** 영구 정지 계정. */
    ACCOUNT_BANNED,

    /** 이 설치에 이미 활성 계정이 있다. */
    INSTALLATION_ALREADY_REGISTERED,

    /** signup_token 이 만료·위조·사용됨. */
    INVALID_SIGNUP_TOKEN,

    NICKNAME_FORMAT_INVALID,
    NICKNAME_DUPLICATED,
    NICKNAME_RECENTLY_RELEASED,

    BIRTHDATE_INVALID,

    /** 만 14세 미만. */
    BIRTHDATE_UNDERAGE,

    GENDER_REQUIRED,
    INTEREST_LIMIT_EXCEEDED,
    REQUIRED_AGREEMENT_MISSING,

    IMAGE_TOO_LARGE,
    IMAGE_INVALID_TYPE,
    IMAGE_CORRUPTED,

    /** 다른 기기 로그인 등으로 세션이 끊겼다. */
    SESSION_EXPIRED,

    /** 계정 잠금 중 차단된 기능(프로필 편집 등). */
    ACCOUNT_LOCKED,

    /** 네트워크·오프라인. */
    NETWORK,

    UNKNOWN,
}

/** [AuthFailure] 를 실은 예외. */
class AuthException(
    val failure: AuthFailure,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
