package com.ruleup.onboarding.domain.auth.entity

/**
 * 인증·가입에서 화면이 구분해야 하는 실패. 서버 에러 코드를 도메인 어휘로 옮긴 값이다.
 *
 * presentation 이 서버 코드 문자열을 직접 보게 두면 `core:network` 의 `ApiException` 에 의존하게
 * 되고, 코드가 바뀔 때마다 화면을 뒤져야 한다. data 가 여기로 번역하고 화면은 이 enum 만 본다.
 */
enum class AuthFailure {
    /** 인가 코드·id_token 검증 실패. */
    LOGIN_FAILED,

    /** redirectUri 불일치(구글). 사용자가 할 수 있는 게 없어 로그인 실패와 같이 안내한다. */
    INVALID_REDIRECT_URI,

    /** deviceId·deviceInfo 누락/형식 오류. 마찬가지로 사용자 조치 불가. */
    INVALID_DEVICE_INFO,

    /** IdP 장애(502). 다른 제공자를 권한다. */
    PROVIDER_UNAVAILABLE,

    /** 영구 정지 계정. 로그인 자체가 막힌다. */
    ACCOUNT_BANNED,

    /** 이 설치에 이미 활성 계정이 있다. 신규 가입만 막히고 기존 계정 로그인은 된다. */
    INSTALLATION_ALREADY_REGISTERED,

    /** signup_token 이 만료·위조·사용됨. 로그인부터 다시 한다. */
    INVALID_SIGNUP_TOKEN,

    NICKNAME_FORMAT_INVALID,
    NICKNAME_DUPLICATED,
    NICKNAME_RECENTLY_RELEASED,

    BIRTHDATE_INVALID,

    /** 만 14세 미만. 가입이 불가하다. */
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

    /** 네트워크·오프라인. 재시도로 풀릴 수 있다. */
    NETWORK,

    UNKNOWN,
}

/** [AuthFailure] 를 실은 예외. 화면은 [failure] 로 분기하고 [message] 는 진단용으로만 쓴다. */
class AuthException(
    val failure: AuthFailure,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
