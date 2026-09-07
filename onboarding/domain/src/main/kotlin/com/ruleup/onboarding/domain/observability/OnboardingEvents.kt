package com.ruleup.onboarding.domain.observability

import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.model.attributes

/**
 * 온보딩 퍼널 이벤트. **기획 스펙 9번(로깅)의 릴리즈 게이트**라, 하나라도 빠지면 완주율·이탈
 * 지점을 측정할 수 없어 릴리즈가 보류된다.
 *
 * 팩토리 시그니처가 곧 스키마다 — 파라미터 타입이 값 타입을 고정하므로 별도 스키마 선언을 두지
 * 않고, 출력을 그대로 박아 두는 단위 테스트로 검증한다.
 *
 * 권한(`permission_*`)·홈 첫 진입(`home_first_view`)·탈퇴(`withdraw_complete`)는 여기 없다.
 * 권한이 온보딩 밖으로 빠지면서 발생 지점이 각각 인증·홈·마이페이지 모듈로 옮겨갔고, 그쪽에서
 * 같은 방식으로 팩토리를 두는 게 맞다.
 */
object OnboardingEvents {
    /** 로그인 화면 진입. 완주율의 분모다. */
    fun loginScreenView(entryType: LoginEntryType) =
        BusinessPayload.Custom(
            "login_screen_view",
            attributes { put("entry_type", entryType.value) },
        )

    /** 소셜 로그인 버튼 클릭. 성공률의 분모이자 가입 소요 시간의 시작점이다. */
    fun loginAttempt(provider: String) =
        BusinessPayload.Custom(
            "login_attempt",
            attributes { put("provider", provider) },
        )

    /**
     * 토큰 발급 또는 실패.
     *
     * @param errorCode 실패 사유. 성공이면 null 이라 키 자체를 넣지 않는다 — 빈 문자열을 넣으면
     *   집계에서 "빈 값"이라는 가짜 분류가 하나 생긴다.
     */
    fun loginResult(
        provider: String,
        success: Boolean,
        errorCode: String? = null,
        isNewUser: Boolean? = null,
        restored: Boolean? = null,
    ) = BusinessPayload.Custom(
        "login_result",
        attributes {
            put("provider", provider)
            put("success", success)
            errorCode?.let { put("error_code", it) }
            isNewUser?.let { put("is_new_user", it) }
            restored?.let { put("restored", it) }
        },
    )

    /** 온보딩 각 단계 진입. 단계별 이탈 지점 분포를 낸다. */
    fun stepView(step: OnboardingStep) =
        BusinessPayload.Custom(
            "onboarding_step_view",
            attributes {
                put("step", step.value)
                put("step_index", step.index.toLong())
            },
        )

    /**
     * 각 단계 완료.
     *
     * @param skipped 그 단계에서 아무것도 고르지 않고 넘어갔는지. 관심사·사진의 선택률이 여기서 나온다.
     */
    fun stepComplete(
        step: OnboardingStep,
        skipped: Boolean,
    ) = BusinessPayload.Custom(
        "onboarding_step_complete",
        attributes {
            put("step", step.value)
            put("skipped", skipped)
        },
    )

    /** 닉네임 확인 응답. 닉네임 때문에 막히는 사용자를 진단한다. */
    fun nicknameCheck(
        valid: Boolean,
        available: Boolean,
        reason: String? = null,
    ) = BusinessPayload.Custom(
        "nickname_check",
        attributes {
            put("valid", valid)
            put("available", available)
            reason?.let { put("reason", it) }
        },
    )

    /**
     * 가입 성공. **완주율의 분자**다.
     *
     * @param durationMs `login_attempt` 부터의 경과. 가입 소요 시간 중앙값을 낸다.
     */
    fun signupComplete(
        interestCount: Int,
        hasGender: Boolean,
        optionalAgreements: Int,
        durationMs: Long?,
    ) = BusinessPayload.Custom(
        "signup_complete",
        attributes {
            put("interest_count", interestCount.toLong())
            put("has_gender", hasGender)
            put("optional_agreements", optionalAgreements.toLong())
            durationMs?.let { put("duration_ms", it) }
        },
    )

    /** 가입 실패. 특히 `BIRTHDATE_UNDERAGE` 분포를 본다. */
    fun signupFailed(errorCode: String) =
        BusinessPayload.Custom(
            "signup_failed",
            attributes { put("error_code", errorCode) },
        )

    /** 프로필 사진 등록 결과. 사진 등록률을 낸다. */
    fun profileImageUploadResult(
        success: Boolean,
        errorCode: String? = null,
    ) = BusinessPayload.Custom(
        "profile_image_upload_result",
        attributes {
            put("success", success)
            errorCode?.let { put("error_code", it) }
        },
    )

    /** 세션이 끊겨 로그인으로 돌아옴. 단일 활성 기기 정책의 부작용을 모니터링한다. */
    fun sessionExpired(trigger: SessionExpiredTrigger) =
        BusinessPayload.Custom(
            "session_expired",
            attributes { put("trigger", trigger.value) },
        )
}

/** 로그인 화면에 어떻게 왔는지. 첫 설치와 재로그인을 나눠야 완주율 분모가 뒤섞이지 않는다. */
enum class LoginEntryType(
    val value: String,
) {
    FRESH("fresh"),
    RELOGIN("relogin"),
}

/** 세션 종료 사유. 다른 기기 로그인 때문인지, 그냥 만료인지 나눈다. */
enum class SessionExpiredTrigger(
    val value: String,
) {
    OTHER_DEVICE("other_device"),
    EXPIRED("expired"),
}

/**
 * 온보딩 단계. [index] 는 1부터 센다 — 화면의 `n/6` 표기와 같은 기준이라야 집계와 화면이 어긋나지
 * 않는다.
 */
enum class OnboardingStep(
    val value: String,
    val index: Int,
) {
    NICKNAME("nickname", 1),
    INTEREST("interest", 2),
    BIRTH("birth", 3),
    GENDER("gender", 4),
    PHOTO("photo", 5),
    TERMS("terms", 6),
}
