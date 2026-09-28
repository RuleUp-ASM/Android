package com.ruleup.onboarding.domain.logging

import com.ruleup.logging.domain.BizEvent
import com.ruleup.logging.domain.bizAttributes

/** 온보딩 퍼널 이벤트. */
object OnboardingEvents {
    /** 로그인 화면 진입. */
    fun loginScreenView(entryType: LoginEntryType) =
        BizEvent(
            "login_screen_view",
            bizAttributes { put("entry_type", entryType.value) },
        )

    /** 소셜 로그인 버튼 클릭. */
    fun loginAttempt(provider: String) =
        BizEvent(
            "login_attempt",
            bizAttributes { put("provider", provider) },
        )

    /** 토큰 발급 또는 실패. */
    fun loginResult(
        provider: String,
        success: Boolean,
        errorCode: String? = null,
        isNewUser: Boolean? = null,
        restored: Boolean? = null,
    ) = BizEvent(
        "login_result",
        bizAttributes {
            put("provider", provider)
            put("success", success)
            errorCode?.let { put("error_code", it) }
            isNewUser?.let { put("is_new_user", it) }
            restored?.let { put("restored", it) }
        },
    )

    /** 온보딩 각 단계 진입. */
    fun stepView(step: OnboardingStep) =
        BizEvent(
            "onboarding_step_view",
            bizAttributes {
                put("step", step.value)
                put("step_index", step.index.toLong())
            },
        )

    /** 각 단계 완료. */
    fun stepComplete(
        step: OnboardingStep,
        skipped: Boolean,
    ) = BizEvent(
        "onboarding_step_complete",
        bizAttributes {
            put("step", step.value)
            put("skipped", skipped)
        },
    )

    /** 닉네임 확인 응답. */
    fun nicknameCheck(
        valid: Boolean,
        available: Boolean,
        reason: String? = null,
    ) = BizEvent(
        "nickname_check",
        bizAttributes {
            put("valid", valid)
            put("available", available)
            reason?.let { put("reason", it) }
        },
    )

    /** 가입 성공. */
    fun signupComplete(
        interestCount: Int,
        hasGender: Boolean,
        optionalAgreements: Int,
        durationMs: Long?,
    ) = BizEvent(
        "signup_complete",
        bizAttributes {
            put("interest_count", interestCount.toLong())
            put("has_gender", hasGender)
            put("optional_agreements", optionalAgreements.toLong())
            durationMs?.let { put("duration_ms", it) }
        },
    )

    /** 가입 실패. */
    fun signupFailed(errorCode: String) =
        BizEvent(
            "signup_failed",
            bizAttributes { put("error_code", errorCode) },
        )

    /** 프로필 사진 등록 결과. */
    fun profileImageUploadResult(
        success: Boolean,
        errorCode: String? = null,
    ) = BizEvent(
        "profile_image_upload_result",
        bizAttributes {
            put("success", success)
            errorCode?.let { put("error_code", it) }
        },
    )

    /** 세션이 끊겨 로그인으로 돌아옴. */
    fun sessionExpired(trigger: SessionExpiredTrigger) =
        BizEvent(
            "session_expired",
            bizAttributes { put("trigger", trigger.value) },
        )
}

/** 로그인 화면에 어떻게 왔는지. */
enum class LoginEntryType(
    val value: String,
) {
    FRESH("fresh"),
    RELOGIN("relogin"),
}

/** 세션 종료 사유. */
enum class SessionExpiredTrigger(
    val value: String,
) {
    OTHER_DEVICE("other_device"),
    EXPIRED("expired"),
}

/** 온보딩 단계. */
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
