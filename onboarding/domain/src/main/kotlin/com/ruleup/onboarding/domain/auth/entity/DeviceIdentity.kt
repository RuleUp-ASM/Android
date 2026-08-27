package com.ruleup.onboarding.domain.auth.entity

/**
 * 로그인·가입 요청에 함께 실리는 두 식별자. **서로 다른 질문에 답하므로 같은 값을 쓰면 안 된다.**
 *
 * @property deviceId "같은 **기기**냐" — 단일 활성 기기 판정 키. 서버는 값이 다르면 기존 세션을 끊는다.
 *   **재설치에는 살아남고** 기기 교체·초기화에는 바뀌어야 한다.
 * @property installationId "같은 **설치본**이냐" — 한 설치에 계정 하나(`INSTALLATION_ALREADY_REGISTERED`)
 *   와 제재 회피 재가입 판별 키. 정의상 **재설치하면 새 값**이어야 한다.
 */
data class DeviceIdentity(
    val deviceId: String,
    val installationId: String,
)
